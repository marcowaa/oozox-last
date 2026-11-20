package loadshedding

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

// Default configuration constants.
const (
	DefaultGoroutinesEnterThreshold = 10000
	DefaultGoroutinesExitThreshold  = 8000
	DefaultSampleInterval           = 1 * time.Second
	DefaultSustainedTicksToEnter    = 2
	DefaultSustainedTicksToExit     = 3
	DefaultRetryAfterSeconds        = 5
)

// DefaultSystemLoadThresholds calculates CPU-aware default system load thresholds.
// Enter threshold: max(NumCPU * 2.0, 4.0)
// Exit threshold:  max(NumCPU * 1.5, 3.0)
func DefaultSystemLoadThresholds() (enter float64, exit float64) {
	numCPU := float64(runtime.NumCPU())
	if numCPU < 1.0 {
		numCPU = 1.0
	}
	enter = numCPU * 2.0
	if enter < 4.0 {
		enter = 4.0
	}
	exit = numCPU * 1.5
	if exit < 3.0 {
		exit = 3.0
	}
	return enter, exit
}

// Config defines the operational parameters and thresholds for load shedding.
type Config struct {
	// Enabled determines if the load shedding protection is active.
	Enabled bool

	// GoroutinesEnterThreshold is the active goroutine count at or above which overload is triggered.
	GoroutinesEnterThreshold int

	// GoroutinesExitThreshold is the active goroutine count at or below which recovery is permitted.
	// Must be lower than GoroutinesEnterThreshold to prevent flapping.
	GoroutinesExitThreshold int

	// SystemLoadEnterThreshold is the 1-minute system load average at or above which overload is triggered.
	SystemLoadEnterThreshold float64

	// SystemLoadExitThreshold is the 1-minute system load average at or below which recovery is permitted.
	// Must be lower than SystemLoadEnterThreshold to prevent flapping.
	SystemLoadExitThreshold float64

	// SampleInterval is the frequency at which system metrics are measured.
	SampleInterval time.Duration

	// SustainedTicksToEnter is the number of consecutive over-threshold samples required to enter overload.
	SustainedTicksToEnter int

	// SustainedTicksToExit is the number of consecutive under-threshold samples required to exit overload.
	SustainedTicksToExit int

	// RetryAfterSeconds is the value populated in the HTTP Retry-After header on rejection.
	RetryAfterSeconds int
}

// ConfigFromEnv reads load shedding configuration from environment variables with safe defaults.
func ConfigFromEnv() Config {
	defaultSysEnter, defaultSysExit := DefaultSystemLoadThresholds()

	cfg := Config{
		Enabled:                  true,
		GoroutinesEnterThreshold: DefaultGoroutinesEnterThreshold,
		GoroutinesExitThreshold:  DefaultGoroutinesExitThreshold,
		SystemLoadEnterThreshold: defaultSysEnter,
		SystemLoadExitThreshold:  defaultSysExit,
		SampleInterval:           DefaultSampleInterval,
		SustainedTicksToEnter:    DefaultSustainedTicksToEnter,
		SustainedTicksToExit:     DefaultSustainedTicksToExit,
		RetryAfterSeconds:        DefaultRetryAfterSeconds,
	}

	if val := os.Getenv("LOAD_SHED_ENABLED"); val != "" {
		if enabled, err := strconv.ParseBool(val); err == nil {
			cfg.Enabled = enabled
		}
	}

	// Max goroutines / Enter threshold
	if val := os.Getenv("LOAD_SHED_MAX_GOROUTINES"); val != "" {
		if v, err := strconv.Atoi(val); err == nil && v > 0 {
			cfg.GoroutinesEnterThreshold = v
		}
	}
	if val := os.Getenv("LOAD_SHED_GOROUTINES_ENTER_THRESHOLD"); val != "" {
		if v, err := strconv.Atoi(val); err == nil && v > 0 {
			cfg.GoroutinesEnterThreshold = v
		}
	}

	// Goroutines exit / recovery threshold
	if val := os.Getenv("LOAD_SHED_GOROUTINES_EXIT_THRESHOLD"); val != "" {
		if v, err := strconv.Atoi(val); err == nil && v > 0 {
			cfg.GoroutinesExitThreshold = v
		}
	}

	// Max system load / Enter threshold
	if val := os.Getenv("LOAD_SHED_MAX_SYSTEM_LOAD"); val != "" {
		if v, err := strconv.ParseFloat(val, 64); err == nil && v > 0 {
			cfg.SystemLoadEnterThreshold = v
		}
	}
	if val := os.Getenv("LOAD_SHED_SYSTEM_LOAD_ENTER_THRESHOLD"); val != "" {
		if v, err := strconv.ParseFloat(val, 64); err == nil && v > 0 {
			cfg.SystemLoadEnterThreshold = v
		}
	}

	// System load exit / recovery threshold
	if val := os.Getenv("LOAD_SHED_SYSTEM_LOAD_EXIT_THRESHOLD"); val != "" {
		if v, err := strconv.ParseFloat(val, 64); err == nil && v > 0 {
			cfg.SystemLoadExitThreshold = v
		}
	}

	// Sample interval in milliseconds
	if val := os.Getenv("LOAD_SHED_SAMPLE_INTERVAL_MS"); val != "" {
		if v, err := strconv.Atoi(val); err == nil && v > 0 {
			cfg.SampleInterval = time.Duration(v) * time.Millisecond
		}
	}

	// Sustained ticks
	if val := os.Getenv("LOAD_SHED_SUSTAINED_TICKS"); val != "" {
		if v, err := strconv.Atoi(val); err == nil && v > 0 {
			cfg.SustainedTicksToEnter = v
		}
	}
	if val := os.Getenv("LOAD_SHED_RECOVERY_TICKS"); val != "" {
		if v, err := strconv.Atoi(val); err == nil && v > 0 {
			cfg.SustainedTicksToExit = v
		}
	}

	cfg.validateAndSanitize()
	return cfg
}

