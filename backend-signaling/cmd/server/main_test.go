package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"runtime"
	"testing"
	"time"

	"backend-signaling/pkg/loadshedding"
	"backend-signaling/pkg/matchmaking"
	"backend-signaling/pkg/signaling"

	"github.com/alicebob/miniredis/v2"
	"github.com/golang-jwt/jwt/v5"
	"github.com/redis/go-redis/v9"
)

func createTestToken(userID string, secret []byte) string {
	claims := jwt.MapClaims{
		"sub": userID,
		"exp": time.Now().Add(time.Hour).Unix(),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, _ := token.SignedString(secret)
	return tokenString
}

func setupIntegrationServer(t *testing.T) (*Server, *miniredis.Miniredis) {
	mr := miniredis.RunT(t)
	rdb := redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})

	hub := signaling.NewHub(rdb)
	matcher := matchmaking.New(rdb)
	bridge := signaling.NewBridge(hub, rdb)
	secret := []byte("test-jwt-secret")
	loadShedder := loadshedding.NewLoadShedder(loadshedding.ConfigFromEnv())

	server := &Server{
		hub:         hub,
		matcher:     matcher,
		bridge:      bridge,
		rdb:         rdb,
		jwtSecret:   secret,
		loadShedder: loadShedder,
	}

	return server, mr
}

func TestTurnCredentialsIntegration(t *testing.T) {
	server, _ := setupIntegrationServer(t)
	tokenCharlie := createTestToken("charlie", server.jwtSecret)

	// 1. Without Token -> Should return 401 Unauthorized
	req401 := httptest.NewRequest(http.MethodPost, "/turn-credentials", nil)
	rec401 := httptest.NewRecorder()
	server.handleTurnCredentials(rec401, req401)
	if rec401.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401 when token missing, got %d", rec401.Code)
	}

	// 2. Without TURN_URLS set -> Should return 503
	t.Setenv("TURN_URLS", "")
	req503 := httptest.NewRequest(http.MethodPost, "/turn-credentials", nil)
	req503.Header.Set("Authorization", "Bearer "+tokenCharlie)
	rec503 := httptest.NewRecorder()

	server.handleTurnCredentials(rec503, req503)
	if rec503.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected 503 when TURN_URLS empty, got %d", rec503.Code)
	}

	// 3. With TURN_URLS and COTURN_SECRET set -> Should return 200 with credentials
	t.Setenv("TURN_URLS", "turn:turn.ayer.app:3478?transport=udp")
	t.Setenv("COTURN_SECRET", "super-secret-turn-key-at-least-32-chars-long")

	req200 := httptest.NewRequest(http.MethodPost, "/turn-credentials", bytes.NewReader([]byte(`{"ttl": 1800}`)))
	req200.Header.Set("Authorization", "Bearer "+tokenCharlie)
	rec200 := httptest.NewRecorder()

	server.handleTurnCredentials(rec200, req200)
	if rec200.Code != http.StatusOK {
		t.Fatalf("expected 200 for charlie, got %d: %s", rec200.Code, rec200.Body.String())
	}

	var credResp struct {
		URLs       []string `json:"urls"`
		Username   string   `json:"username"`
		Credential string   `json:"credential"`
		TTL        int      `json:"ttl"`
	}
	if err := json.NewDecoder(rec200.Body).Decode(&credResp); err != nil {
		t.Fatalf("failed to decode credentials response: %v", err)
	}
	if len(credResp.URLs) == 0 || credResp.Username == "" || credResp.Credential == "" {
		t.Fatalf("incomplete credentials: %+v", credResp)
	}
	if credResp.TTL != 1800 {
		t.Fatalf("expected TTL 1800, got %d", credResp.TTL)
	}

	// 4. Immediate second request by charlie -> Should return 429 Too Many Requests (Rate limit 30s)
	req429 := httptest.NewRequest(http.MethodPost, "/turn-credentials", nil)
	req429.Header.Set("Authorization", "Bearer "+tokenCharlie)
	rec429 := httptest.NewRecorder()

	server.handleTurnCredentials(rec429, req429)
	if rec429.Code != http.StatusTooManyRequests {
		t.Fatalf("expected 429 rate limit exceeded, got %d", rec429.Code)
	}

	// 5. Test another user requesting 24 hours (86400s) -> Must strictly receive 1 hour (3600s) TTL
	tokenDave := createTestToken("dave", server.jwtSecret)
	req24h := httptest.NewRequest(http.MethodPost, "/turn-credentials", bytes.NewReader([]byte(`{"ttl": 86400}`)))
	req24h.Header.Set("Authorization", "Bearer "+tokenDave)
	rec24h := httptest.NewRecorder()

	server.handleTurnCredentials(rec24h, req24h)
	if rec24h.Code != http.StatusOK {
		t.Fatalf("expected 200 for dave, got %d: %s", rec24h.Code, rec24h.Body.String())
	}
	var credResp24h struct {
		TTL int `json:"ttl"`
	}
	if err := json.NewDecoder(rec24h.Body).Decode(&credResp24h); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}
	if credResp24h.TTL != 3600 {
		t.Fatalf("expected 24h request to be strictly clamped to 3600s (1h), got %d", credResp24h.TTL)
	}
}

