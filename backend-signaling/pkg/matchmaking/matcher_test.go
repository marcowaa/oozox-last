package matchmaking

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	"github.com/redis/go-redis/v9"
)

func setupTestMatcher(t *testing.T) (*Matcher, *miniredis.Miniredis) {
	mr := miniredis.RunT(t)
	rdb := redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})
	return New(rdb), mr
}

// 1. مستخدم واحد -> waiting ("") والتحقق من TTL = 5 دقائق
func TestSingleUserWaiting(t *testing.T) {
	m, mr := setupTestMatcher(t)
	ctx := context.Background()

	peer, err := m.RequestMatch(ctx, "user1", "EG")
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if peer != "" {
		t.Fatalf("expected empty peer (waiting), got: %s", peer)
	}

	// التحقق من أن TTL لا يقل عن 4 دقائق و 50 ثانية (UserQueueTTL = 5 minutes)
	ttl := mr.TTL("match:user:user1")
	if ttl <= 4*time.Minute {
		t.Fatalf("expected TTL to be around 5 minutes, got: %v", ttl)
	}
}

// 2. مستخدمان -> matched
func TestTwoUsersMatched(t *testing.T) {
	m, _ := setupTestMatcher(t)
	ctx := context.Background()

	// user1 enters queue and waits
	peer1, err := m.RequestMatch(ctx, "user1", "EG")
	if err != nil {
		t.Fatalf("expected user1 to join queue without error, got: %v", err)
	}
	if peer1 != "" {
		t.Fatalf("expected user1 to be waiting, got peer: %s", peer1)
	}

	// user2 enters queue and matches with user1
	peer2, err := m.RequestMatch(ctx, "user2", "EG")
	if err != nil {
		t.Fatalf("expected user2 to match, got error: %v", err)
	}
	if peer2 != "user1" {
		t.Fatalf("expected peer to be user1, got: %s", peer2)
	}
}

// 3. نفس المستخدم مرتين -> ErrAlreadyInQueue عبر SetNX
func TestSameUserTwice(t *testing.T) {
	m, _ := setupTestMatcher(t)
	ctx := context.Background()

	_, err := m.RequestMatch(ctx, "user1", "EG")
	if err != nil {
		t.Fatalf("expected first request to succeed, got: %v", err)
	}

	// Second request by the same user while in queue
	_, err = m.RequestMatch(ctx, "user1", "EG")
	if !errors.Is(err, ErrAlreadyInQueue) {
		t.Fatalf("expected ErrAlreadyInQueue, got: %v", err)
	}
}

// 4. Anti-repeat: بعد match، نفس الشخصين مش يتربطوا تاني
func TestAntiRepeat(t *testing.T) {
	m, _ := setupTestMatcher(t)
	ctx := context.Background()

	// First match: user1 and user2
	_, _ = m.RequestMatch(ctx, "user1", "EG")
	peer, err := m.RequestMatch(ctx, "user2", "EG")
	if err != nil || peer != "user1" {
		t.Fatalf("expected initial match between user1 and user2, got: %s, err: %v", peer, err)
	}

	// Now both try to match again in the same queue
	p1, err := m.RequestMatch(ctx, "user1", "EG")
	if err != nil {
		t.Fatalf("unexpected error on user1 re-entry: %v", err)
	}
	if p1 != "" {
		t.Fatalf("expected user1 to wait, got peer: %s", p1)
	}

	p2, err := m.RequestMatch(ctx, "user2", "EG")
	if err != nil {
		t.Fatalf("unexpected error on user2 re-entry: %v", err)
	}
	// Anti-repeat must prevent matching user2 with user1
	if p2 == "user1" {
		t.Fatalf("anti-repeat failed: user2 matched with user1 again!")
	}
	if p2 != "" {
		t.Fatalf("expected user2 to wait in queue without matching user1, got peer: %s", p2)
	}
}

// 5. LeaveQueue يحذف من الطابور
func TestLeaveQueue(t *testing.T) {
	m, _ := setupTestMatcher(t)
	ctx := context.Background()

	// user1 joins queue
	_, err := m.RequestMatch(ctx, "user1", "EG")
	if err != nil {
		t.Fatalf("failed to join queue: %v", err)
	}

	// user1 leaves queue
	if err := m.LeaveQueue(ctx, "user1", "EG"); err != nil {
		t.Fatalf("failed to leave queue: %v", err)
	}

	// user1 should now be able to rejoin without ErrAlreadyInQueue
	peer, err := m.RequestMatch(ctx, "user1", "EG")
	if err != nil {
		t.Fatalf("expected user1 to rejoin without error, got: %v", err)
	}
	if peer != "" {
		t.Fatalf("expected user1 to be waiting, got peer: %s", peer)
	}
}

// 6. اختبار TestAntiRepeatThreeUsers مع 3 مستخدمين
func TestAntiRepeatThreeUsers(t *testing.T) {
	m, _ := setupTestMatcher(t)
	ctx := context.Background()

	// Step 1: user1 و user2 يتربطوا
	p1, err := m.RequestMatch(ctx, "user1", "EG")
	if err != nil || p1 != "" {
		t.Fatalf("expected user1 to wait, got: %s, err: %v", p1, err)
	}
	p2, err := m.RequestMatch(ctx, "user2", "EG")
	if err != nil || p2 != "user1" {
		t.Fatalf("expected user2 to match user1, got: %s, err: %v", p2, err)
	}

	// Step 2: user1 و user3 يتربطوا
	p1Re, err := m.RequestMatch(ctx, "user1", "EG")
	if err != nil || p1Re != "" {
		t.Fatalf("expected user1 to wait again, got: %s, err: %v", p1Re, err)
	}
	p3, err := m.RequestMatch(ctx, "user3", "EG")
	if err != nil || p3 != "user1" {
		t.Fatalf("expected user3 to match user1, got: %s, err: %v", p3, err)
	}

	// Step 3: user2 و user3 لازم يتربطوا (anti-repeat بيسمح لأنهم لم يلتقيا بعد)
	p2Re, err := m.RequestMatch(ctx, "user2", "EG")
	if err != nil || p2Re != "" {
		t.Fatalf("expected user2 to wait, got: %s, err: %v", p2Re, err)
	}
	p3Re, err := m.RequestMatch(ctx, "user3", "EG")
	if err != nil || p3Re != "user2" {
		t.Fatalf("expected user3 to match user2, got: %s, err: %v", p3Re, err)
	}

	// Step 4: بعد كده، لو user1 و user2 في الطابور في نفس الوقت، ميربطوش مع بعض
	p1Third, err := m.RequestMatch(ctx, "user1", "EG")
	if err != nil || p1Third != "" {
		t.Fatalf("expected user1 to wait, got: %s, err: %v", p1Third, err)
	}
	p2Third, err := m.RequestMatch(ctx, "user2", "EG")
	if err != nil {
		t.Fatalf("unexpected error on user2 entry: %v", err)
	}
	if p2Third == "user1" {
		t.Fatalf("anti-repeat failed! user2 matched with user1 again")
	}
	if p2Third != "" {
		t.Fatalf("expected user2 to wait without matching user1, got: %s", p2Third)
	}

	// Step 5: عند دخول مستخدم رابع (user4)، يرتبط بأحدهما (FIFO) لأن anti-repeat لا يمنعهما
	p4, err := m.RequestMatch(ctx, "user4", "EG")
	if err != nil {
		t.Fatalf("unexpected error on user4 entry: %v", err)
	}
	if p4 != "user1" && p4 != "user2" {
		t.Fatalf("expected user4 to match with either user1 or user2, got: %s", p4)
	}
}
