package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
	"time"

	"backend-signaling/pkg/loadshedding"
	"backend-signaling/pkg/matchmaking"
	"backend-signaling/pkg/signaling"
	"backend-signaling/pkg/turn"

	"github.com/golang-jwt/jwt/v5"
	"github.com/gorilla/websocket"
	"github.com/redis/go-redis/v9"
)

func getUpgrader() websocket.Upgrader {
	allowedOriginsEnv := os.Getenv("ALLOWED_ORIGINS")
	isProd := strings.EqualFold(strings.TrimSpace(os.Getenv("APP_ENV")), "production")
	var allowedOrigins []string
	if allowedOriginsEnv != "" {
		for _, o := range strings.Split(allowedOriginsEnv, ",") {
			trimmed := strings.TrimSpace(o)
			if trimmed != "" {
				allowedOrigins = append(allowedOrigins, trimmed)
			}
		}
	}

	return websocket.Upgrader{
		ReadBufferSize:  1024,
		WriteBufferSize: 1024,
		CheckOrigin: func(r *http.Request) bool {
			origin := r.Header.Get("Origin")
			// Allow empty Origin (e.g. native Android/iOS mobile clients)
			if origin == "" {
				return true
			}
			// If allowedOrigins configured, check against whitelist
			if len(allowedOrigins) > 0 {
				for _, allowed := range allowedOrigins {
					if allowed == origin {
						return true
					}
					if allowed == "*" && !isProd {
						return true
					}
				}
				return false
			}
			// In production without ALLOWED_ORIGINS, block unknown browser origins
			if isProd {
				return false
			}
			// Local dev fallback
			return strings.HasPrefix(origin, "http://localhost:") ||
				strings.HasPrefix(origin, "http://127.0.0.1:")
		},
	}
}

var upgrader = getUpgrader()

// Server holds shared application dependencies.
type Server struct {
	hub          *signaling.Hub
	matcher      *matchmaking.Matcher
	bridge       *signaling.Bridge
	rdb          *redis.Client
	jwtSecret    []byte
	loadShedder  *loadshedding.LoadShedder
	callReporter *signaling.CallEventReporter
}

// MatchRequest represents the JSON request payload for /match.
type MatchRequest struct {
	Country string `json:"country"`
}

// MatchResponse represents the response payload for /match.
type MatchResponse struct {
	Status string `json:"status"`            // "waiting" or "matched"
	PeerID string `json:"peer_id,omitempty"` // populated if status == "matched"
	Error  string `json:"error,omitempty"`
}

// parseToken validates a JWT and extracts the user ID from claims.
func (s *Server) parseToken(tokenStr string) (string, error) {
	if tokenStr == "" {
		return "", errors.New("empty token")
	}

	token, err := jwt.Parse(tokenStr, func(t *jwt.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", t.Header["alg"])
		}
		return s.jwtSecret, nil
	})
	if err != nil || !token.Valid {
		return "", errors.New("invalid token")
	}

	claims, ok := token.Claims.(jwt.MapClaims)
	if !ok {
		return "", errors.New("invalid claims")
	}

	// Try "sub", "user_id", then "id" (Express backend signs id + sub)
	if sub, ok := claims["sub"].(string); ok && sub != "" {
		return sub, nil
	}
	if uid, ok := claims["user_id"].(string); ok && uid != "" {
		return uid, nil
	}
	if idStr, ok := claims["id"].(string); ok && idStr != "" {
		return idStr, nil
	}

	return "", errors.New("user identifier not found in claims")
}

// authenticate extracts token from Authorization header (Bearer ...) or query string.
func (s *Server) authenticate(r *http.Request) (string, error) {
	authHeader := r.Header.Get("Authorization")
	if strings.HasPrefix(authHeader, "Bearer ") {
		return s.parseToken(strings.TrimPrefix(authHeader, "Bearer "))
	}
	// Fallback to query parameter (common in WebSocket handshakes)
	tokenQuery := r.URL.Query().Get("token")
	if tokenQuery != "" {
		return s.parseToken(tokenQuery)
	}
	return "", errors.New("missing authorization credentials")
}

// handleMatch handles POST /match.
func (s *Server) handleMatch(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	userID, err := s.authenticate(r)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusUnauthorized)
		_ = json.NewEncoder(w).Encode(MatchResponse{Error: err.Error()})
		return
	}

	var req MatchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Country == "" {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		_ = json.NewEncoder(w).Encode(MatchResponse{Error: "country is required"})
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	peerID, err := s.matcher.RequestMatch(ctx, userID, req.Country)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusConflict)
		_ = json.NewEncoder(w).Encode(MatchResponse{Error: err.Error()})
		return
	}

	w.Header().Set("Content-Type", "application/json")

	if peerID != "" {
		// Successful match: Notify both users via the Bridge
		if err := s.bridge.NotifyMatch(ctx, userID, peerID); err != nil {
			log.Printf("failed to notify match between %s and %s: %v", userID, peerID, err)
		}

		w.WriteHeader(http.StatusOK)
		_ = json.NewEncoder(w).Encode(MatchResponse{
			Status: "matched",
			PeerID: peerID,
		})
		return
	}

	// User placed in waiting state
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(MatchResponse{
		Status: "waiting",
	})
}

