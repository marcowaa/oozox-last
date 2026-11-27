package signaling

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"sync"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	"github.com/redis/go-redis/v9"
)

func TestSendToDeviceTargetsOnlyIntendedDevice(t *testing.T) {
	hub := NewHub(nil)
	oldPhone := NewClientWithDevice("user_1", "dev_old", nil)
	newPhone := NewClientWithDevice("user_1", "dev_new", nil)

	hub.Register(oldPhone)
	hub.Register(newPhone)

	msg := []byte(`{"type":"force_logout"}`)
	if ok := hub.SendToDevice("user_1", "dev_old", msg); !ok {
		t.Fatal("expected delivery to dev_old")
	}

	select {
	case got := <-oldPhone.Send:
		if string(got) != string(msg) {
			t.Fatalf("unexpected payload on dev_old: %s", string(got))
		}
	case <-time.After(500 * time.Millisecond):
		t.Fatal("timed out waiting for dev_old")
	}

	select {
	case extra := <-newPhone.Send:
		t.Fatalf("dev_new should NOT receive dev_old targeted message, got %s", string(extra))
	case <-time.After(80 * time.Millisecond):
		// expected
	}
}

func TestCallEventReporterSignsWebhookAndVerifiesCallAuth(t *testing.T) {
	const secret = "test_signaling_hmac_secret_32chars"
	t.Setenv("INTERNAL_SIGNALING_HMAC_SECRET", secret)

	var gotSig, gotTs string
	var gotBody []byte
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		gotSig = r.Header.Get("X-Signaling-Signature")
		gotTs = r.Header.Get("X-Signaling-Timestamp")
		gotBody, _ = io.ReadAll(r.Body)
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	reporter := &CallEventReporter{
		BackendURL: srv.URL,
		HMACSecret: secret,
		HTTPClient: srv.Client(),
	}

	err := reporter.Report(context.Background(), CallEventPayload{
		CallID:    "call_123",
		EventID:   "call_123:ACCEPTED",
		EventType: "ACCEPTED",
		CallerID:  "u1",
		CalleeID:  "u2",
		CallType:  "VIDEO",
	})
	if err != nil {
		t.Fatalf("Report returned error: %v", err)
	}

	mac := hmac.New(sha256.New, []byte(secret))
	_, _ = mac.Write([]byte(gotTs + "."))
	_, _ = mac.Write(gotBody)
	expected := hex.EncodeToString(mac.Sum(nil))
	if gotSig != expected {
		t.Fatalf("signature mismatch: got %s want %s", gotSig, expected)
	}

	exp := time.Now().Add(2 * time.Minute).Unix()
	sig := ComputeCallAuthSignature(secret, "call_123", "u1", "u2", "VIDEO", exp)
	if !VerifyCallAuthToken("call_123", "u1", "u2", "VIDEO", exp, sig) {
		t.Fatal("expected VerifyCallAuthToken to pass")
	}
	if VerifyCallAuthToken("call_123", "u1", "u2", "VIDEO", exp, "bad_sig") {
		t.Fatal("expected VerifyCallAuthToken to reject bad signature")
	}
}

func TestPubSubRealtimeEventEnvelopeRoutesToUser(t *testing.T) {
	_, hub, mr := setupTestBridge(t)
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	client := NewClientWithDevice("u_chat", "dev_1", nil)
	hub.Register(client)
	go hub.Run(ctx)
	time.Sleep(60 * time.Millisecond)

	envBytes, _ := json.Marshal(RealtimeEventEnvelope{
		Type:          "chat:message",
		TargetUserIDs: []string{"u_chat"},
		Payload:       json.RawMessage(`{"text":"hello"}`),
	})
	mr.Publish("oozox:realtime:events", string(envBytes))

	select {
	case msg := <-client.Send:
		var out map[string]interface{}
		if err := json.Unmarshal(msg, &out); err != nil {
			t.Fatalf("unmarshal failed: %v", err)
		}
		if out["type"] != "chat:message" {
			t.Fatalf("expected chat:message, got %v", out["type"])
		}
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for realtime event")
	}
}

func TestProductionRejectsUnauthenticatedCallOffer(t *testing.T) {
	mr := miniredis.RunT(t)
	rdb := redis.NewClient(&redis.Options{Addr: mr.Addr()})
	hub := NewHub(rdb)

	const secret = "prod_signaling_hmac_secret_at_least_32_chars"
	t.Setenv("APP_ENV", "production")
	t.Setenv("INTERNAL_SIGNALING_HMAC_SECRET", secret)
	t.Setenv("REQUIRE_CALL_AUTH_SIGNATURE", "false") // Production must still enforce signature

	caller := NewClientWithDevice("caller_1", "dev_a", nil)
	hub.Register(caller)

	rawOffer, _ := json.Marshal(map[string]interface{}{
		"type":     "CALL_OFFER",
		"callId":   "call_unauth_1",
		"target":   "callee_2",
		"callType": "AUDIO",
	})

	if InspectAndReportCallSignal(context.Background(), rdb, nil, hub, "caller_1", rawOffer) {
		t.Fatal("expected production mode to reject CALL_OFFER without valid callAuthSignature")
	}

	select {
	case msg := <-caller.Send:
		var out map[string]interface{}
		_ = json.Unmarshal(msg, &out)
		if out["type"] != "CALL_BUDGET_BLOCKED" {
			t.Fatalf("expected CALL_BUDGET_BLOCKED notification, got %v", out["type"])
		}
	case <-time.After(500 * time.Millisecond):
		t.Fatal("expected caller to receive CALL_BUDGET_BLOCKED")
	}
}

