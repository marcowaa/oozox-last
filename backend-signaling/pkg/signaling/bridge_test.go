package signaling

import (
	"context"
	"encoding/json"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	"github.com/redis/go-redis/v9"
)

func setupTestBridge(t *testing.T) (*Bridge, *Hub, *miniredis.Miniredis) {
	mr := miniredis.RunT(t)
	rdb := redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})
	hub := NewHub(rdb)
	bridge := NewBridge(hub, rdb)
	return bridge, hub, mr
}

func TestBridgeNotifyMatchLocalAndRemote(t *testing.T) {
	bridge, hub, _ := setupTestBridge(t)
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	userA := "alice"
	userB := "bob"

	// userA is connected locally to the Hub
	clientA := NewClient(userA, nil)
	hub.Register(clientA)

	// userB is on another instance, so we listen to userB's Redis channel
	subChanB := hub.SubscribeToUser(ctx, userB)

	// Ensure pubsub subscription is established
	time.Sleep(50 * time.Millisecond)

	// Trigger NotifyMatch
	if err := bridge.NotifyMatch(ctx, userA, userB); err != nil {
		t.Fatalf("NotifyMatch failed: %v", err)
	}

	// 1. Verify userA receives message locally via Hub.Send
	select {
	case msgA := <-clientA.Send:
		var notifA MatchNotification
		if err := json.Unmarshal(msgA, &notifA); err != nil {
			t.Fatalf("failed to unmarshal notification for userA: %v", err)
		}
		if notifA.Type != "match_found" {
			t.Errorf("expected type match_found, got %s", notifA.Type)
		}
		if notifA.Payload.PeerID != userB {
			t.Errorf("expected peer_id %s, got %s", userB, notifA.Payload.PeerID)
		}
		// alice < bob -> is_polite must be true
		if !notifA.Payload.IsPolite {
			t.Errorf("expected userA (alice) to be polite (alice < bob)")
		}
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for local delivery to userA")
	}

	// 2. Verify userB receives message via Redis Pub/Sub fallback
	select {
	case msgB := <-subChanB:
		var notifB MatchNotification
		if err := json.Unmarshal(msgB, &notifB); err != nil {
			t.Fatalf("failed to unmarshal notification for userB: %v", err)
		}
		if notifB.Type != "match_found" {
			t.Errorf("expected type match_found, got %s", notifB.Type)
		}
		if notifB.Payload.PeerID != userA {
			t.Errorf("expected peer_id %s, got %s", userA, notifB.Payload.PeerID)
		}
		// bob > alice -> is_polite must be false
		if notifB.Payload.IsPolite {
			t.Errorf("expected userB (bob) to be impolite (not polite)")
		}
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for pubsub delivery to userB")
	}
}
