package signaling

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/redis/go-redis/v9"
)

// MatchPayload contains the peer information and polite peer negotiation flag.
type MatchPayload struct {
	PeerID   string `json:"peer_id"`
	IsPolite bool   `json:"is_polite"`
}

// MatchNotification is the message payload sent to users when a match occurs.
type MatchNotification struct {
	Type    string       `json:"type"`
	Payload MatchPayload `json:"payload"`
}

// Bridge coordinates match notifications across local WebSockets and distributed Redis instances.
type Bridge struct {
	hub *Hub
	rdb *redis.Client
}

// NewBridge creates a new Bridge instance.
func NewBridge(hub *Hub, rdb *redis.Client) *Bridge {
	return &Bridge{
		hub: hub,
		rdb: rdb,
	}
}

// NotifyMatch builds and dispatches the match_found event to both userA and userB.
// is_polite is true for userA if userA < userB alphabetically, ensuring deterministic polite/impolite roles.
// If a user is not connected to the local instance, the notification is published via Redis Pub/Sub.
func (b *Bridge) NotifyMatch(ctx context.Context, userA, userB string) error {
	// Determine polite peer deterministically: userA is polite if userA < userB
	isAPolite := userA < userB
	isBPolite := !isAPolite

	msgA, err := json.Marshal(MatchNotification{
		Type: "match_found",
		Payload: MatchPayload{
			PeerID:   userB,
			IsPolite: isAPolite,
		},
	})
	if err != nil {
		return fmt.Errorf("failed to marshal notification for userA: %w", err)
	}

	msgB, err := json.Marshal(MatchNotification{
		Type: "match_found",
		Payload: MatchPayload{
			PeerID:   userA,
			IsPolite: isBPolite,
		},
	})
	if err != nil {
		return fmt.Errorf("failed to marshal notification for userB: %w", err)
	}

	// 1. Deliver to userA (local Hub first, fallback to Redis Pub/Sub)
	if !b.hub.SendTo(userA, msgA) {
		channelA := fmt.Sprintf("match:notify:%s", userA)
		if err := b.rdb.Publish(ctx, channelA, msgA).Err(); err != nil {
			return fmt.Errorf("failed to publish notification for userA to redis: %w", err)
		}
	}

	// 2. Deliver to userB (local Hub first, fallback to Redis Pub/Sub)
	if !b.hub.SendTo(userB, msgB) {
		channelB := fmt.Sprintf("match:notify:%s", userB)
		if err := b.rdb.Publish(ctx, channelB, msgB).Err(); err != nil {
			return fmt.Errorf("failed to publish notification for userB to redis: %w", err)
		}
	}

	return nil
}
