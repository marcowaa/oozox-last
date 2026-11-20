package loadshedding

import (
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
	"time"
)

func TestLoadShedder_NormalLoad(t *testing.T) {
	cfg := Config{
		Enabled:                  true,
		GoroutinesEnterThreshold: 1000,
		GoroutinesExitThreshold:  800,
		SystemLoadEnterThreshold: 10.0,
		SystemLoadExitThreshold:  8.0,
		SustainedTicksToEnter:    1,
		SustainedTicksToExit:     1,
		RetryAfterSeconds:        5,
	}

	ls := NewLoadShedder(cfg)
	// Mock normal metrics
	ls.GoroutineCollector = func() int { return 100 }
	ls.SystemLoadCollector = func() (float64, error) { return 1.5, nil }

	ls.EvaluateOnce()

	if ls.IsOverloaded() {
		t.Fatalf("expected server not to be overloaded under normal load")
	}

	// Middleware should pass through
	handlerCalled := false
	handler := ls.Middleware(func(w http.ResponseWriter, r *http.Request) {
		handlerCalled = true
		w.WriteHeader(http.StatusOK)
	})

	req := httptest.NewRequest(http.MethodGet, "/ws", nil)
	rec := httptest.NewRecorder()
	handler(rec, req)

	if !handlerCalled {
		t.Errorf("expected handler to be called when not overloaded")
	}
	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", rec.Code)
	}
}

func TestLoadShedder_GoroutinesAboveThreshold(t *testing.T) {
	cfg := Config{
		Enabled:                  true,
		GoroutinesEnterThreshold: 500,
		GoroutinesExitThreshold:  400,
		SystemLoadEnterThreshold: 10.0,
		SystemLoadExitThreshold:  8.0,
		SustainedTicksToEnter:    2,
		SustainedTicksToExit:     2,
		RetryAfterSeconds:        5,
	}

	ls := NewLoadShedder(cfg)
	var goroutineCount int
	ls.GoroutineCollector = func() int { return goroutineCount }
	ls.SystemLoadCollector = func() (float64, error) { return 1.0, nil }

	// First sample above threshold (tick 1) -> not overloaded yet due to sustained requirement (2)
	goroutineCount = 600
	ls.EvaluateOnce()
	if ls.IsOverloaded() {
		t.Errorf("should not be overloaded on first tick when sustained ticks = 2")
	}

	// Second sample above threshold (tick 2) -> should enter overload
	ls.EvaluateOnce()
	if !ls.IsOverloaded() {
		t.Fatalf("expected server to enter overload state after sustained high goroutines")
	}

	// Request should be rejected with 503
	handler := ls.Middleware(func(w http.ResponseWriter, r *http.Request) {
		t.Fatal("handler should not be called when overloaded")
	})

	req := httptest.NewRequest(http.MethodGet, "/ws", nil)
	rec := httptest.NewRecorder()
	handler(rec, req)

	if rec.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected 503 Service Unavailable, got %d", rec.Code)
	}
	if retry := rec.Header().Get("Retry-After"); retry != "5" {
		t.Errorf("expected Retry-After 5, got %s", retry)
	}

	var resp map[string]interface{}
	if err := json.NewDecoder(rec.Body).Decode(&resp); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}
	if resp["status"] != float64(http.StatusServiceUnavailable) {
		t.Errorf("expected status 503 in body, got %v", resp["status"])
	}

	status := ls.Status()
	if status.RejectedCount != 1 {
		t.Errorf("expected rejected count = 1, got %d", status.RejectedCount)
	}
}

