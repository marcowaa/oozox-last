package signaling

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/redis/go-redis/v9"
)

const (
	writeWait                      = 10 * time.Second
	pongWait                       = 60 * time.Second
	pingPeriod                     = (pongWait * 9) / 10
	sendChanBuf                    = 256
	NumShards                      = 64
	UserNodeMappingTTL             = 60 * time.Second
	HeartbeatInterval              = 20 * time.Second
	MaxForwardRetries              = 3
	ActiveNodesSetKey              = "goldchat:active_nodes"
	DefaultNodeInactivityThreshold = 10 * time.Minute
	DefaultMaintenanceInterval     = 5 * time.Minute
	MaintenanceLockKey             = "goldchat:maintenance:stream_prune_lock"
	StreamPrefix                   = "goldchat:signaling:node:"
)

var (
	// Atomic Unregister: Deletes user mapping ONLY IF it still matches this node:connID
	atomicUnregisterLua = redis.NewScript(`
		local current = redis.call("GET", KEYS[1])
		if current == ARGV[1] then
			return redis.call("DEL", KEYS[1])
		else
			return 0
		end
	`)

	// Atomic Heartbeat: Refreshes TTL only if mapping still belongs to this connection
	refreshHeartbeatLua = redis.NewScript(`
		local current = redis.call("GET", KEYS[1])
		if current == ARGV[1] then
			return redis.call("PEXPIRE", KEYS[1], ARGV[2])
		else
			return 0
		end
	`)
)

func randomHex(n int) string {
	b := make([]byte, n)
	_, _ = rand.Read(b)
	return hex.EncodeToString(b)
}

// Client represents an active WebSocket connection belonging to a specific user & optional device.
type Client struct {
	ConnID   string
	UserID   string
	DeviceID string
	Conn     *websocket.Conn
	Send     chan []byte
	Done     chan struct{}
	Once     sync.Once
}

// NewClient initializes a new Client instance.
func NewClient(userID string, conn *websocket.Conn) *Client {
	return NewClientWithDevice(userID, "", conn)
}

// NewClientWithDevice initializes a new Client tagged with a specific DeviceID.
func NewClientWithDevice(userID, deviceID string, conn *websocket.Conn) *Client {
	return &Client{
		ConnID:   randomHex(12),
		UserID:   userID,
		DeviceID: strings.TrimSpace(deviceID),
		Conn:     conn,
		Send:     make(chan []byte, sendChanBuf),
		Done:     make(chan struct{}),
	}
}

// Close safely terminates the connection and channels once.
func (c *Client) Close() {
	c.Once.Do(func() {
		close(c.Done)
		if c.Conn != nil {
			_ = c.Conn.Close()
		}
	})
}

// WritePump pumps messages from the Send channel to the WebSocket connection.
func (c *Client) WritePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.Close()
	}()

	for {
		select {
		case <-c.Done:
			return
		case msg, ok := <-c.Send:
			if !ok {
				if c.Conn != nil {
					_ = c.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				}
				return
			}

			if c.Conn != nil {
				_ = c.Conn.SetWriteDeadline(time.Now().Add(writeWait))
				if err := c.Conn.WriteMessage(websocket.TextMessage, msg); err != nil {
					return
				}
			}
		case <-ticker.C:
			if c.Conn != nil {
				_ = c.Conn.SetWriteDeadline(time.Now().Add(writeWait))
				if err := c.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
					return
				}
			}
		}
	}
}

type Shard struct {
	sync.RWMutex
	clients      map[string]*Client
	multiClients map[string]map[string]*Client
}

// Hub manages active WebSocket clients and handles message routing across instances.
type Hub struct {
	NodeID              string
	StreamName          string
	GroupName           string
	InactivityThreshold time.Duration
	MaintenanceInterval time.Duration
	shards              [NumShards]*Shard
	rdb                 *redis.Client
	ctx                 context.Context
	cancel              context.CancelFunc
	wg                  sync.WaitGroup
}