func TestHTTPMatchTwoUsersIntegration(t *testing.T) {
	server, _ := setupIntegrationServer(t)

	tokenAlice := createTestToken("alice", server.jwtSecret)
	tokenBob := createTestToken("bob", server.jwtSecret)

	// Register Alice locally in the Hub to capture match_found notification
	clientAlice := signaling.NewClient("alice", nil)
	server.hub.Register(clientAlice)

	// 1. Alice requests match (Country: "EG") -> Should wait
	bodyAlice, _ := json.Marshal(MatchRequest{Country: "EG"})
	reqAlice := httptest.NewRequest(http.MethodPost, "/match", bytes.NewReader(bodyAlice))
	reqAlice.Header.Set("Authorization", "Bearer "+tokenAlice)
	recAlice := httptest.NewRecorder()

	server.handleMatch(recAlice, reqAlice)

	if recAlice.Code != http.StatusOK {
		t.Fatalf("expected status 200 for Alice, got %d", recAlice.Code)
	}

	var resAlice MatchResponse
	if err := json.NewDecoder(recAlice.Body).Decode(&resAlice); err != nil {
		t.Fatalf("failed to decode Alice response: %v", err)
	}
	if resAlice.Status != "waiting" {
		t.Fatalf("expected Alice status 'waiting', got '%s'", resAlice.Status)
	}

	// 2. Bob requests match (Country: "EG") -> Should match with Alice
	bodyBob, _ := json.Marshal(MatchRequest{Country: "EG"})
	reqBob := httptest.NewRequest(http.MethodPost, "/match", bytes.NewReader(bodyBob))
	reqBob.Header.Set("Authorization", "Bearer "+tokenBob)
	recBob := httptest.NewRecorder()

	server.handleMatch(recBob, reqBob)

	if recBob.Code != http.StatusOK {
		t.Fatalf("expected status 200 for Bob, got %d", recBob.Code)
	}

	var resBob MatchResponse
	if err := json.NewDecoder(recBob.Body).Decode(&resBob); err != nil {
		t.Fatalf("failed to decode Bob response: %v", err)
	}
	if resBob.Status != "matched" {
		t.Fatalf("expected Bob status 'matched', got '%s'", resBob.Status)
	}
	if resBob.PeerID != "alice" {
		t.Fatalf("expected Bob peer_id 'alice', got '%s'", resBob.PeerID)
	}

	// 3. Verify that Alice received the match_found notification via Hub.Send
	select {
	case msgAlice := <-clientAlice.Send:
		var notif signaling.MatchNotification
		if err := json.Unmarshal(msgAlice, &notif); err != nil {
			t.Fatalf("failed to unmarshal Alice's notification: %v", err)
		}
		if notif.Type != "match_found" {
			t.Errorf("expected notification type match_found, got: %s", notif.Type)
		}
		if notif.Payload.PeerID != "bob" {
			t.Errorf("expected notification peer_id bob, got: %s", notif.Payload.PeerID)
		}
		// alice < bob -> alice is polite
		if !notif.Payload.IsPolite {
			t.Errorf("expected alice to be polite (alice < bob)")
		}
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for Alice to receive match_found notification")
	}
}