func TestLoadShedder_SystemLoadAboveThreshold(t *testing.T) {
	cfg := Config{
		Enabled:                  true,
		GoroutinesEnterThreshold: 1000,
		GoroutinesExitThreshold:  800,
		SystemLoadEnterThreshold: 4.0,
		SystemLoadExitThreshold:  3.0,
		SustainedTicksToEnter:    1,
		SustainedTicksToExit:     1,
		RetryAfterSeconds:        10,
	}

	ls := NewLoadShedder(cfg)
	ls.GoroutineCollector = func() int { return 50 } // normal
	ls.SystemLoadCollector = func() (float64, error) { return 6.5, nil } // high load

	ls.EvaluateOnce()
	if !ls.IsOverloaded() {
		t.Fatalf("expected server to be overloaded due to high system load")
	}

	req := httptest.NewRequest(http.MethodGet, "/ws", nil)
	rec := httptest.NewRecorder()
	ls.RejectConnection(rec, req)

	if rec.Code != http.StatusServiceUnavailable {
		t.Errorf("expected 503, got %d", rec.Code)
	}
	if rec.Header().Get("Retry-After") != "10" {
		t.Errorf("expected Retry-After 10, got %s", rec.Header().Get("Retry-After"))
	}
}

func TestLoadShedder_RecoveryWhenNormalAgain(t *testing.T) {
	cfg := Config{
		Enabled:                  true,
		GoroutinesEnterThreshold: 500,
		GoroutinesExitThreshold:  400,
		SystemLoadEnterThreshold: 5.0,
		SystemLoadExitThreshold:  4.0,
		SustainedTicksToEnter:    1,
		SustainedTicksToExit:     3, // Default recovery requires 3 consecutive healthy samples
	}

	ls := NewLoadShedder(cfg)
	var gCount int
	var sLoad float64
	ls.GoroutineCollector = func() int { return gCount }
	ls.SystemLoadCollector = func() (float64, error) { return sLoad, nil }

	// Enter overload
	gCount = 600
	sLoad = 6.0
	ls.EvaluateOnce()
	if !ls.IsOverloaded() {
		t.Fatal("expected overloaded state")
	}

	// First recovery tick (metrics low) -> still overloaded because SustainedTicksToExit = 3
	gCount = 200
	sLoad = 2.0
	ls.EvaluateOnce()
	if !ls.IsOverloaded() {
		t.Fatal("should still be overloaded after only 1 recovery tick")
	}

	// Second recovery tick (metrics low) -> still overloaded because SustainedTicksToExit = 3
	ls.EvaluateOnce()
	if !ls.IsOverloaded() {
		t.Fatal("should still be overloaded after only 2 recovery ticks")
	}

	// Third recovery tick -> now should recover!
	ls.EvaluateOnce()
	if ls.IsOverloaded() {
		t.Fatal("expected recovery to normal state after 3 consecutive healthy samples")
	}
}

func TestLoadShedder_CPUAwareDefaults(t *testing.T) {
	numCPU := float64(runtime.NumCPU())
	expectedEnter := numCPU * 2.0
	if expectedEnter < 4.0 {
		expectedEnter = 4.0
	}
	expectedExit := numCPU * 1.5
	if expectedExit < 3.0 {
		expectedExit = 3.0
	}

	enter, exit := DefaultSystemLoadThresholds()
	if enter != expectedEnter {
		t.Errorf("expected enter threshold %f for %f CPUs, got %f", expectedEnter, numCPU, enter)
	}
	if exit != expectedExit {
		t.Errorf("expected exit threshold %f for %f CPUs, got %f", expectedExit, numCPU, exit)
	}
	if exit >= enter {
		t.Errorf("expected exit threshold (%f) < enter threshold (%f)", exit, enter)
	}

	// Verify ConfigFromEnv without overrides adopts CPU-aware defaults and 3 recovery ticks
	cfg := ConfigFromEnv()
	if cfg.SystemLoadEnterThreshold != expectedEnter {
		t.Errorf("expected ConfigFromEnv to use CPU-aware enter threshold %f, got %f", expectedEnter, cfg.SystemLoadEnterThreshold)
	}
	if cfg.SystemLoadExitThreshold != expectedExit {
		t.Errorf("expected ConfigFromEnv to use CPU-aware exit threshold %f, got %f", expectedExit, cfg.SystemLoadExitThreshold)
	}
	if cfg.SustainedTicksToExit != 3 {
		t.Errorf("expected default SustainedTicksToExit=3, got %d", cfg.SustainedTicksToExit)
	}
	if cfg.SustainedTicksToEnter != 2 {
		t.Errorf("expected default SustainedTicksToEnter=2, got %d", cfg.SustainedTicksToEnter)
	}
	if cfg.GoroutinesEnterThreshold != 10000 {
		t.Errorf("expected default GoroutinesEnterThreshold=10000, got %d", cfg.GoroutinesEnterThreshold)
	}
	if cfg.GoroutinesExitThreshold != 8000 {
		t.Errorf("expected default GoroutinesExitThreshold=8000, got %d", cfg.GoroutinesExitThreshold)
	}
}

