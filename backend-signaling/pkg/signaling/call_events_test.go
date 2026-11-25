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
	"testing"
	"time"
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
	const secret = "test_signaling_hmac_secret"
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