// NewHub constructs a new Hub instance with 64 RWMutex shards.
func NewHub(rdb *redis.Client) *Hub {
	nodeID := fmt.Sprintf("node-%s", randomHex(4))
	ctx, cancel := context.WithCancel(context.Background())

	inactivityThreshold := DefaultNodeInactivityThreshold
	if envThreshold := os.Getenv("NODE_INACTIVITY_THRESHOLD_SECONDS"); envThreshold != "" {
		if v, err := strconv.Atoi(envThreshold); err == nil && v > 0 {
			inactivityThreshold = time.Duration(v) * time.Second
		}
	}

	maintenanceInterval := DefaultMaintenanceInterval
	if envInterval := os.Getenv("MAINTENANCE_INTERVAL_SECONDS"); envInterval != "" {
		if v, err := strconv.Atoi(envInterval); err == nil && v > 0 {
			maintenanceInterval = time.Duration(v) * time.Second
		}
	}

	h := &Hub{
		NodeID:              nodeID,
		StreamName:          fmt.Sprintf("goldchat:signaling:node:%s", nodeID),
		GroupName:           "group_" + nodeID,
		InactivityThreshold: inactivityThreshold,
		MaintenanceInterval: maintenanceInterval,
		rdb:                 rdb,
		ctx:                 ctx,
		cancel:              cancel,
	}

	for i := 0; i < NumShards; i++ {
		h.shards[i] = &Shard{
			clients:      make(map[string]*Client),
			multiClients: make(map[string]map[string]*Client),
		}
	}

	return h
}

func (h *Hub) getShard(userID string) *Shard {
	hash := uint32(2166136261)
	for i := 0; i < len(userID); i++ {
		hash ^= uint32(userID[i])
		hash *= 16777619
	}
	return h.shards[hash%NumShards]
}

func (h *Hub) getMappingValue(connID string) string {
	return fmt.Sprintf("%s:%s", h.NodeID, connID)
}

// Register adds a client connection to the hub shard, preserving concurrent distinct devices per user.
func (h *Hub) Register(client *Client) {
	if client == nil || strings.TrimSpace(client.UserID) == "" {
		return
	}
	shard := h.getShard(client.UserID)
	shard.Lock()
	if shard.multiClients[client.UserID] == nil {
		shard.multiClients[client.UserID] = make(map[string]*Client)
	}
	// Replace only an existing connection on the same deviceId (or untagged legacy connection)
	for existingConnID, existing := range shard.multiClients[client.UserID] {
		if (client.DeviceID == "" && existing.DeviceID == "") ||
			(client.DeviceID != "" && existing.DeviceID == client.DeviceID) {
			delete(shard.multiClients[client.UserID], existingConnID)
			existing.Close()
		}
	}
	shard.multiClients[client.UserID][client.ConnID] = client
	shard.clients[client.UserID] = client
	shard.Unlock()
}

// SetUserNodeMappingWithRetry writes the mapping with retry logic to avoid zombie connections
func (h *Hub) SetUserNodeMappingWithRetry(ctx context.Context, client *Client, maxAttempts int) error {
	if h.rdb == nil {
		return nil
	}
	key := fmt.Sprintf("user:node:%s", client.UserID)
	val := h.getMappingValue(client.ConnID)

	var lastErr error
	backoff := 50 * time.Millisecond

	for attempt := 1; attempt <= maxAttempts; attempt++ {
		err := h.rdb.Set(ctx, key, val, UserNodeMappingTTL).Err()
		if err == nil {
			return nil
		}
		lastErr = err
		log.Printf("[Signaling] Warning: Attempt %d/%d to register user:node mapping for %s failed: %v",
			attempt, maxAttempts, client.UserID, err)

		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(backoff):
			backoff *= 2
		}
	}
	return fmt.Errorf("exhausted %d attempts: %w", maxAttempts, lastErr)
}

