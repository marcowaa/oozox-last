package signaling

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/redis/go-redis/v9"
)

const (
	defaultMaxCallDurationSeconds = 14400 // 4 hours
	callSessionTTLBuffer          = 1 * time.Hour
	endedCallGraceTTL             = 60 * time.Second
	callSessionRedisPrefix        = "oozox:call:session:"
)

// CallEventPayload mirrors Backend POST /api/calls/events contract.
type CallEventPayload struct {
	CallID    string `json:"callId"`
	EventID   string `json:"eventId"`
	EventType string `json:"eventType"`
	CallerID  string `json:"callerId"`
	CalleeID  string `json:"calleeId"`
	CallType  string `json:"callType"`
	Timestamp string `json:"timestamp"`
	EndReason string `json:"endReason,omitempty"`
}

// CallSessionRecord tracks server-verified participants for an authorized active call in Redis.
type CallSessionRecord struct {
	CallID    string `json:"callId"`
	CallerID  string `json:"callerId"`
	CalleeID  string `json:"calleeId"`
	CallType  string `json:"callType"`
	Ended     bool   `json:"ended"`
	CreatedAt int64  `json:"createdAt"`
}

func isProductionEnv() bool {
	return strings.EqualFold(strings.TrimSpace(os.Getenv("APP_ENV")), "production")
}

// GetMaxCallDuration returns the configured maximum call duration (default 4h).
func GetMaxCallDuration() time.Duration {
	if raw := strings.TrimSpace(os.Getenv("RTC_MAX_CALL_DURATION_SECONDS")); raw != "" {
		if secs, err := strconv.Atoi(raw); err == nil && secs >= 60 {
			return time.Duration(secs) * time.Second
		}
	}
	return defaultMaxCallDurationSeconds * time.Second
}

// GetCallSessionTTL returns a Redis TTL strictly exceeding max call duration (max + 1h, default 5h)
// so calls approaching max duration never lose their Redis authorization state.
func GetCallSessionTTL() time.Duration {
	return GetMaxCallDuration() + callSessionTTLBuffer
}

func resolveSignalingHMACSecret() string {
	secret := strings.TrimSpace(os.Getenv("INTERNAL_SIGNALING_HMAC_SECRET"))
	if isProductionEnv() {
		return secret
	}
	if secret != "" {
		return secret
	}
	return strings.TrimSpace(os.Getenv("JWT_SECRET"))
}

// ValidateSignalingHMACSecret verifies that INTERNAL_SIGNALING_HMAC_SECRET meets production rules.
func ValidateSignalingHMACSecret() error {
	secret := strings.TrimSpace(os.Getenv("INTERNAL_SIGNALING_HMAC_SECRET"))
	if isProductionEnv() {
		if len(secret) < 32 {
			return fmt.Errorf("INTERNAL_SIGNALING_HMAC_SECRET must be set and at least 32 chars in production")
		}
	} else if resolveSignalingHMACSecret() == "" {
		return fmt.Errorf("INTERNAL_SIGNALING_HMAC_SECRET (or JWT_SECRET in dev) must be configured")
	}
	return nil
}

// ComputeCallAuthSignature verifies client pre-call authorization signed by Backend /api/calls/authorize.
func ComputeCallAuthSignature(secret, callID, callerID, calleeID, callType string, exp int64) string {
	canonical := fmt.Sprintf("%s:%s:%s:%s:%d", callID, callerID, calleeID, callType, exp)
	mac := hmac.New(sha256.New, []byte(secret))
	_, _ = mac.Write([]byte(canonical))
	return hex.EncodeToString(mac.Sum(nil))
}