// TurnCredentialsRequest represents optional JSON payload for /turn-credentials.
type TurnCredentialsRequest struct {
	TTL int `json:"ttl"`
}

// handleTurnCredentials handles POST /turn-credentials.
func (s *Server) handleTurnCredentials(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	userID, err := s.authenticate(r)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusUnauthorized)
		_ = json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	urls := turn.TurnURLs()
	if len(urls) == 0 {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusServiceUnavailable)
		_ = json.NewEncoder(w).Encode(map[string]string{"error": "turn not configured"})
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	// Rate limit: 1 request every 30 seconds per userID using Redis
	if s.rdb != nil {
		rateKey := fmt.Sprintf("ratelimit:turn:%s", userID)
		ok, err := s.rdb.SetNX(ctx, rateKey, "1", 30*time.Second).Result()
		if err == nil && !ok {
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusTooManyRequests)
			_ = json.NewEncoder(w).Encode(map[string]string{"error": "rate limit exceeded: 1 request per 30 seconds"})
			return
		}
	}

	var req TurnCredentialsRequest
	_ = json.NewDecoder(r.Body).Decode(&req)

	ttl := req.TTL
	if ttl <= 0 || ttl > turn.MaxTurnTTL {
		if envTTL := os.Getenv("TURN_TTL"); envTTL != "" {
			if v, err := strconv.Atoi(envTTL); err == nil && v > 0 && v <= turn.MaxTurnTTL {
				ttl = v
			}
		}
		if ttl <= 0 || ttl > turn.MaxTurnTTL {
			ttl = turn.DefaultTurnTTL // Strictly enforce maximum 1 hour (3600 seconds) ephemeral lifetime
		}
	}

	username, credential, err := turn.GenerateCredentials(userID, ttl)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		_ = json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	resp := turn.Credentials{
		URLs:       urls,
		Username:   username,
		Credential: credential,
		TTL:        ttl,
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(resp)
}

// TokenRequest represents the JSON request payload for /auth/token.
type TokenRequest struct {
	UserID string `json:"user_id"`
}

// TokenResponse represents the JSON response for /auth/token.
type TokenResponse struct {
	Token string `json:"token"`
	Error string `json:"error,omitempty"`
}

// handleAuthToken generates a JWT for a given userID.
// WARNING: This endpoint is for DEVELOPMENT ONLY. Disable in production by setting AUTH_TOKEN_ENABLED=false.
func (s *Server) handleAuthToken(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	env := strings.ToLower(strings.TrimSpace(os.Getenv("APP_ENV")))
	if os.Getenv("AUTH_TOKEN_ENABLED") == "false" || (env == "production" && os.Getenv("AUTH_TOKEN_ENABLED") != "true") {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusForbidden)
		_ = json.NewEncoder(w).Encode(TokenResponse{Error: "auth token endpoint disabled"})
		return
	}

	var req TokenRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.UserID == "" {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		_ = json.NewEncoder(w).Encode(TokenResponse{Error: "user_id is required"})
		return
	}

	claims := jwt.MapClaims{
		"sub": req.UserID,
		"iat": time.Now().Unix(),
		"exp": time.Now().Add(24 * time.Hour).Unix(),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenStr, err := token.SignedString(s.jwtSecret)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		_ = json.NewEncoder(w).Encode(TokenResponse{Error: "failed to sign token"})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(TokenResponse{Token: tokenStr})
}

// handleWS manages real-time WebSocket signaling.
func (s *Server) handleWS(w http.ResponseWriter, r *http.Request) {
	// Load shedding protection: reject incoming connections under sustained heavy load
	if s.loadShedder != nil && s.loadShedder.IsOverloaded() {
		s.loadShedder.RejectConnection(w, r)
		return
	}

	userID, err := s.authenticate(r)
	if err != nil {
		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("websocket upgrade error: %v", err)
		return
	}

	deviceID := strings.TrimSpace(r.URL.Query().Get("deviceId"))
	if deviceID == "" {
		deviceID = strings.TrimSpace(r.Header.Get("X-Device-Id"))
	}

	client := signaling.NewClientWithDevice(userID, deviceID, conn)
	s.hub.Register(client)
	defer s.hub.Unregister(client)

	// Register cluster routing mapping with retry to prevent zombie connections
	writeCtx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	if err := s.hub.SetUserNodeMappingWithRetry(writeCtx, client, 3); err != nil {
		cancel()
		log.Printf("[Signaling] Failed to register user node mapping for %s: %v. Aborting connection.", userID, err)
		closeMsg := websocket.FormatCloseMessage(websocket.CloseInternalServerErr, "cluster mapping failed")
		_ = conn.WriteControl(websocket.CloseMessage, closeMsg, time.Now().Add(time.Second))
		return
	}
	cancel()

	go client.WritePump()

	// ReadPump configuration
	conn.SetReadLimit(64 * 1024)
	_ = conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	conn.SetPongHandler(func(string) error {
		_ = conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		s.hub.RefreshUserHeartbeat(context.Background(), client)
		return nil
	})

	// Read pump loop for signaling messages (relay offer/answer/candidate & log call lifecycle)
	for {
		_, rawMsg, err := conn.ReadMessage()
		if err != nil {
			break
		}

		s.hub.RefreshUserHeartbeat(context.Background(), client)

		if !signaling.InspectAndReportCallSignal(context.Background(), s.callReporter, s.hub, userID, rawMsg) {
			continue
		}

		var envelope struct {
			Target string `json:"target"`
		}
		if err := json.Unmarshal(rawMsg, &envelope); err == nil {
			target := strings.TrimSpace(envelope.Target)
			if target != "" && target != userID && len(target) <= 128 {
				_ = s.hub.RouteSignal(context.Background(), target, rawMsg)
			}
		}
	}
}