// RefreshUserHeartbeat refreshes TTL atomically only if mapping belongs to this connection
func (h *Hub) RefreshUserHeartbeat(ctx context.Context, client *Client) {
	if h.rdb == nil {
		return
	}
	key := fmt.Sprintf("user:node:%s", client.UserID)
	val := h.getMappingValue(client.ConnID)
	_ = refreshHeartbeatLua.Run(ctx, h.rdb, []string{key}, val, UserNodeMappingTTL.Milliseconds()).Err()
}

// Unregister safely removes only the specific client instance from the hub and atomic Lua unregisters mapping.
func (h *Hub) Unregister(client *Client) {
	if client == nil {
		return
	}
	shard := h.getShard(client.UserID)
	shard.Lock()
	if conns, ok := shard.multiClients[client.UserID]; ok {
		if _, found := conns[client.ConnID]; found {
			delete(conns, client.ConnID)
			client.Close()
		}
		if len(conns) == 0 {
			delete(shard.multiClients, client.UserID)
			delete(shard.clients, client.UserID)
		} else if cur, exists := shard.clients[client.UserID]; exists && cur == client {
			for _, replacement := range conns {
				shard.clients[client.UserID] = replacement
				break
			}
		}
	} else if cur, ok := shard.clients[client.UserID]; ok && cur == client {
		delete(shard.clients, client.UserID)
		client.Close()
	}
	shard.Unlock()

	if h.rdb != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
		defer cancel()

		key := fmt.Sprintf("user:node:%s", client.UserID)
		expectedVal := h.getMappingValue(client.ConnID)
		_ = atomicUnregisterLua.Run(ctx, h.rdb, []string{key}, expectedVal).Err()
	}
}

// GetClient retrieves an active local client by UserID.
func (h *Hub) GetClient(userID string) (*Client, bool) {
	shard := h.getShard(userID)
	shard.RLock()
	client, ok := shard.clients[userID]
	shard.RUnlock()
	return client, ok
}

// SendTo delivers a message directly to all active local device connections for userID.
func (h *Hub) SendTo(userID string, msg []byte) bool {
	return h.SendToDevice(userID, "", msg)
}

// SendToDevice delivers a message to a specific deviceID for userID (or all devices if targetDeviceID is empty).
func (h *Hub) SendToDevice(userID, targetDeviceID string, msg []byte) bool {
	shard := h.getShard(userID)
	shard.RLock()
	connsMap := shard.multiClients[userID]
	var targets []*Client
	if len(connsMap) > 0 {
		for _, c := range connsMap {
			if targetDeviceID == "" || c.DeviceID == targetDeviceID {
				targets = append(targets, c)
			}
		}
	} else if c, ok := shard.clients[userID]; ok {
		if targetDeviceID == "" || c.DeviceID == targetDeviceID {
			targets = append(targets, c)
		}
	}
	shard.RUnlock()

	delivered := false
	for _, client := range targets {
		select {
		case <-client.Done:
			continue
		case client.Send <- msg:
			delivered = true
		default:
			go h.Unregister(client)
		}
	}
	return delivered
}

// RouteSignal routes a message to a target user, either locally or through Redis Streams.
func (h *Hub) RouteSignal(ctx context.Context, targetUserID string, data []byte) error {
	if h.SendTo(targetUserID, data) {
		return nil
	}

	if h.rdb == nil {
		return fmt.Errorf("client %s offline and redis client not configured", targetUserID)
	}

	key := fmt.Sprintf("user:node:%s", targetUserID)
	val, err := h.rdb.Get(ctx, key).Result()
	if err == redis.Nil {
		return fmt.Errorf("user %s is offline", targetUserID)
	} else if err != nil {
		return err
	}

	parts := strings.Split(val, ":")
	targetNodeID := parts[0]

	targetStream := fmt.Sprintf("goldchat:signaling:node:%s", targetNodeID)
	return h.rdb.XAdd(ctx, &redis.XAddArgs{
		Stream: targetStream,
		MaxLen: 5000,
		Approx: true,
		Values: map[string]interface{}{
			"to":      targetUserID,
			"payload": string(data),
		},
	}).Err()
}