// validateAndSanitize guarantees threshold consistency and prevents flapping anomalies.
func (c *Config) validateAndSanitize() {
	if c.GoroutinesEnterThreshold <= 0 {
		c.GoroutinesEnterThreshold = DefaultGoroutinesEnterThreshold
	}
	// Enforce exit threshold < enter threshold for hysteresis
	if c.GoroutinesExitThreshold <= 0 || c.GoroutinesExitThreshold >= c.GoroutinesEnterThreshold {
		c.GoroutinesExitThreshold = int(float64(c.GoroutinesEnterThreshold) * 0.8)
	}

	if c.SystemLoadEnterThreshold <= 0 {
		defaultEnter, _ := DefaultSystemLoadThresholds()
		c.SystemLoadEnterThreshold = defaultEnter
	}
	// Enforce exit threshold < enter threshold for hysteresis
	if c.SystemLoadExitThreshold <= 0 || c.SystemLoadExitThreshold >= c.SystemLoadEnterThreshold {
		c.SystemLoadExitThreshold = c.SystemLoadEnterThreshold * 0.75
	}

	if c.SampleInterval <= 0 {
		c.SampleInterval = DefaultSampleInterval
	}
	if c.SustainedTicksToEnter <= 0 {
		c.SustainedTicksToEnter = DefaultSustainedTicksToEnter
	}
	if c.SustainedTicksToExit <= 0 {
		c.SustainedTicksToExit = DefaultSustainedTicksToExit
	}
	if c.RetryAfterSeconds <= 0 {
		c.RetryAfterSeconds = DefaultRetryAfterSeconds
	}
}

// StatusSnapshot contains point-in-time metrics and state for observability.
type StatusSnapshot struct {
	Overloaded           bool      `json:"overloaded"`
	CurrentGoroutines    int       `json:"current_goroutines"`
	CurrentSystemLoad    float64   `json:"current_system_load"`
	GoroutinesEnterLimit int       `json:"goroutines_enter_limit"`
	GoroutinesExitLimit  int       `json:"goroutines_exit_limit"`
	SystemLoadEnterLimit float64   `json:"system_load_enter_limit"`
	SystemLoadExitLimit  float64   `json:"system_load_exit_limit"`
	RejectedCount        uint64    `json:"rejected_count"`
	LastStateChange      time.Time `json:"last_state_change"`
	LoadMetricAvailable  bool      `json:"load_metric_available"`
}

// LoadShedder protects server stability by shedding new incoming connections during sustained high load.
type LoadShedder struct {
	config Config

	overloaded atomic.Bool

	// Atomic telemetry
	currentGoroutines   atomic.Int64
	currentSystemLoadMu sync.RWMutex
	currentSystemLoad   float64
	loadAvailable       atomic.Bool
	rejectedCount       atomic.Uint64

	// State machine counters for flapping prevention
	enterStreak int
	exitStreak  int
	lastChange  time.Time
	stateMu     sync.Mutex

	// Collectors (pluggable for unit testing)
	GoroutineCollector func() int
	SystemLoadCollector func() (float64, error)

	ctx    context.Context
	cancel context.CancelFunc
	wg     sync.WaitGroup
}