func TestLoadShedder_HysteresisAndAntiFlapping(t *testing.T) {
	cfg := Config{
		Enabled:                  true,
		GoroutinesEnterThreshold: 500,
		GoroutinesExitThreshold:  400,
		SystemLoadEnterThreshold: 5.0,
		SystemLoadExitThreshold:  4.0,
		SustainedTicksToEnter:    1,
		SustainedTicksToExit:     1,
	}

	ls := NewLoadShedder(cfg)
	var gCount int
	ls.GoroutineCollector = func() int { return gCount }
	ls.SystemLoadCollector = func() (float64, error) { return 1.0, nil }

	// 1. Below enter threshold: normal
	gCount = 450
	ls.EvaluateOnce()
	if ls.IsOverloaded() {
		t.Errorf("450 is below enter threshold (500), should NOT be overloaded")
	}

	// 2. Spike to 510: enters overload
	gCount = 510
	ls.EvaluateOnce()
	if !ls.IsOverloaded() {
		t.Fatalf("510 is >= enter threshold, should be overloaded")
	}

	// 3. Drop back to 450 (which is in the deadband [400, 500]):
	// In the deadband, it MUST remain in previous state (Overloaded) to prevent flapping!
	gCount = 450
	ls.EvaluateOnce()
	if !ls.IsOverloaded() {
		t.Errorf("in deadband (450), server should stay overloaded to avoid flapping")
	}

	// 4. Drop to 401 (still in deadband): still overloaded
	gCount = 401
	ls.EvaluateOnce()
	if !ls.IsOverloaded() {
		t.Errorf("at 401, should still stay overloaded")
	}

	// 5. Drop to 390 (<= exit threshold 400): recovers to Normal
	gCount = 390
	ls.EvaluateOnce()
	if ls.IsOverloaded() {
		t.Errorf("at 390 (<= 400), server should have recovered to Normal")
	}

	// 6. Rise again to 450 (deadband): stays Normal!
	gCount = 450
	ls.EvaluateOnce()
	if ls.IsOverloaded() {
		t.Errorf("in deadband from normal state, server should remain Normal")
	}
}

func TestLoadShedder_MetricCollectionFailureFallback(t *testing.T) {
	cfg := Config{
		Enabled:                  true,
		GoroutinesEnterThreshold: 1000,
		GoroutinesExitThreshold:  800,
		SystemLoadEnterThreshold: 5.0,
		SystemLoadExitThreshold:  4.0,
		SustainedTicksToEnter:    1,
		SustainedTicksToExit:     1,
	}

	ls := NewLoadShedder(cfg)
	ls.GoroutineCollector = func() int { return 100 }
	// Simulate metric failure (e.g. read error or non-Linux OS)
	ls.SystemLoadCollector = func() (float64, error) {
		return 0, errors.New("cannot read /proc/loadavg: file not found")
	}

	ls.EvaluateOnce()

	// Safe fallback policy: unavailable metric does NOT cause false overload
	if ls.IsOverloaded() {
		t.Errorf("metric failure should fail-open and not falsely trigger overload")
	}

	status := ls.Status()
	if status.LoadMetricAvailable {
		t.Errorf("expected LoadMetricAvailable to be false")
	}
	if status.CurrentSystemLoad != -1.0 {
		t.Errorf("expected fallback load of -1.0, got %f", status.CurrentSystemLoad)
	}

	// But if goroutines exceed threshold, it should still trigger overload!
	ls.GoroutineCollector = func() int { return 1200 }
	ls.EvaluateOnce()
	if !ls.IsOverloaded() {
		t.Errorf("high goroutines should still trigger overload even if system load is unavailable")
	}
}