// StartStreamConsumer runs the per-node Redis Stream consumer.
func (h *Hub) StartStreamConsumer() {
	if h.rdb == nil {
		return
	}
	h.wg.Add(1)
	defer h.wg.Done()
	_ = h.rdb.XGroupCreateMkStream(h.ctx, h.StreamName, h.GroupName, "$").Err()

	for {
		select {
		case <-h.ctx.Done():
			return
		default:
			streams, err := h.rdb.XReadGroup(h.ctx, &redis.XReadGroupArgs{
				Group:    h.GroupName,
				Consumer: h.NodeID,
				Streams:  []string{h.StreamName, ">"},
				Count:    50,
				Block:    2 * time.Second,
			}).Result()

			if err != nil {
				if err != redis.Nil && !strings.Contains(err.Error(), "context canceled") {
					time.Sleep(100 * time.Millisecond)
				}
				continue
			}

			for _, stream := range streams {
				for _, msg := range stream.Messages {
					targetID, _ := msg.Values["to"].(string)
					rawPayload, _ := msg.Values["payload"].(string)

					if h.SendTo(targetID, []byte(rawPayload)) {
						_ = h.rdb.XAck(h.ctx, h.StreamName, h.GroupName, msg.ID)
					} else {
						// Stale delivery migration fallback: check if user migrated to another node
						h.handleStaleDelivery(h.ctx, targetID, rawPayload, msg.ID)
					}
				}
			}
		}
	}
}

func (h *Hub) handleStaleDelivery(ctx context.Context, targetID, rawPayload, msgID string) {
	val, err := h.rdb.Get(ctx, fmt.Sprintf("user:node:%s", targetID)).Result()
	if err == nil {
		parts := strings.Split(val, ":")
		newTargetNode := parts[0]

		if newTargetNode != h.NodeID {
			newStream := fmt.Sprintf("goldchat:signaling:node:%s", newTargetNode)
			err = h.rdb.XAdd(ctx, &redis.XAddArgs{
				Stream: newStream,
				MaxLen: 5000,
				Approx: true,
				Values: map[string]interface{}{
					"to":      targetID,
					"payload": rawPayload,
				},
			}).Err()

			if err == nil {
				_ = h.rdb.XAck(ctx, h.StreamName, h.GroupName, msgID)
				return
			}
		}
	}
	_ = h.rdb.XAck(ctx, h.StreamName, h.GroupName, msgID)
}

// StartNodeLifecycle maintains node heartbeat in ActiveNodesSetKey and periodically runs stream maintenance.
func (h *Hub) StartNodeLifecycle() {
	if h.rdb == nil {
		return
	}
	h.wg.Add(1)
	defer h.wg.Done()

	heartbeatTicker := time.NewTicker(HeartbeatInterval)
	defer heartbeatTicker.Stop()

	// Initial node registration
	_ = h.rdb.HSet(h.ctx, ActiveNodesSetKey, h.NodeID, time.Now().Unix()).Err()

	mInterval := h.MaintenanceInterval
	if mInterval <= 0 {
		mInterval = DefaultMaintenanceInterval
	}
	cleanerTicker := time.NewTicker(mInterval)
	defer cleanerTicker.Stop()

	// Run initial pruning pass in background after a short delay
	go func() {
		time.Sleep(5 * time.Second)
		initCtx, cancel := context.WithTimeout(h.ctx, 30*time.Second)
		defer cancel()
		_, _ = h.PruneOrphanedStreams(initCtx)
	}()

	for {
		select {
		case <-h.ctx.Done():
			return
		case <-heartbeatTicker.C:
			_ = h.rdb.HSet(h.ctx, ActiveNodesSetKey, h.NodeID, time.Now().Unix()).Err()
		case <-cleanerTicker.C:
			mCtx, cancel := context.WithTimeout(h.ctx, 45*time.Second)
			_, _ = h.PruneOrphanedStreams(mCtx)
			cancel()
		}
	}
}

