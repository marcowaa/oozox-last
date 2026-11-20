package turn

import (
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"strconv"
	"strings"
	"testing"
	"time"
)

// 1. TestGenerateCredentialsDeterministic verifies that HMAC-SHA1 output strictly matches
// the manual HMAC calculation without relying on flaky time.Now() calls.
func TestGenerateCredentialsDeterministic(t *testing.T) {
	secret := "test-secret-32-chars-minimum-xxxxxx"
	t.Setenv("COTURN_SECRET", secret)

	fixedNow := int64(1700000000)
	ttl := 3600
	userID := "bob"

	username, cred, err := GenerateCredentialsAt(userID, ttl, fixedNow)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	expectedUsername := "1700003600:bob"
	if username != expectedUsername {
		t.Fatalf("expected username %s, got %s", expectedUsername, username)
	}

	mac := hmac.New(sha1.New, []byte(secret))
	mac.Write([]byte(expectedUsername))
	expectedCred := base64.StdEncoding.EncodeToString(mac.Sum(nil))

	if cred != expectedCred {
		t.Fatalf("expected credential %s, got %s", expectedCred, cred)
	}
}

// 2. TestGenerateCredentialsSecretValidation verifies that secret length < 16 returns an error.
func TestGenerateCredentialsSecretValidation(t *testing.T) {
	t.Setenv("COTURN_SECRET", "short")
	_, _, err := GenerateCredentials("bob", 3600)
	if err == nil {
		t.Fatal("expected error when COTURN_SECRET is shorter than 16 characters")
	}

	t.Setenv("COTURN_SECRET", "")
	_, _, err = GenerateCredentials("bob", 3600)
	if err == nil {
		t.Fatal("expected error when COTURN_SECRET is empty")
	}
}

// 3. TestGenerateCredentialsFormat verifies runtime format (expiry:userID) with valid secret.
func TestGenerateCredentialsFormat(t *testing.T) {
	t.Setenv("COTURN_SECRET", "test-turn-secret-key-at-least-32-chars-long")

	userID := "user-alice-123"
	now := time.Now().Unix()

	username, cred, err := GenerateCredentials(userID, 1800)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cred == "" {
		t.Fatal("expected non-empty credential")
	}

	parts := strings.Split(username, ":")
	if len(parts) != 2 {
		t.Fatalf("expected username format 'expiry:userID', got: %s", username)
	}

	expiry, err := strconv.ParseInt(parts[0], 10, 64)
	if err != nil {
		t.Fatalf("failed to parse expiry timestamp: %v", err)
	}

	if parts[1] != userID {
		t.Fatalf("expected userID %s, got: %s", userID, parts[1])
	}

	expectedExpiry := now + 1800
	diff := expiry - expectedExpiry
	if diff < -2 || diff > 2 {
		t.Fatalf("expected expiry around %d, got %d", expectedExpiry, expiry)
	}
}

// 4. TestGenerateCredentialsEnforcesOneHourTTL verifies that requesting 24 hours (86400s) or any TTL > 3600s
// is strictly clamped to a maximum 1 hour (3600s) lifetime.
func TestGenerateCredentialsEnforcesOneHourTTL(t *testing.T) {
	t.Setenv("COTURN_SECRET", "test-turn-secret-key-at-least-32-chars-long")

	fixedNow := int64(1700000000)
	userID := "dave"

	// Requesting 24 hours (86400 seconds) must be clamped to 3600 seconds (1 hour)
	username24h, _, err := GenerateCredentialsAt(userID, 86400, fixedNow)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	expectedUsername1h := "1700003600:dave"
	if username24h != expectedUsername1h {
		t.Fatalf("expected 24h request to be clamped to 1h (%s), got: %s", expectedUsername1h, username24h)
	}

	// Requesting 0 or negative TTL must default to 3600 seconds (1 hour)
	username0, _, err := GenerateCredentialsAt(userID, 0, fixedNow)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if username0 != expectedUsername1h {
		t.Fatalf("expected 0 TTL request to default to 1h (%s), got: %s", expectedUsername1h, username0)
	}
}

// 5. TestTurnURLs tests parsing of comma-separated TURN URLs.
func TestTurnURLs(t *testing.T) {
	// Empty case
	t.Setenv("TURN_URLS", "")
	urls := TurnURLs()
	if len(urls) != 0 {
		t.Fatalf("expected empty slice, got %d elements", len(urls))
	}

	// Populated case
	envVal := "turn:turn.ayer.app:3478?transport=udp, turn:turn.ayer.app:3478?transport=tcp"
	t.Setenv("TURN_URLS", envVal)

	urls = TurnURLs()
	if len(urls) != 2 {
		t.Fatalf("expected 2 URLs, got %d", len(urls))
	}

	expected0 := "turn:turn.ayer.app:3478?transport=udp"
	expected1 := "turn:turn.ayer.app:3478?transport=tcp"
	if urls[0] != expected0 {
		t.Errorf("expected URL[0] to be %s, got: %s", expected0, urls[0])
	}
	if urls[1] != expected1 {
		t.Errorf("expected URL[1] to be %s, got: %s", expected1, urls[1])
	}
}
