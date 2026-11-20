package signaling

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	"github.com/redis/go-redis/v9"
)

func setupTestHubWithRedis(t *testing.T) (*Hub, *redis.Client, *miniredis.Miniredis) {
	mr := miniredis.RunT(t)
	rdb := redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})
	hub := NewHub(rdb)
	return hub, rdb, mr
}

func TestPruneOrphanedStreams(t *testing.T) {
	hub, rdb, _ := setupTestHubWithRedis(t)
	ctx := context.Background()

	hub.InactivityThreshold = 10 * time.Second

	now := time.Now().Unix()

	// 1. Setup an active node (heartbeat 2s ago)
	activeNodeID := "node-active-123"
	activeStream := fmt.Sprintf("%s%s", StreamPrefix, activeNodeID)
	_ = rdb.HSet(ctx, ActiveNodesSetKey, activeNodeID, now-2).Err()
	_ = rdb.XAdd(ctx, &redis.XAddArgs{
		Stream: activeStream,
		Values: map[string]interface{}{"msg": "hello active"},
	}).Err()

	// 2. Setup an inactive dead node (heartbeat 60s ago > 10s threshold)
	deadNodeID := "node-dead-999"
	deadStream := fmt.Sprintf("%s%s", StreamPrefix, deadNodeID)
	_ = rdb.HSet(ctx, ActiveNodesSetKey, deadNodeID, now-60).Err()
	_ = rdb.XAdd(ctx, &redis.XAddArgs{
		Stream: deadStream,
		Values: map[string]interface{}{"msg": "hello dead"},
	}).Err()

	// 3. Setup an orphaned unindexed stream (exists in Redis, but not in ActiveNodesSetKey)
	orphanedNodeID := "node-orphan-777"
	orphanedStream := fmt.Sprintf("%s%s", StreamPrefix, orphanedNodeID)
	_ = rdb.XAdd(ctx, &redis.XAddArgs{
		Stream: orphanedStream,
		Values: map[string]interface{}{"msg": "hello orphan"},
	}).Err()

	// 4. Setup our own node's stream
	_ = rdb.HSet(ctx, ActiveNodesSetKey, hub.NodeID, now).Err()
	_ = rdb.XAdd(ctx, &redis.XAddArgs{
		Stream: hub.StreamName,
		Values: map[string]interface{}{"msg": "self"},
	}).Err()

	// Run maintenance pruning
	pruned, err := hub.PruneOrphanedStreams(ctx)
	if err != nil {
		t.Fatalf("unexpected error during PruneOrphanedStreams: %v", err)
	}

	if pruned != 2 {
		t.Fatalf("expected 2 pruned streams (dead and orphan), got %d", pruned)
	}

	// Verify active node stream STILL EXISTS
	existsActive, err := rdb.Exists(ctx, activeStream).Result()
	if err != nil || existsActive != 1 {
		t.Errorf("active node stream should still exist, got %d (err: %v)", existsActive, err)
	}

	// Verify our own node stream STILL EXISTS
	existsSelf, err := rdb.Exists(ctx, hub.StreamName).Result()
	if err != nil || existsSelf != 1 {
		t.Errorf("self node stream should still exist, got %d (err: %v)", existsSelf, err)
	}

	// Verify dead node stream WAS DELETED
	existsDead, err := rdb.Exists(ctx, deadStream).Result()
	if err != nil || existsDead != 0 {
		t.Errorf("dead node stream should have been deleted, got %d", existsDead)
	}

	// Verify orphaned stream WAS DELETED
	existsOrphan, err := rdb.Exists(ctx, orphanedStream).Result()
	if err != nil || existsOrphan != 0 {
		t.Errorf("orphaned stream should have been deleted, got %d", existsOrphan)
	}

	// Verify dead node was removed from ActiveNodesSetKey
	deadInHash, err := rdb.HExists(ctx, ActiveNodesSetKey, deadNodeID).Result()
	if err != nil || deadInHash {
		t.Errorf("dead node should have been removed from ActiveNodesSetKey, got %v", deadInHash)
	}
}

// TestMaintenance_InactivityThresholds verifies that nodes inactive for < 10m are preserved
// and nodes inactive for > 10m have their streams and consumer groups pruned.
func TestMaintenance_InactivityThresholds(t *testing.T) {
	hub, rdb, _ := setupTestHubWithRedis(t)
	ctx := context.Background()

	// Default threshold is 10 minutes (600 seconds)
	hub.InactivityThreshold = 10 * time.Minute
	now := time.Now().Unix()

	// Case A: Active node (heartbeat 10s ago) with an empty stream -> Must be PRESERVED
	nodeActive := "node-active-test"
	streamActive := StreamPrefix + nodeActive
	_ = rdb.HSet(ctx, ActiveNodesSetKey, nodeActive, now-10).Err()
	_ = rdb.XAdd(ctx, &redis.XAddArgs{Stream: streamActive, Values: map[string]interface{}{"init": "1"}}).Err()
	_ = rdb.XGroupCreateMkStream(ctx, streamActive, "group_"+nodeActive, "$").Err()

	// Case B: Inactive for 5 minutes (< 10 minutes threshold) -> Must be PRESERVED
	node5Min := "node-5min-inactive"
	stream5Min := StreamPrefix + node5Min
	_ = rdb.HSet(ctx, ActiveNodesSetKey, node5Min, now-300).Err()
	_ = rdb.XAdd(ctx, &redis.XAddArgs{Stream: stream5Min, Values: map[string]interface{}{"init": "1"}}).Err()
	_ = rdb.XGroupCreateMkStream(ctx, stream5Min, "group_"+node5Min, "$").Err()

	// Case C: Inactive for 11 minutes (> 10 minutes threshold) -> Must be PRUNED
	node11Min := "node-11min-inactive"
	stream11Min := StreamPrefix + node11Min
	_ = rdb.HSet(ctx, ActiveNodesSetKey, node11Min, now-660).Err()
	_ = rdb.XAdd(ctx, &redis.XAddArgs{Stream: stream11Min, Values: map[string]interface{}{"init": "1"}}).Err()
	_ = rdb.XGroupCreateMkStream(ctx, stream11Min, "group_"+node11Min, "$").Err()

	pruned, err := hub.PruneOrphanedStreams(ctx)
	if err != nil {
		t.Fatalf("PruneOrphanedStreams returned error: %v", err)
	}
	if pruned != 1 {
		t.Fatalf("expected exactly 1 pruned stream, got %d", pruned)
	}

	// Active node stream and group must still exist
	if exists, _ := rdb.Exists(ctx, streamActive).Result(); exists != 1 {
		t.Errorf("active node stream was unexpectedly deleted")
	}

	// 5min inactive node stream and group must still exist
	if exists, _ := rdb.Exists(ctx, stream5Min).Result(); exists != 1 {
		t.Errorf("5-min inactive stream was unexpectedly deleted")
	}

	// 11min inactive node stream and group must be gone
	if exists, _ := rdb.Exists(ctx, stream11Min).Result(); exists != 0 {
		t.Errorf("11-min inactive stream should have been deleted")
	}
}