// PruneOrphanedStreams scans and removes orphaned per-node Redis streams and associated
// consumer groups for nodes that have been inactive for more than the defined threshold.
// This guarantees Redis memory usage remains stable over time.
func (h *Hub) PruneOrphanedStreams(ctx context.Context) (int, error) {
	if h.rdb == nil {
		return 0, nil
	}

	// 1. Acquire distributed maintenance lock so only one cluster instance runs pruning per tick
	locked, err := h.rdb.SetNX(ctx, MaintenanceLockKey, h.NodeID, 45*time.Second).Result()
	if err != nil {
		return 0, fmt.Errorf("maintenance lock error: %w", err)
	}
	if !locked {
		// Another cluster node is actively performing maintenance
		return 0, nil
	}
	defer func() {
		_ = h.rdb.Eval(ctx, `
			if redis.call("GET", KEYS[1]) == ARGV[1] then
				return redis.call("DEL", KEYS[1])
			else
				return 0
			end
		`, []string{MaintenanceLockKey}, h.NodeID).Err()
	}()

	now := time.Now().Unix()
	thresholdSeconds := int64(h.InactivityThreshold.Seconds())
	if thresholdSeconds <= 0 {
		thresholdSeconds = int64(DefaultNodeInactivityThreshold.Seconds())
	}

	// 2. Fetch known registered active nodes and their last heartbeat timestamp
	nodeMap, err := h.rdb.HGetAll(ctx, ActiveNodesSetKey).Result()
	if err != nil && err != redis.Nil {
		log.Printf("[Signaling Maintenance] Warning: failed to read %s: %v", ActiveNodesSetKey, err)
		nodeMap = make(map[string]string)
	}
	if nodeMap == nil {
		nodeMap = make(map[string]string)
	}

	// 3. Discover candidates: active nodes hash AND scan Redis keys matching StreamPrefix*
	candidates := make(map[string]string) // streamKey -> nodeID

	for nID := range nodeMap {
		candidates[StreamPrefix+nID] = nID
	}

	var cursor uint64
	for {
		keys, nextCursor, scanErr := h.rdb.Scan(ctx, cursor, StreamPrefix+"*", 100).Result()
		if scanErr != nil {
			log.Printf("[Signaling Maintenance] SCAN error during stream discovery: %v", scanErr)
			break
		}
		for _, k := range keys {
			nID := strings.TrimPrefix(k, StreamPrefix)
			if nID != "" {
				candidates[k] = nID
			}
		}
		cursor = nextCursor
		if cursor == 0 {
			break
		}
	}

	prunedCount := 0

	// 4. Evaluate each candidate stream
	for streamKey, nodeID := range candidates {
		func() {
			defer func() {
				if r := recover(); r != nil {
					log.Printf("[Signaling Maintenance] Recovered from panic while pruning stream '%s' (node '%s'): %v", streamKey, nodeID, r)
				}
			}()

			// Never prune our own currently running node
			if nodeID == h.NodeID {
				return
			}

			isOrphaned := false
			lastSeenStr, hasHeartbeat := nodeMap[nodeID]

			if hasHeartbeat {
				lastSeen, parseErr := strconv.ParseInt(lastSeenStr, 10, 64)
				if parseErr != nil || (now-lastSeen) > thresholdSeconds {
					isOrphaned = true
				}
			} else {
				// Stream exists without an active node registration -> orphaned
				isOrphaned = true
			}

			if !isOrphaned {
				return
			}

			// Requirement 5: Re-check the node's activity/state right before deletion
			// to minimize race conditions with a node that has just become active.
			freshSeenStr, freshErr := h.rdb.HGet(ctx, ActiveNodesSetKey, nodeID).Result()
			if freshErr == nil && freshSeenStr != "" {
				if freshLastSeen, parseErr := strconv.ParseInt(freshSeenStr, 10, 64); parseErr == nil {
					if (time.Now().Unix() - freshLastSeen) <= thresholdSeconds {
						// Node recently updated its heartbeat or became active around the cleanup boundary.
						// Abort deletion to protect active resources.
						log.Printf("[Signaling Maintenance] Node '%s' recently active (lastSeen: %d). Skipping pruning.", nodeID, freshLastSeen)
						return
					}
				}
			}

			// 5. Destroy ALL consumer groups associated with this orphaned stream to reclaim PEL memory
			destroyedGroups := 0
			groups, grpErr := h.rdb.XInfoGroups(ctx, streamKey).Result()
			if grpErr == nil {
				for _, grp := range groups {
					if destroyErr := h.rdb.XGroupDestroy(ctx, streamKey, grp.Name).Err(); destroyErr != nil {
						if !isNotFoundError(destroyErr) {
							log.Printf("[Signaling Maintenance] Warning: error destroying consumer group '%s' on stream '%s' (node '%s'): %v",
								grp.Name, streamKey, nodeID, destroyErr)
						}
					} else {
						destroyedGroups++
					}
				}
			} else if !isNotFoundError(grpErr) {
				// Fallback: attempt to destroy convention group if stream groups couldn't be enumerated
				defaultGroup := "group_" + nodeID
				if err := h.rdb.XGroupDestroy(ctx, streamKey, defaultGroup).Err(); err == nil {
					destroyedGroups++
				} else if !isNotFoundError(err) {
					log.Printf("[Signaling Maintenance] Warning: fallback XGroupDestroy on '%s': %v", streamKey, err)
				}
			}

			// 6. Delete stream asynchronously (UNLINK) to avoid blocking Redis memory management
			if unlinkErr := h.rdb.Unlink(ctx, streamKey).Err(); unlinkErr != nil {
				if !isNotFoundError(unlinkErr) {
					log.Printf("[Signaling Maintenance] Warning: UNLINK failed on stream '%s' (node '%s'): %v. Falling back to DEL", streamKey, nodeID, unlinkErr)
					if delErr := h.rdb.Del(ctx, streamKey).Err(); delErr != nil && !isNotFoundError(delErr) {
						log.Printf("[Signaling Maintenance] Error: failed to delete stream '%s' (node '%s'): %v", streamKey, nodeID, delErr)
					}
				}
			}

			// 7. Remove node registration from tracking hash
			if hdelErr := h.rdb.HDel(ctx, ActiveNodesSetKey, nodeID).Err(); hdelErr != nil && !isNotFoundError(hdelErr) {
				log.Printf("[Signaling Maintenance] Warning: failed to remove node '%s' from %s: %v", nodeID, ActiveNodesSetKey, hdelErr)
			}

			prunedCount++
			log.Printf("[Signaling Maintenance] Successfully pruned orphaned stream '%s' (%d consumer groups destroyed) for inactive node '%s'",
				streamKey, destroyedGroups, nodeID)
		}()
	}

	if prunedCount > 0 {
		log.Printf("[Signaling Maintenance] Pruning cycle complete: cleaned %d orphaned stream(s), reclaimed Redis memory", prunedCount)
	}

	return prunedCount, nil
}

