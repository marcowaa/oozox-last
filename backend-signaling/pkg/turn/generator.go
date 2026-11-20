package turn

import (
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"fmt"
	"os"
	"strings"
	"time"
)

// Credentials holds the temporary TURN configuration returned to clients.
type Credentials struct {
	URLs       []string `json:"urls"`
	Username   string   `json:"username"`
	Credential string   `json:"credential"`
	TTL        int      `json:"ttl"`
}

// Constants for TURN credentials security
const (
	// DefaultTurnTTL strictly enforces 1 hour (3600s) lifetime, replacing the previous 24-hour setting
	DefaultTurnTTL = 3600
	// MaxTurnTTL strictly caps ephemeral TURN credential lifespan to 1 hour
	MaxTurnTTL = 3600
)

// GenerateCredentialsAt creates time-limited ephemeral TURN credentials based on a reference unix timestamp.
func GenerateCredentialsAt(userID string, ttlSeconds int, nowUnix int64) (username, credential string, err error) {
	secret := os.Getenv("COTURN_SECRET")
	if secret == "" {
		return "", "", fmt.Errorf("COTURN_SECRET environment variable is missing")
	}
	if len(secret) < 16 {
		return "", "", fmt.Errorf("COTURN_SECRET must be set and at least 16 characters long")
	}

	// Strictly enforce 1-hour expiration time (replacing any 24-hour setting)
	if ttlSeconds <= 0 || ttlSeconds > MaxTurnTTL {
		ttlSeconds = DefaultTurnTTL
	}

	expiry := nowUnix + int64(ttlSeconds)
	username = fmt.Sprintf("%d:%s", expiry, userID)

	mac := hmac.New(sha1.New, []byte(secret))
	mac.Write([]byte(username))
	credential = base64.StdEncoding.EncodeToString(mac.Sum(nil))

	return username, credential, nil
}

// GenerateCredentials creates time-limited ephemeral TURN credentials using HMAC-SHA1.
func GenerateCredentials(userID string, ttlSeconds int) (username, credential string, err error) {
	return GenerateCredentialsAt(userID, ttlSeconds, time.Now().Unix())
}

// TurnURLs returns the list of configured TURN server URLs from the TURN_URLS environment variable.
func TurnURLs() []string {
	raw := os.Getenv("TURN_URLS")
	if strings.TrimSpace(raw) == "" {
		return []string{}
	}

	parts := strings.Split(raw, ",")
	urls := make([]string, 0, len(parts))
	for _, p := range parts {
		trimmed := strings.TrimSpace(p)
		if trimmed != "" {
			urls = append(urls, trimmed)
		}
	}

	return urls
}
