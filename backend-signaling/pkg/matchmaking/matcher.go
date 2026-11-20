package matchmaking

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/redis/go-redis/v9"
)

var (
	ErrAlreadyInQueue   = errors.New("user already in queue")
	ErrNoMatchAvailable = errors.New("no match available")
	ErrAllPeersRepeat   = errors.New("all available peers in queue are repeated")
)

const (
	// TTL for user queue waiting state: 5 minutes
	UserQueueTTL = 5 * time.Minute
	// TTL for anti-repeat pair suppression: 30 minutes
	RepeatSuppressionTTL = 1800 * time.Second
)

// Matcher manages matchmaking queues and pairing using Redis.
type Matcher struct {
	rdb *redis.Client
}

// New creates a new Matcher instance with injected Redis client.
func New(rdb *redis.Client) *Matcher {
	return &Matcher{
		rdb: rdb,
	}
}

// repeatKey builds a deterministic key for user pairs regardless of order.
func (m *Matcher) repeatKey(u1, u2 string) string {
	if u1 > u2 {
		u1, u2 = u2, u1
	}
	return fmt.Sprintf("match:repeat:%s:%s", u1, u2)
}

// RequestMatch attempts to match a user with an existing queued user in the same country.
// Returns the matched peerID, or "" if the user was placed in queue to wait.
func (m *Matcher) RequestMatch(ctx context.Context, userID, country string) (string, error) {
	userKey := fmt.Sprintf("match:user:%s", userID)
	queueKey := fmt.Sprintf("match:queue:%s", country)

	// a. Atomic Check-and-Set using SetNX (TTL = 5 minutes) to prevent Race Conditions
	ok, err := m.rdb.SetNX(ctx, userKey, "waiting", UserQueueTTL).Result()
	if err != nil {
		return "", err
	}
	if !ok {
		return "", ErrAlreadyInQueue
	}

	// b. LPUSH user to country queue. Roll back userKey if LPUSH fails.
	if err := m.rdb.LPush(ctx, queueKey, userID).Err(); err != nil {
		_ = m.rdb.Del(ctx, userKey).Err()
		return "", err
	}

	// c-f. RPOP loop with anti-repeat check (max 5 attempts)
	maxAttempts := 5
	for attempt := 0; attempt < maxAttempts; attempt++ {
		// c. RPOP from the same queue
		peerID, err := m.rdb.RPop(ctx, queueKey).Result()
		if errors.Is(err, redis.Nil) {
			// d. Queue is empty: user remains in waiting state (TTL = 5m) and return ""
			return "", nil
		}
		if err != nil {
			return "", err
		}

		// e. Same user popped (userID == peerID): re-enqueue user at LPUSH and return "" (waiting)
		if peerID == userID {
			if err := m.rdb.LPush(ctx, queueKey, userID).Err(); err != nil {
				return "", err
			}
			return "", nil
		}

		// f. Check anti-repeat: match:repeat:{userA}:{userB}
		repKey := m.repeatKey(userID, peerID)
		repExists, err := m.rdb.Exists(ctx, repKey).Result()
		if err != nil {
			return "", err
		}
		if repExists > 0 {
			// Re-queue peerID and retry pop
			if err := m.rdb.LPush(ctx, queueKey, peerID).Err(); err != nil {
				return "", err
			}
			continue
		}

		// g. Successful match:
		// Delete active queue user keys
		peerUserKey := fmt.Sprintf("match:user:%s", peerID)
		if err := m.rdb.Del(ctx, userKey, peerUserKey).Err(); err != nil {
			return "", err
		}

		// إزالة المستخدم الحالي من الطابور لمنع بقاء مدخلات شبح (Ghost entries) بعد نجاح المطابقة
		if err := m.rdb.LRem(ctx, queueKey, 0, userID).Err(); err != nil {
			return "", err
		}

		// Save anti-repeat record with TTL = 1800s (30 minutes)
		if err := m.rdb.Set(ctx, repKey, "1", RepeatSuppressionTTL).Err(); err != nil {
			return "", err
		}

		return peerID, nil
	}

	// If all 5 anti-repeat attempts were exhausted without finding a valid peer:
	// Do NOT fail silently. Clean up user state and return ErrAllPeersRepeat.
	_ = m.rdb.LRem(ctx, queueKey, 0, userID).Err()
	_ = m.rdb.Del(ctx, userKey).Err()

	return "", ErrAllPeersRepeat
}

// LeaveQueue removes the user from the country queue and deletes their waiting key.
func (m *Matcher) LeaveQueue(ctx context.Context, userID, country string) error {
	userKey := fmt.Sprintf("match:user:%s", userID)
	queueKey := fmt.Sprintf("match:queue:%s", country)

	// Remove from Redis list
	if err := m.rdb.LRem(ctx, queueKey, 0, userID).Err(); err != nil {
		return err
	}

	// Delete user queue key
	if err := m.rdb.Del(ctx, userKey).Err(); err != nil {
		return err
	}

	return nil
}