// isNotFoundError returns true if the error indicates the Redis key or consumer group does not exist.
func isNotFoundError(err error) bool {
	if err == nil || errors.Is(err, redis.Nil) {
		return true
	}
	s := strings.ToLower(err.Error())
	return strings.Contains(s, "no such key") || strings.Contains(s, "nogroup") || strings.Contains(s, "not found")
}

func (h *Hub) cleanOrphanedStreams(ctx context.Context) {
	_, _ = h.PruneOrphanedStreams(ctx)
}

// Shutdown cleans up local resources and deregisters from Redis cluster.
func (h *Hub) Shutdown(ctx context.Context) {
	h.cancel()
	h.wg.Wait()

	if h.rdb != nil {
		_ = h.rdb.XGroupDestroy(ctx, h.StreamName, h.GroupName).Err()
		if unlinkErr := h.rdb.Unlink(ctx, h.StreamName).Err(); unlinkErr != nil {
			_ = h.rdb.Del(ctx, h.StreamName).Err()
		}
		_ = h.rdb.HDel(ctx, ActiveNodesSetKey, h.NodeID).Err()
		_ = h.rdb.Eval(ctx, `
			if redis.call("GET", KEYS[1]) == ARGV[1] then
				return redis.call("DEL", KEYS[1])
			else
				return 0
			end
		`, []string{MaintenanceLockKey}, h.NodeID).Err()
	}
}