// VerifyCallAuthToken validates callAuthSignature & expiration issued by Backend /api/calls/authorize.
func VerifyCallAuthToken(callID, callerID, calleeID, callType string, exp int64, signature string) bool {
	if callID == "" || callerID == "" || calleeID == "" || signature == "" {
		return false
	}
	if time.Now().Unix() > exp {
		return false
	}
	secret := resolveSignalingHMACSecret()
	if secret == "" || (isProductionEnv() && len(secret) < 32) {
		return false
	}
	normalizedType := "AUDIO"
	if strings.EqualFold(strings.TrimSpace(callType), "VIDEO") {
		normalizedType = "VIDEO"
	}
	expected := ComputeCallAuthSignature(secret, callID, callerID, calleeID, normalizedType, exp)
	return hmac.Equal([]byte(strings.ToLower(signature)), []byte(expected))
}

// SignWebhookPayload signs `<ts>.<rawBody>` using HMAC-SHA256 for Backend /api/calls/events.
func SignWebhookPayload(secret string, ts int64, rawBody []byte) string {
	mac := hmac.New(sha256.New, []byte(secret))
	_, _ = mac.Write([]byte(fmt.Sprintf("%d.", ts)))
	_, _ = mac.Write(rawBody)
	return hex.EncodeToString(mac.Sum(nil))
}

// CallEventReporter posts idempotent HMAC-signed call lifecycle webhooks to Node Backend.
type CallEventReporter struct {
	BackendURL string
	HMACSecret string
	HTTPClient *http.Client
}

// NewCallEventReporterFromEnv creates a reporter using BACKEND_INTERNAL_URL and INTERNAL_SIGNALING_HMAC_SECRET.
func NewCallEventReporterFromEnv() *CallEventReporter {
	backendURL := strings.TrimRight(strings.TrimSpace(os.Getenv("BACKEND_INTERNAL_URL")), "/")
	secret := resolveSignalingHMACSecret()
	return &CallEventReporter{
		BackendURL: backendURL,
		HMACSecret: secret,
		HTTPClient: &http.Client{Timeout: 4 * time.Second},
	}
}

// Report sends one call lifecycle event asynchronously-safe with HMAC signature.
func (r *CallEventReporter) Report(ctx context.Context, ev CallEventPayload) error {
	if r == nil || r.BackendURL == "" || ev.CallID == "" || ev.EventType == "" {
		return nil
	}
	if ev.Timestamp == "" {
		ev.Timestamp = time.Now().UTC().Format(time.RFC3339Nano)
	}
	if ev.EventID == "" {
		ev.EventID = fmt.Sprintf("%s:%s:%d", ev.CallID, ev.EventType, time.Now().UnixNano())
	}
	raw, err := json.Marshal(ev)
	if err != nil {
		return err
	}
	ts := time.Now().Unix()
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, r.BackendURL+"/api/calls/events", bytes.NewReader(raw))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Signaling-Timestamp", strconv.FormatInt(ts, 10))
	if r.HMACSecret != "" {
		req.Header.Set("X-Signaling-Signature", SignWebhookPayload(r.HMACSecret, ts, raw))
	}

	resp, err := r.HTTPClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 400 {
		return fmt.Errorf("backend /api/calls/events returned %d", resp.StatusCode)
	}
	return nil
}

// IncomingSignalEnvelope captures signaling fields needed for budget guard & lifecycle logging.
type IncomingSignalEnvelope struct {
	Type              string `json:"type"`
	Target            string `json:"target"`
	CallID            string `json:"callId"`
	CallType          string `json:"callType"`
	CallAuthExp       int64  `json:"callAuthExp"`
	CallAuthSignature string `json:"callAuthSignature"`
	Reason            string `json:"reason"`
}

func saveCallSession(ctx context.Context, rdb *redis.Client, rec CallSessionRecord, ttl time.Duration) error {
	if rdb == nil {
		return fmt.Errorf("redis client is nil")
	}
	raw, err := json.Marshal(rec)
	if err != nil {
		return err
	}
	return rdb.Set(ctx, callSessionRedisPrefix+rec.CallID, raw, ttl).Err()
}