// NewLoadShedder creates a new LoadShedder instance with default collectors.
func NewLoadShedder(cfg Config) *LoadShedder {
	cfg.validateAndSanitize()
	ctx, cancel := context.WithCancel(context.Background())

	ls := &LoadShedder{
		config:              cfg,
		lastChange:          time.Now(),
		GoroutineCollector:  runtime.NumGoroutine,
		SystemLoadCollector: ReadLinuxLoadAverage,
		ctx:                 ctx,
		cancel:              cancel,
	}
	ls.loadAvailable.Store(true)
	return ls
}

// ReadLinuxLoadAverage reads the 1-minute load average from /proc/loadavg on Linux.
func ReadLinuxLoadAverage() (float64, error) {
	data, err := os.ReadFile("/proc/loadavg")
	if err != nil {
		return 0, err
	}
	fields := strings.Fields(string(data))
	if len(fields) < 1 {
		return 0, fmt.Errorf("malformed /proc/loadavg content")
	}
	load1, err := strconv.ParseFloat(fields[0], 64)
	if err != nil {
		return 0, fmt.Errorf("failed to parse loadavg: %w", err)
	}
	return load1, nil
}

// Start launches the lightweight background metrics sampler.
func (ls *LoadShedder) Start() {
	if !ls.config.Enabled {
		log.Println("[LoadShedder] Disabled via configuration.")
		return
	}

	ls.wg.Add(1)
	go ls.sampleLoop()
	log.Printf("[LoadShedder] Started. Goroutine Thresholds: Enter=%d / Exit=%d; System Load Thresholds: Enter=%.1f / Exit=%.1f; Interval=%v",
		ls.config.GoroutinesEnterThreshold,
		ls.config.GoroutinesExitThreshold,
		ls.config.SystemLoadEnterThreshold,
		ls.config.SystemLoadExitThreshold,
		ls.config.SampleInterval,
	)
}

// Stop cleanly terminates the metrics sampler and waits for its exit.
func (ls *LoadShedder) Stop() {
	ls.cancel()
	ls.wg.Wait()
}

// sampleLoop periodically samples metrics and evaluates the hysteresis state machine.
func (ls *LoadShedder) sampleLoop() {
	defer ls.wg.Done()

	ticker := time.NewTicker(ls.config.SampleInterval)
	defer ticker.Stop()

	// Initial evaluation
	ls.EvaluateOnce()

	for {
		select {
		case <-ls.ctx.Done():
			return
		case <-ticker.C:
			ls.EvaluateOnce()
		}
	}
}

// EvaluateOnce samples the metrics and updates the overload state. Exposed for direct testing.
func (ls *LoadShedder) EvaluateOnce() {
	// 1. Collect Goroutines
	goroutines := 0
	if ls.GoroutineCollector != nil {
		goroutines = ls.GoroutineCollector()
	} else {
		goroutines = runtime.NumGoroutine()
	}
	ls.currentGoroutines.Store(int64(goroutines))

	// 2. Collect System Load (with fallback policy)
	var sysLoad float64
	var loadErr error
	if ls.SystemLoadCollector != nil {
		sysLoad, loadErr = ls.SystemLoadCollector()
	} else {
		sysLoad, loadErr = ReadLinuxLoadAverage()
	}

	loadMetricOk := true
	if loadErr != nil {
		// Documented Fallback Policy:
		// If metric collection fails or is unavailable (e.g. non-Linux environment),
		// we fail-open for this specific metric and do not falsely flag overload.
		loadMetricOk = false
		sysLoad = -1.0
	}
	ls.loadAvailable.Store(loadMetricOk)

	ls.currentSystemLoadMu.Lock()
	ls.currentSystemLoad = sysLoad
	ls.currentSystemLoadMu.Unlock()

	// 3. Evaluate criteria against thresholds
	isHighGoroutines := goroutines >= ls.config.GoroutinesEnterThreshold
	isHighSystemLoad := loadMetricOk && (sysLoad >= ls.config.SystemLoadEnterThreshold)
	isSampleOverloaded := isHighGoroutines || isHighSystemLoad

	isLowGoroutines := goroutines <= ls.config.GoroutinesExitThreshold
	isLowSystemLoad := !loadMetricOk || (sysLoad <= ls.config.SystemLoadExitThreshold)
	isSampleRecovered := isLowGoroutines && isLowSystemLoad

	// 4. Update state machine with hysteresis and sustained sample windows
	ls.stateMu.Lock()
	defer ls.stateMu.Unlock()

	wasOverloaded := ls.overloaded.Load()

	if !wasOverloaded {
		// Currently Normal: Need SustainedTicksToEnter consecutive high samples to enter Overload
		if isSampleOverloaded {
			ls.enterStreak++
			if ls.enterStreak >= ls.config.SustainedTicksToEnter {
				ls.overloaded.Store(true)
				ls.lastChange = time.Now()
				ls.enterStreak = 0
				ls.exitStreak = 0

				log.Printf("[LoadShedder] *** OVERLOAD DETECTED *** state=OVERLOADED goroutines=%d (threshold=%d) system_load=%.2f (threshold=%.2f, ok=%v) total_rejected=%d",
					goroutines, ls.config.GoroutinesEnterThreshold, sysLoad, ls.config.SystemLoadEnterThreshold, loadMetricOk, ls.rejectedCount.Load())
			}
		} else {
			ls.enterStreak = 0
		}
	} else {
		// Currently Overloaded: Need SustainedTicksToExit consecutive recovered samples to return to Normal
		if isSampleRecovered {
			ls.exitStreak++
			if ls.exitStreak >= ls.config.SustainedTicksToExit {
				ls.overloaded.Store(false)
				ls.lastChange = time.Now()
				ls.enterStreak = 0
				ls.exitStreak = 0

				log.Printf("[LoadShedder] *** SYSTEM RECOVERED *** state=NORMAL goroutines=%d (recovery_threshold=%d) system_load=%.2f (recovery_threshold=%.2f) total_rejected=%d",
					goroutines, ls.config.GoroutinesExitThreshold, sysLoad, ls.config.SystemLoadExitThreshold, ls.rejectedCount.Load())
			}
		} else {
			ls.exitStreak = 0
		}
	}
}