func TestLoadShedder_ConfigurationFromEnv(t *testing.T) {
	t.Setenv("LOAD_SHED_ENABLED", "true")
	t.Setenv("LOAD_SHED_MAX_GOROUTINES", "15000")
	t.Setenv("LOAD_SHED_GOROUTINES_EXIT_THRESHOLD", "12000")
	t.Setenv("LOAD_SHED_MAX_SYSTEM_LOAD", "16.5")
	t.Setenv("LOAD_SHED_SYSTEM_LOAD_EXIT_THRESHOLD", "11.2")
	t.Setenv("LOAD_SHED_SAMPLE_INTERVAL_MS", "500")
	t.Setenv("LOAD_SHED_SUSTAINED_TICKS", "3")
	t.Setenv("LOAD_SHED_RECOVERY_TICKS", "4")

	cfg := ConfigFromEnv()

	if !cfg.Enabled {
		t.Errorf("expected Enabled=true")
	}
	if cfg.GoroutinesEnterThreshold != 15000 {
		t.Errorf("expected 15000, got %d", cfg.GoroutinesEnterThreshold)
	}
	if cfg.GoroutinesExitThreshold != 12000 {
		t.Errorf("expected 12000, got %d", cfg.GoroutinesExitThreshold)
	}
	if cfg.SystemLoadEnterThreshold != 16.5 {
		t.Errorf("expected 16.5, got %f", cfg.SystemLoadEnterThreshold)
	}
	if cfg.SystemLoadExitThreshold != 11.2 {
		t.Errorf("expected 11.2, got %f", cfg.SystemLoadExitThreshold)
	}
	if cfg.SampleInterval != 500*time.Millisecond {
		t.Errorf("expected 500ms, got %v", cfg.SampleInterval)
	}
	if cfg.SustainedTicksToEnter != 3 {
		t.Errorf("expected 3, got %d", cfg.SustainedTicksToEnter)
	}
	if cfg.SustainedTicksToExit != 4 {
		t.Errorf("expected 4, got %d", cfg.SustainedTicksToExit)
	}
}

func TestLoadShedder_LifecycleShutdown(t *testing.T) {
	cfg := Config{
		Enabled:                  true,
		GoroutinesEnterThreshold: 500,
		GoroutinesExitThreshold:  400,
		SampleInterval:           20 * time.Millisecond,
		SustainedTicksToEnter:    1,
		SustainedTicksToExit:     1,
	}

	ls := NewLoadShedder(cfg)
	var sampleCount atomic.Int32
	ls.GoroutineCollector = func() int {
		sampleCount.Add(1)
		return 50
	}
	ls.SystemLoadCollector = func() (float64, error) {
		return 1.0, nil
	}

	ls.Start()

	// Wait for a few background sample ticks
	time.Sleep(60 * time.Millisecond)
	if sampleCount.Load() == 0 {
		t.Fatalf("expected background sampler to run")
	}

	// Clean shutdown
	ls.Stop()

	countAfterStop := sampleCount.Load()
	time.Sleep(50 * time.Millisecond)

	// Verify no more samples after stop
	if sampleCount.Load() > countAfterStop+1 {
		t.Errorf("expected background sampler to be stopped cleanly")
	}
}
