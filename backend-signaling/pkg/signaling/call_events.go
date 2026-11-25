package signaling

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
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
	secret := strings.TrimSpace(os.Getenv("INTERNAL_SIGNALING_HMAC_SECRET"))
	if secret == "" {
		secret = strings.TrimSpace(os.Getenv("JWT_SECRET"))
	}
	if secret == "" {
		secret = "oozox_dev_signaling_hmac_secret"
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
	secret := strings.TrimSpace(os.Getenv("INTERNAL_SIGNALING_HMAC_SECRET"))
	if secret == "" {
		secret = strings.TrimSpace(os.Getenv("JWT_SECRET"))
	}
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

// InspectAndReportCallSignal enforces call budget signature when required and emits lifecycle webhooks.
// Returns false if a new call offer should be rejected back to the caller.
func InspectAndReportCallSignal(
	ctx context.Context,
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
	callKind := "AUDIO"
	if strings.EqualFold(strings.TrimSpace(env.CallType), "VIDEO") {
		callKind = "VIDEO"
	}

	switch normType {
	case "CALL_OFFER":
		// If signature provided or strict mode enabled in production, enforce Backend 100% budget authorization
		if env.CallAuthSignature != "" || strings.EqualFold(os.Getenv("REQUIRE_CALL_AUTH_SIGNATURE"), "true") {
			if !VerifyCallAuthToken(callID, senderUserID, env.Target, callKind, env.CallAuthExp, env.CallAuthSignature) {
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
		if reporter != nil && callID != "" {
			go func() {
				_ = reporter.Report(context.Background(), CallEventPayload{
					CallID:    callID,
					EventID:   callID + ":RINGING",
					EventType: "RINGING",
					CallerID:  senderUserID,
					CalleeID:  env.Target,
					CallType:  callKind,
				})
			}()
		}
	case "CALL_ACCEPT", "CALL_ACCEPTED":
		if reporter != nil && callID != "" {
			go func() {
				_ = reporter.Report(context.Background(), CallEventPayload{
					CallID:    callID,
					EventID:   callID + ":ACCEPTED",
					EventType: "ACCEPTED",
					CallerID:  env.Target,
					CalleeID:  senderUserID,
					CallType:  callKind,
				})
			}()
		}
	case "CALL_END", "CALL_ENDED":
		if reporter != nil && callID != "" {
			go func() {
				_ = reporter.Report(context.Background(), CallEventPayload{
					CallID:    callID,
					EventID:   callID + ":ENDED",
					EventType: "ENDED",
					CallerID:  senderUserID,
					CalleeID:  env.Target,
					CallType:  callKind,
					EndReason: env.Reason,
				})
			}()
		}
	case "CALL_REJECT", "CALL_MISSED":
		if reporter != nil && callID != "" {
			evName := "REJECTED"
			if normType == "CALL_MISSED" {
				evName = "MISSED"
			}
			go func() {
				_ = reporter.Report(context.Background(), CallEventPayload{
					CallID:    callID,
					EventID:   callID + ":" + evName,
					EventType: evName,
					CallerID:  env.Target,
					CalleeID:  senderUserID,
					CallType:  callKind,
					EndReason: env.Reason,
				})
			}()
		}
	}

	return true
}