func TestAuthTokenEndpoint(t *testing.T) {
	server, _ := setupIntegrationServer(t)
	body := []byte(`{"user_id":"alice"}`)
	req := httptest.NewRequest(http.MethodPost, "/auth/token", bytes.NewReader(body))
	rec := httptest.NewRecorder()
	server.handleAuthToken(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", rec.Code, rec.Body.String())
	}
	var resp struct{ Token string `json:"token"` }
	json.NewDecoder(rec.Body).Decode(&resp)
	if resp.Token == "" {
		t.Fatal("expected non-empty token")
	}
	// Verify token is valid
	userID, err := server.parseToken(resp.Token)
	if err != nil {
		t.Fatalf("token parse failed: %v", err)
	}
	if userID != "alice" {
		t.Fatalf("expected alice, got %s", userID)
	}
}

func TestWebSocketLoadSheddingIntegration(t *testing.T) {
	server, _ := setupIntegrationServer(t)

	// Configure LoadShedder with explicit test thresholds
	cfg := loadshedding.Config{
		Enabled:                  true,
		GoroutinesEnterThreshold: 500,
		GoroutinesExitThreshold:  400,
		SystemLoadEnterThreshold: 8.0,
		SystemLoadExitThreshold:  6.0,
		SustainedTicksToEnter:    1,
		SustainedTicksToExit:     1,
		RetryAfterSeconds:        5,
	}

	ls := loadshedding.NewLoadShedder(cfg)
	var mockGoroutines int = 100
	var mockLoad float64 = 1.0
	ls.GoroutineCollector = func() int { return mockGoroutines }
	ls.SystemLoadCollector = func() (float64, error) { return mockLoad, nil }
	ls.EvaluateOnce()

	server.loadShedder = ls

	// 1. Existing connection: Register client "alice"
	clientAlice := &signaling.Client{
		ConnID: "conn-alice-123",
		UserID: "alice",
		Send:   make(chan []byte, 10),
		Done:   make(chan struct{}),
	}
	server.hub.Register(clientAlice)
	defer server.hub.Unregister(clientAlice)

	// Verify Alice is connected and healthy
	if !server.hub.SendTo("alice", []byte(`{"test":"hello"}`)) {
		t.Fatal("expected SendTo alice to succeed under normal load")
	}

	// 2. Server experiences sustained heavy load (goroutines surge to 600 >= 500)
	mockGoroutines = 600
	ls.EvaluateOnce()

	if !ls.IsOverloaded() {
		t.Fatal("expected LoadShedder to be overloaded")
	}

	// 3. Existing connection for Alice MUST remain unaffected
	select {
	case msg := <-clientAlice.Send:
		if string(msg) != `{"test":"hello"}` {
			t.Errorf("expected alice to receive hello message, got %s", string(msg))
		}
	default:
		t.Fatal("alice did not receive message")
	}

	// Alice can still receive new messages while server is overloaded
	if !server.hub.SendTo("alice", []byte(`{"test":"during_overload"}`)) {
		t.Fatal("existing client Alice must continue receiving messages during overload")
	}

	// 4. New incoming WebSocket connection attempt from Bob MUST be rejected with 503
	tokenBob := createTestToken("bob", server.jwtSecret)
	reqBob := httptest.NewRequest(http.MethodGet, "/ws", nil)
	reqBob.Header.Set("Authorization", "Bearer "+tokenBob)
	recBob := httptest.NewRecorder()

	server.handleWS(recBob, reqBob)

	if recBob.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected 503 Service Unavailable for new connection during overload, got %d", recBob.Code)
	}
	if recBob.Header().Get("Retry-After") != "5" {
		t.Errorf("expected Retry-After: 5, got %s", recBob.Header().Get("Retry-After"))
	}

	var errResp map[string]interface{}
	if err := json.NewDecoder(recBob.Body).Decode(&errResp); err != nil {
		t.Fatalf("failed to decode 503 response: %v", err)
	}
	if errResp["status"] != float64(http.StatusServiceUnavailable) {
		t.Errorf("expected status 503 in body, got %v", errResp["status"])
	}

	// 5. Verify /status/load reports overload and rejection
	recStatus := httptest.NewRecorder()
	reqStatus := httptest.NewRequest(http.MethodGet, "/status/load", nil)
	server.handleLoadStatus(recStatus, reqStatus)
	if recStatus.Code != http.StatusOK {
		t.Fatalf("expected 200 from /status/load, got %d", recStatus.Code)
	}
	var status loadshedding.StatusSnapshot
	if err := json.NewDecoder(recStatus.Body).Decode(&status); err != nil {
		t.Fatalf("failed to decode status: %v", err)
	}
	if !status.Overloaded {
		t.Errorf("expected status.Overloaded to be true")
	}
	if status.RejectedCount != 1 {
		t.Errorf("expected RejectedCount=1, got %d", status.RejectedCount)
	}

	// 6. Metrics return to normal (< exit threshold 400)
	mockGoroutines = 350
	ls.EvaluateOnce()

	if ls.IsOverloaded() {
		t.Fatal("expected LoadShedder to recover to normal state")
	}

	// 7. Verify /status/load now reports normal
	recStatus2 := httptest.NewRecorder()
	server.handleLoadStatus(recStatus2, reqStatus)
	var status2 loadshedding.StatusSnapshot
	_ = json.NewDecoder(recStatus2.Body).Decode(&status2)
	if status2.Overloaded {
		t.Errorf("expected status2.Overloaded to be false after recovery")
	}
}