// IsOverloaded returns true if the server is currently under sustained overload.
func (ls *LoadShedder) IsOverloaded() bool {
	if ls == nil || !ls.config.Enabled {
		return false
	}
	return ls.overloaded.Load()
}

// RecordRejection increments the total rejected connections counter and logs telemetry.
func (ls *LoadShedder) RecordRejection() uint64 {
	total := ls.rejectedCount.Add(1)

	// Log on first rejections and periodic intervals to prevent log flood
	if total == 1 || total%25 == 0 {
		goroutines := ls.currentGoroutines.Load()
		ls.currentSystemLoadMu.RLock()
		sysLoad := ls.currentSystemLoad
		ls.currentSystemLoadMu.RUnlock()

		log.Printf("[LoadShedder] Rejection Event: total_rejected=%d goroutines=%d system_load=%.2f enter_thresholds(goroutines=%d, load=%.2f)",
			total, goroutines, sysLoad, ls.config.GoroutinesEnterThreshold, ls.config.SystemLoadEnterThreshold)
	}
	return total
}

// RejectConnection writes an HTTP 503 Service Unavailable response before WebSocket upgrade.
func (ls *LoadShedder) RejectConnection(w http.ResponseWriter, r *http.Request) {
	ls.RecordRejection()

	retryAfter := strconv.Itoa(ls.config.RetryAfterSeconds)
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Retry-After", retryAfter)
	w.WriteHeader(http.StatusServiceUnavailable)

	_ = json.NewEncoder(w).Encode(map[string]interface{}{
		"error":               "server is temporarily overloaded, please retry later",
		"status":              http.StatusServiceUnavailable,
		"retry_after_seconds": ls.config.RetryAfterSeconds,
	})
}

// Middleware returns an HTTP middleware wrapping a handler with load-shedding protection.
func (ls *LoadShedder) Middleware(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if ls != nil && ls.IsOverloaded() {
			ls.RejectConnection(w, r)
			return
		}
		next(w, r)
	}
}

// Status returns a point-in-time snapshot of the load shedder metrics and state.
func (ls *LoadShedder) Status() StatusSnapshot {
	ls.currentSystemLoadMu.RLock()
	sysLoad := ls.currentSystemLoad
	ls.currentSystemLoadMu.RUnlock()

	ls.stateMu.Lock()
	lastChange := ls.lastChange
	ls.stateMu.Unlock()

	return StatusSnapshot{
		Overloaded:           ls.IsOverloaded(),
		CurrentGoroutines:    int(ls.currentGoroutines.Load()),
		CurrentSystemLoad:    sysLoad,
		GoroutinesEnterLimit: ls.config.GoroutinesEnterThreshold,
		GoroutinesExitLimit:  ls.config.GoroutinesExitThreshold,
		SystemLoadEnterLimit: ls.config.SystemLoadEnterThreshold,
		SystemLoadExitLimit:  ls.config.SystemLoadExitThreshold,
		RejectedCount:        ls.rejectedCount.Load(),
		LastStateChange:      lastChange,
		LoadMetricAvailable:  ls.loadAvailable.Load(),
	}
}