func TestRedisSessionRoleValidationAndSpoofPrevention(t *testing.T) {
	mr := miniredis.RunT(t)
	rdb := redis.NewClient(&redis.Options{Addr: mr.Addr()})
	hub := NewHub(rdb)

	const secret = "prod_signaling_hmac_secret_at_least_32_chars"
	t.Setenv("APP_ENV", "production")
	t.Setenv("INTERNAL_SIGNALING_HMAC_SECRET", secret)

	var mu sync.Mutex
	var events []CallEventPayload
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		raw, _ := io.ReadAll(r.Body)
		var ev CallEventPayload
		if err := json.Unmarshal(raw, &ev); err == nil {
			mu.Lock()
			events = append(events, ev)
			mu.Unlock()
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	reporter := &CallEventReporter{
		BackendURL: srv.URL,
		HMACSecret: secret,
		HTTPClient: srv.Client(),
	}

	exp := time.Now().Add(2 * time.Minute).Unix()
	sig := ComputeCallAuthSignature(secret, "call_sec_1", "caller_1", "callee_2", "VIDEO", exp)
	offerMsg, _ := json.Marshal(map[string]interface{}{
		"type":              "CALL_OFFER",
		"callId":            "call_sec_1",
		"target":            "callee_2",
		"callType":          "VIDEO",
		"callAuthExp":       exp,
		"callAuthSignature": sig,
	})
	if !InspectAndReportCallSignal(context.Background(), rdb, reporter, hub, "caller_1", offerMsg) {
		t.Fatal("expected valid signed CALL_OFFER to succeed")
	}

	// Verify Redis TTL exceeds 4h max call duration (default 5h)
	ttl := mr.TTL(callSessionRedisPrefix + "call_sec_1")
	if ttl <= 4*time.Hour {
		t.Fatalf("expected Redis call session TTL > 4h, got %v", ttl)
	}

	// Non-participant attacker cannot accept or end call
	acceptPayload, _ := json.Marshal(map[string]interface{}{
		"type":   "CALL_ACCEPT",
		"callId": "call_sec_1",
		"target": "caller_1",
	})
	if InspectAndReportCallSignal(context.Background(), rdb, reporter, hub, "attacker_3", acceptPayload) {
		t.Fatal("expected non-participant CALL_ACCEPT to be dropped")
	}
	// Caller cannot spoof CALL_ACCEPT on their own behalf
	if InspectAndReportCallSignal(context.Background(), rdb, reporter, hub, "caller_1", acceptPayload) {
		t.Fatal("expected caller CALL_ACCEPT to be dropped")
	}

	// Callee passing spoofed/wrong target still records true Redis participants
	spoofedTargetAccept, _ := json.Marshal(map[string]interface{}{
		"type":     "CALL_ACCEPT",
		"callId":   "call_sec_1",
		"target":   "wrong_user_999",
		"callType": "AUDIO", // Also tries to downgrade VIDEO -> AUDIO
	})
	if !InspectAndReportCallSignal(context.Background(), rdb, reporter, hub, "callee_2", spoofedTargetAccept) {
		t.Fatal("expected valid callee CALL_ACCEPT to pass while ignoring client target")
	}

	// Heartbeat refreshes TTL
	mr.FastForward(2 * time.Hour)
	hbMsg, _ := json.Marshal(map[string]interface{}{
		"type":   "CALL_HEARTBEAT",
		"callId": "call_sec_1",
	})
	if !InspectAndReportCallSignal(context.Background(), rdb, reporter, hub, "caller_1", hbMsg) {
		t.Fatal("expected CALL_HEARTBEAT to pass")
	}
	refreshedTTL := mr.TTL(callSessionRedisPrefix + "call_sec_1")
	if refreshedTTL <= 4*time.Hour {
		t.Fatalf("expected refreshed TTL > 4h after heartbeat, got %v", refreshedTTL)
	}

	// Unknown/expired callId must be rejected
	unknownAccept, _ := json.Marshal(map[string]interface{}{
		"type":   "CALL_ACCEPT",
		"callId": "call_expired_999",
		"target": "caller_1",
	})
	if InspectAndReportCallSignal(context.Background(), rdb, reporter, hub, "callee_2", unknownAccept) {
		t.Fatal("expected unknown/expired callId to be dropped")
	}

	// Non-participant cannot end call
	endMsg, _ := json.Marshal(map[string]interface{}{
		"type":   "CALL_END",
		"callId": "call_sec_1",
		"target": "caller_1",
	})
	if InspectAndReportCallSignal(context.Background(), rdb, reporter, hub, "attacker_3", endMsg) {
		t.Fatal("expected non-participant CALL_END to be dropped")
	}

	// Give async reporters a moment to deliver webhooks
	time.Sleep(120 * time.Millisecond)
	mu.Lock()
	defer mu.Unlock()
	var acceptedEv *CallEventPayload
	for i := range events {
		if events[i].EventType == "ACCEPTED" {
			acceptedEv = &events[i]
			break
		}
	}
	if acceptedEv == nil {
		t.Fatalf("expected ACCEPTED webhook event, got %+v", events)
	}
	if acceptedEv.CallerID != "caller_1" || acceptedEv.CalleeID != "callee_2" || acceptedEv.CallType != "VIDEO" {
		t.Fatalf("expected webhook IDs derived strictly from Redis (caller_1/callee_2/VIDEO), got %+v", acceptedEv)
	}
}