func TestServerDefaultLoadSheddingConfig(t *testing.T) {
	server, _ := setupIntegrationServer(t)
	if server.loadShedder == nil {
		t.Fatal("expected loadShedder to be initialized")
	}
	status := server.loadShedder.Status()
	numCPU := float64(runtime.NumCPU())
	expectedEnter := numCPU * 2.0
	if expectedEnter < 4.0 {
		expectedEnter = 4.0
	}
	expectedExit := numCPU * 1.5
	if expectedExit < 3.0 {
		expectedExit = 3.0
	}

	if status.SystemLoadEnterLimit != expectedEnter {
		t.Errorf("expected SystemLoadEnterLimit %f, got %f", expectedEnter, status.SystemLoadEnterLimit)
	}
	if status.SystemLoadExitLimit != expectedExit {
		t.Errorf("expected SystemLoadExitLimit %f, got %f", expectedExit, status.SystemLoadExitLimit)
	}
	if status.GoroutinesEnterLimit != 10000 {
		t.Errorf("expected GoroutinesEnterLimit=10000, got %d", status.GoroutinesEnterLimit)
	}
	if status.GoroutinesExitLimit != 8000 {
		t.Errorf("expected GoroutinesExitLimit=8000, got %d", status.GoroutinesExitLimit)
	}
}

func TestServerLoadSheddingEnvOverrides(t *testing.T) {
	t.Setenv("LOAD_SHED_MAX_SYSTEM_LOAD", "25.0")
	t.Setenv("LOAD_SHED_SYSTEM_LOAD_EXIT_THRESHOLD", "18.0")
	t.Setenv("LOAD_SHED_RECOVERY_TICKS", "5")

	cfg := loadshedding.ConfigFromEnv()
	if cfg.SystemLoadEnterThreshold != 25.0 {
		t.Errorf("expected 25.0, got %f", cfg.SystemLoadEnterThreshold)
	}
	if cfg.SystemLoadExitThreshold != 18.0 {
		t.Errorf("expected 18.0, got %f", cfg.SystemLoadExitThreshold)
	}
	if cfg.SustainedTicksToExit != 5 {
		t.Errorf("expected SustainedTicksToExit=5, got %d", cfg.SustainedTicksToExit)
	}
}