// RealtimeEventEnvelope matches messages published on "oozox:realtime:events" by Node Backend.
type RealtimeEventEnvelope struct {
	Type           string          `json:"type"`
	TargetUserIDs  []string        `json:"targetUserIds"`
	TargetDeviceID string          `json:"targetDeviceId,omitempty"`
	Payload        json.RawMessage `json:"payload"`
	EmittedAt      string          `json:"emittedAt,omitempty"`
}

// Run listens to both "match:notify:*" and "oozox:realtime:events" Redis Pub/Sub channels.
func (h *Hub) Run(ctx context.Context) {
	if h.rdb == nil {
		return
	}

	pubsub := h.rdb.PSubscribe(ctx, "match:notify:*", "oozox:realtime:events")
	defer pubsub.Close()

	ch := pubsub.Channel()
	for {
		select {
		case <-ctx.Done():
			return
		case msg, ok := <-ch:
			if !ok {
				return
			}
			if msg.Channel == "oozox:realtime:events" {
				var ev RealtimeEventEnvelope
				if err := json.Unmarshal([]byte(msg.Payload), &ev); err != nil || ev.Type == "" {
					continue
				}
				outbound, err := json.Marshal(map[string]interface{}{
					"type":      ev.Type,
					"payload":   ev.Payload,
					"emittedAt": ev.EmittedAt,
				})
				if err != nil {
					continue
				}
				for _, uid := range ev.TargetUserIDs {
					cleanUID := strings.TrimSpace(uid)
					if cleanUID == "" {
						continue
					}
					if ev.TargetDeviceID != "" {
						h.SendToDevice(cleanUID, ev.TargetDeviceID, outbound)
					} else {
						_ = h.RouteSignal(ctx, cleanUID, outbound)
					}
				}
				continue
			}

			targetUserID := strings.TrimPrefix(msg.Channel, "match:notify:")
			if targetUserID != "" {
				h.SendTo(targetUserID, []byte(msg.Payload))
			}
		}
	}
}

// SubscribeToUser is a direct channel subscription helper for testing or dedicated listeners.
func (h *Hub) SubscribeToUser(ctx context.Context, userID string) <-chan []byte {
	out := make(chan []byte, sendChanBuf)
	if h.rdb == nil {
		close(out)
		return out
	}

	channelName := fmt.Sprintf("match:notify:%s", userID)
	pubsub := h.rdb.Subscribe(ctx, channelName)

	go func() {
		defer close(out)
		defer pubsub.Close()

		ch := pubsub.Channel()
		for {
			select {
			case <-ctx.Done():
				return
			case msg, ok := <-ch:
				if !ok {
					return
				}
				select {
				case out <- []byte(msg.Payload):
				case <-ctx.Done():
					return
				}
			}
		}
	}()

	return out
}