// TestMaintenance_RaceConditionBoundary tests when a node updates its heartbeat around the cleanup boundary.
func TestMaintenance_RaceConditionBoundary(t *testing.T) {
	hub, rdb, _ := setupTestHubWithRedis(t)
	ctx := context.Background()

	hub.InactivityThreshold = 10 * time.Minute
	now := time.Now().Unix()

	nodeRacing := "node-racing-boundary"
	streamRacing := StreamPrefix + nodeRacing

	// Initially expired in stale snapshot
	_ = rdb.HSet(ctx, ActiveNodesSetKey, nodeRacing, now-700).Err()
	_ = rdb.XAdd(ctx, &redis.XAddArgs{Stream: streamRacing, Values: map[string]interface{}{"init": "1"}}).Err()

	// Simulate node sending a fresh heartbeat right before deletion check
	_ = rdb.HSet(ctx, ActiveNodesSetKey, nodeRacing, time.Now().Unix()).Err()

	pruned, err := hub.PruneOrphanedStreams(ctx)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if pruned != 0 {
		t.Fatalf("expected 0 pruned due to fresh heartbeat boundary check, got %d", pruned)
	}

	if exists, _ := rdb.Exists(ctx, streamRacing).Result(); exists != 1 {
		t.Errorf("racing stream should have been preserved after fresh heartbeat check")
	}
}

// TestMaintenance_IdempotentMissingResources verifies that cleaning resources that are already
// missing or partially deleted does not fail or crash.
func TestMaintenance_IdempotentMissingResources(t *testing.T) {
	hub, rdb, _ := setupTestHubWithRedis(t)
	ctx := context.Background()

	hub.InactivityThreshold = 10 * time.Minute
	now := time.Now().Unix()

	// Register a dead node whose stream does NOT exist in Redis
	deadGhost := "node-ghost-dead"
	_ = rdb.HSet(ctx, ActiveNodesSetKey, deadGhost, now-800).Err()

	// Pruning should safely clean the hash entry without error
	pruned, err := hub.PruneOrphanedStreams(ctx)
	if err != nil {
		t.Fatalf("expected idempotent success when stream doesn't exist, got: %v", err)
	}
	if pruned != 1 {
		t.Fatalf("expected 1 pruned ghost entry, got %d", pruned)
	}

	inHash, _ := rdb.HExists(ctx, ActiveNodesSetKey, deadGhost).Result()
	if inHash {
		t.Errorf("ghost node should be removed from active nodes hash")
	}
}

// TestMaintenance_Lifecycle verifies that StartNodeLifecycle starts background maintenance
// and stops cleanly when Shutdown is triggered.
func TestMaintenance_Lifecycle(t *testing.T) {
	hub, rdb, _ := setupTestHubWithRedis(t)
	hub.MaintenanceInterval = 100 * time.Millisecond

	// Start background lifecycle
	go hub.StartNodeLifecycle()

	// Wait for initial registration
	time.Sleep(50 * time.Millisecond)

	exists, err := rdb.HExists(context.Background(), ActiveNodesSetKey, hub.NodeID).Result()
	if err != nil || !exists {
		t.Errorf("expected node to register in %s", ActiveNodesSetKey)
	}

	// Trigger clean shutdown
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancel()
	hub.Shutdown(shutdownCtx)

	// After shutdown, node should be deregistered
	stillInHash, _ := rdb.HExists(context.Background(), ActiveNodesSetKey, hub.NodeID).Result()
	if stillInHash {
		t.Errorf("node should be removed from active nodes after shutdown")
	}
}

func TestMaintenanceDistributedLock(t *testing.T) {
	hub, rdb, _ := setupTestHubWithRedis(t)
	ctx := context.Background()

	// Pre-acquire lock by another node
	_ = rdb.Set(ctx, MaintenanceLockKey, "another-node-456", 30*time.Second).Err()

	// PruneOrphanedStreams should gracefully skip when lock is held
	pruned, err := hub.PruneOrphanedStreams(ctx)
	if err != nil {
		t.Fatalf("expected no error when lock is held by another node, got %v", err)
	}
	if pruned != 0 {
		t.Fatalf("expected 0 pruned when lock held, got %d", pruned)
	}
}