func loadCallSession(ctx context.Context, rdb *redis.Client, callID string) (*CallSessionRecord, error) {
	if rdb == nil {
		return nil, fmt.Errorf("redis client is nil")
	}
	raw, err := rdb.Get(ctx, callSessionRedisPrefix+callID).Bytes()
	if err != nil {
		return nil, err
	}
	var rec CallSessionRecord
	if err := json.Unmarshal(raw, &rec); err != nil {
		return nil, err
	}
	return &rec, nil
}

// InspectAndReportCallSignal enforces call budget signature when required, tracks call participants in Redis,
// verifies sender role for call lifecycle events, and emits lifecycle webhooks derived strictly from Redis.
// Returns false if the signal must be dropped / rejected.
func InspectAndReportCallSignal(
	ctx context.Context,
	rdb *redis.Client,
	reporter *CallEventReporter,
	hub *Hub,
	senderUserID string,
	rawMsg []byte,
) bool {
	var env IncomingSignalEnvelope
	if err := json.Unmarshal(rawMsg, &env); err != nil || env.Type == "" {
		return true
	}

	normType := strings.ToUpper(strings.TrimSpace(env.Type))
	callID := strings.TrimSpace(env.CallID)
	targetID := strings.TrimSpace(env.Target)
	callKind := "AUDIO"
	if strings.EqualFold(strings.TrimSpace(env.CallType), "VIDEO") {
		callKind = "VIDEO"
	}
	sessionTTL := GetCallSessionTTL()

	switch normType {
	case "CALL_OFFER":
		requireSig := isProductionEnv() ||
			env.CallAuthSignature != "" ||
			strings.EqualFold(os.Getenv("REQUIRE_CALL_AUTH_SIGNATURE"), "true")

		if requireSig {
			if !VerifyCallAuthToken(callID, senderUserID, targetID, callKind, env.CallAuthExp, env.CallAuthSignature) {
				if hub != nil {
					blockedPayload, _ := json.Marshal(map[string]interface{}{
						"type":   "CALL_BUDGET_BLOCKED",
						"callId": callID,
						"code":   "RTC_AUTHORIZATION_REQUIRED_OR_BUDGET_EXCEEDED",
					})
					hub.SendTo(senderUserID, blockedPayload)
				}
				return false
			}
		}
		if callID == "" || targetID == "" || targetID == senderUserID {
			log.Printf("[Signaling] Dropping malformed CALL_OFFER from %s (callId=%q, target=%q)", senderUserID, callID, targetID)
			return false
		}
		if rdb == nil {
			if isProductionEnv() {
				log.Printf("[Signaling] Dropping CALL_OFFER in production because Redis is unavailable")
				return false
			}
		} else {
			rec := CallSessionRecord{
				CallID:    callID,
				CallerID:  senderUserID,
				CalleeID:  targetID,
				CallType:  callKind,
				Ended:     false,
				CreatedAt: time.Now().Unix(),
			}
			if err := saveCallSession(ctx, rdb, rec, sessionTTL); err != nil {
				log.Printf("[Signaling] Failed saving call session %s to Redis: %v", callID, err)
				return false
			}
		}
		if reporter != nil {
			go func() {
				_ = reporter.Report(context.Background(), CallEventPayload{
					CallID:    callID,
					EventID:   callID + ":RINGING",
					EventType: "RINGING",
					CallerID:  senderUserID,
					CalleeID:  targetID,
					CallType:  callKind,
				})
			}()
		}

	case "CALL_ACCEPT", "CALL_ACCEPTED":
		if callID == "" || rdb == nil {
			log.Printf("[Signaling] Dropping %s without active callId/Redis (sender=%s)", normType, senderUserID)
			return false
		}
		rec, err := loadCallSession(ctx, rdb, callID)
		if err != nil {
			log.Printf("[Signaling] Dropping %s for unknown/expired callId %s (sender=%s): %v", normType, callID, senderUserID, err)
			return false
		}
		if rec.Ended {
			log.Printf("[Signaling] Dropping %s for already-ended callId %s (sender=%s)", normType, callID, senderUserID)
			return false
		}
		if senderUserID != rec.CalleeID {
			log.Printf("[Signaling] SECURITY: Rejecting spoofed %s for call %s from %s (expected callee %s)", normType, callID, senderUserID, rec.CalleeID)
			return false
		}
		// Refresh TTL on active acceptance so long calls stay valid beyond max duration
		_ = rdb.Expire(ctx, callSessionRedisPrefix+callID, sessionTTL).Err()

		if reporter != nil {
			caller, callee, cType := rec.CallerID, rec.CalleeID, rec.CallType
			go func() {
				_ = reporter.Report(context.Background(), CallEventPayload{
					CallID:    callID,
					EventID:   callID + ":ACCEPTED",
					EventType: "ACCEPTED",
					CallerID:  caller,
					CalleeID:  callee,
					CallType:  cType,
				})
			}()
		}

	case "CALL_END", "CALL_ENDED", "CALL_MISSED":
		if callID == "" || rdb == nil {
			log.Printf("[Signaling] Dropping %s without active callId/Redis (sender=%s)", normType, senderUserID)
			return false
		}
		rec, err := loadCallSession(ctx, rdb, callID)
		if err != nil {
			log.Printf("[Signaling] Dropping %s for unknown/expired callId %s (sender=%s): %v", normType, callID, senderUserID, err)
			return false
		}
		if senderUserID != rec.CallerID && senderUserID != rec.CalleeID {
			log.Printf("[Signaling] SECURITY: Rejecting spoofed %s for call %s from non-participant %s", normType, callID, senderUserID)
			return false
		}
		evName := "ENDED"
		if normType == "CALL_MISSED" {
			evName = "MISSED"
		}
		rec.Ended = true
		_ = saveCallSession(ctx, rdb, *rec, endedCallGraceTTL)

		if reporter != nil {
			caller, callee, cType := rec.CallerID, rec.CalleeID, rec.CallType
			reason := env.Reason
			go func() {
				_ = reporter.Report(context.Background(), CallEventPayload{
					CallID:    callID,
					EventID:   callID + ":" + evName,
					EventType: evName,
					CallerID:  caller,
					CalleeID:  callee,
					CallType:  cType,
					EndReason: reason,
				})
			}()
		}

	case "CALL_REJECT":
		if callID == "" || rdb == nil {
			log.Printf("[Signaling] Dropping CALL_REJECT without active callId/Redis (sender=%s)", senderUserID)
			return false
		}
		rec, err := loadCallSession(ctx, rdb, callID)
		if err != nil {
			log.Printf("[Signaling] Dropping CALL_REJECT for unknown/expired callId %s (sender=%s): %v", callID, senderUserID, err)
			return false
		}
		if senderUserID != rec.CalleeID {
			log.Printf("[Signaling] SECURITY: Rejecting spoofed CALL_REJECT for call %s from %s (expected callee %s)", callID, senderUserID, rec.CalleeID)
			return false
		}
		rec.Ended = true
		_ = saveCallSession(ctx, rdb, *rec, endedCallGraceTTL)

		if reporter != nil {
			caller, callee, cType := rec.CallerID, rec.CalleeID, rec.CallType
			reason := env.Reason
			go func() {
				_ = reporter.Report(context.Background(), CallEventPayload{
					CallID:    callID,
					EventID:   callID + ":REJECTED",
					EventType: "REJECTED",
					CallerID:  caller,
					CalleeID:  callee,
					CallType:  cType,
					EndReason: reason,
				})
			}()
		}

	case "CALL_ANSWER", "ICE_CANDIDATE", "CALL_HEARTBEAT", "HEARTBEAT":
		// Refresh Redis call session TTL on legitimate ongoing call traffic / heartbeats
		if callID != "" && rdb != nil {
			if rec, err := loadCallSession(ctx, rdb, callID); err == nil && !rec.Ended {
				if senderUserID == rec.CallerID || senderUserID == rec.CalleeID {
					_ = rdb.Expire(ctx, callSessionRedisPrefix+callID, sessionTTL).Err()
				}
			}
		}
	}

	return true
}