// handleLoadStatus returns point-in-time metrics and load-shedding status.
func (s *Server) handleLoadStatus(w http.ResponseWriter, r *http.Request) {
	if s.loadShedder == nil {
		http.Error(w, "load shedder not configured", http.StatusNotImplemented)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(s.loadShedder.Status())
}

// handleHealth checks Redis connectivity and reports signaling service readiness.
func (s *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet && r.Method != http.MethodHead {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
	defer cancel()

	redisStatus := "UP"
	statusCode := http.StatusOK
	statusLabel := "HEALTHY"
	var errMsg string

	if s.rdb == nil {
		redisStatus = "UNCONFIGURED"
		statusCode = http.StatusServiceUnavailable
		statusLabel = "DEGRADED"
	} else if err := s.rdb.Ping(ctx).Err(); err != nil {
		redisStatus = "DOWN"
		statusCode = http.StatusServiceUnavailable
		statusLabel = "DEGRADED"
		errMsg = err.Error()
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(statusCode)
	resp := map[string]interface{}{
		"status": statusLabel,
		"redis":  redisStatus,
		"overloaded": s.loadShedder != nil && s.loadShedder.IsOverloaded(),
	}
	if errMsg != "" {
		resp["error"] = errMsg
	}
	_ = json.NewEncoder(w).Encode(resp)
}

func main() {
	redisAddr := os.Getenv("REDIS_ADDR")
	if redisAddr == "" {
		redisAddr = "localhost:6379"
	}
	redisPassword := os.Getenv("REDIS_PASSWORD")
	redisDB := 0
	if rawDB := os.Getenv("REDIS_DB"); rawDB != "" {
		if parsed, err := strconv.Atoi(rawDB); err == nil && parsed >= 0 {
			redisDB = parsed
		}
	}

	secret := os.Getenv("JWT_SECRET")
	if len(secret) < 32 {
		log.Fatalf("FATAL: JWT_SECRET must be set and at least 32 chars long")
	}

	coturnSecret := os.Getenv("COTURN_SECRET")
	if coturnSecret == "" {
		log.Fatal("FATAL: COTURN_SECRET environment variable is missing. Server will not start without a secure secret.")
	}
	if len(coturnSecret) < 16 {
		log.Fatal("FATAL: COTURN_SECRET environment variable is too short (must be >= 16 chars). Server will not start without a secure secret.")
	}

	rdb := redis.NewClient(&redis.Options{
		Addr:     redisAddr,
		Password: redisPassword,
		DB:       redisDB,
	})

	hub := signaling.NewHub(rdb)
	matcher := matchmaking.New(rdb)
	bridge := signaling.NewBridge(hub, rdb)
	loadShedder := loadshedding.NewLoadShedder(loadshedding.ConfigFromEnv())

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Run Hub background Redis Stream consumer and node lifecycle manager
	go hub.StartStreamConsumer()
	go hub.StartNodeLifecycle()

	// Run Hub background Redis Pub/Sub dispatcher
	go hub.Run(ctx)

	// Start load shedding monitoring
	loadShedder.Start()

	server := &Server{
		hub:          hub,
		matcher:      matcher,
		bridge:       bridge,
		rdb:          rdb,
		jwtSecret:    []byte(secret),
		loadShedder:  loadShedder,
		callReporter: signaling.NewCallEventReporterFromEnv(),
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/health", server.handleHealth)
	mux.HandleFunc("/match", server.handleMatch)
	mux.HandleFunc("/turn-credentials", server.handleTurnCredentials)
	mux.HandleFunc("/auth/token", server.handleAuthToken)
	mux.HandleFunc("/status/load", server.handleLoadStatus)
	mux.HandleFunc("/ws", server.loadShedder.Middleware(server.handleWS))

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	httpServer := &http.Server{
		Addr:              ":" + port,
		Handler:           mux,
		ReadHeaderTimeout: 3 * time.Second,
	}

	// Graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	go func() {
		<-quit
		log.Println("Shutting down server gracefully...")
		shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer shutdownCancel()
		if err := httpServer.Shutdown(shutdownCtx); err != nil {
			log.Printf("server forced to shutdown: %v", err)
		}
		hub.Shutdown(shutdownCtx)
		loadShedder.Stop()
	}()

	log.Printf("Signaling & Matchmaking server listening on port %s...", port)
	if err := httpServer.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("server error: %v", err)
	}
}
