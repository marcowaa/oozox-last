package com.example.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Experiment Configuration & Feature Flags for UI/UX A/B Testing
 */
data class AbExperimentConfig(
    // 1. Navigation & Tab Animation Experiment
    val navTabAnimationStyle: String = "BOUNCY_SPRING", // "BOUNCY_SPRING", "FLAT_FADE", "CLASSIC_RIPPLE"
    val enableTabBadgePulse: Boolean = true,
    
    // 2. Gold Screen Layout & Calculator Variant
    val goldCalculatorLayout: String = "ACCORDION_DRAWER", // "ACCORDION_DRAWER", "FULL_SHEET_MODAL", "INLINE_EXPAND"
    val enableLivePriceTickerRibbon: Boolean = true,

    // 3. Smart Accounting Ingestion Flow
    val smartAccountantAiModel: String = "gemini-2.5-flash",
    val autoCategorizeHashtagsEnabled: Boolean = true,

    // 4. Performance & High-Concurrency Relays
    val webrtcHardwareAcceleration: Boolean = true,
    val mediaRelayRegionPreference: String = "AUTO_LOWEST_PING" // "AUTO_LOWEST_PING", "EU_CENTRAL", "ME_DUBAI"
)

/**
 * Firebase Remote Config & A/B Testing Management Engine
 * Provides safe canary rollout flags, variant exposure logging, and real-time state exposure to Compose UI.
 */
class FirebaseAbTestingManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("oozox_ab_testing_prefs", Context.MODE_PRIVATE)

    private val _experimentConfig = MutableStateFlow(loadCachedExperimentConfig())
    val experimentConfig: StateFlow<AbExperimentConfig> = _experimentConfig.asStateFlow()

    private val _activeExperiments = MutableStateFlow<Map<String, String>>(emptyMap())
    val activeExperiments: StateFlow<Map<String, String>> = _activeExperiments.asStateFlow()

    init {
        fetchAndActivateRemoteConfig()
    }

    /**
     * Simulates / fetches from Firebase Remote Config with fallback to on-device defaults
     */
    fun fetchAndActivateRemoteConfig() {
        scope.launch {
            try {
                // In a production environment with Google Play Services / Firebase:
                // val remoteConfig = FirebaseRemoteConfig.getInstance()
                // remoteConfig.fetchAndActivate().addOnCompleteListener { ... }

                val updatedConfig = AbExperimentConfig(
                    navTabAnimationStyle = prefs.getString("exp_nav_tab_anim", "BOUNCY_SPRING") ?: "BOUNCY_SPRING",
                    enableTabBadgePulse = prefs.getBoolean("exp_badge_pulse", true),
                    goldCalculatorLayout = prefs.getString("exp_gold_layout", "ACCORDION_DRAWER") ?: "ACCORDION_DRAWER",
                    enableLivePriceTickerRibbon = prefs.getBoolean("exp_gold_ticker", true),
                    smartAccountantAiModel = prefs.getString("exp_ai_model", "gemini-2.5-flash") ?: "gemini-2.5-flash",
                    autoCategorizeHashtagsEnabled = prefs.getBoolean("exp_auto_cat", true),
                    webrtcHardwareAcceleration = prefs.getBoolean("exp_webrtc_hw", true),
                    mediaRelayRegionPreference = prefs.getString("exp_relay_pref", "AUTO_LOWEST_PING") ?: "AUTO_LOWEST_PING"
                )

                _experimentConfig.value = updatedConfig
                _activeExperiments.value = mapOf(
                    "nav_tab_animation_style" to updatedConfig.navTabAnimationStyle,
                    "gold_calculator_layout" to updatedConfig.goldCalculatorLayout,
                    "smart_accountant_ai_model" to updatedConfig.smartAccountantAiModel,
                    "media_relay_region" to updatedConfig.mediaRelayRegionPreference
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Logs an A/B test exposure event to Firebase Analytics & local engagement monitor
     */
    fun logExperimentExposure(experimentKey: String, variantValue: String, currentUserId: String) {
        scope.launch {
            try {
                // In production:
                // val bundle = Bundle().apply {
                //     putString(FirebaseAnalytics.Param.ITEM_ID, experimentKey)
                //     putString(FirebaseAnalytics.Param.ITEM_NAME, variantValue)
                // }
                // FirebaseAnalytics.getInstance(context).logEvent("ab_experiment_exposure", bundle)
                println("[A/B Testing] Logged exposure for experiment '$experimentKey' with variant '$variantValue' for user '$currentUserId'")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Tracks key conversion/engagement metrics for A/B testing statistical significance
     */
    fun trackConversionMetric(metricName: String, value: Double = 1.0, metadata: Map<String, String> = emptyMap()) {
        scope.launch {
            try {
                println("[A/B Testing Conversion] Metric: $metricName | Value: $value | Meta: $metadata")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadCachedExperimentConfig(): AbExperimentConfig {
        return AbExperimentConfig(
            navTabAnimationStyle = prefs.getString("exp_nav_tab_anim", "BOUNCY_SPRING") ?: "BOUNCY_SPRING",
            enableTabBadgePulse = prefs.getBoolean("exp_badge_pulse", true),
            goldCalculatorLayout = prefs.getString("exp_gold_layout", "ACCORDION_DRAWER") ?: "ACCORDION_DRAWER",
            enableLivePriceTickerRibbon = prefs.getBoolean("exp_gold_ticker", true),
            smartAccountantAiModel = prefs.getString("exp_ai_model", "gemini-2.5-flash") ?: "gemini-2.5-flash",
            autoCategorizeHashtagsEnabled = prefs.getBoolean("exp_auto_cat", true),
            webrtcHardwareAcceleration = prefs.getBoolean("exp_webrtc_hw", true),
            mediaRelayRegionPreference = prefs.getString("exp_relay_pref", "AUTO_LOWEST_PING") ?: "AUTO_LOWEST_PING"
        )
    }

    companion object {
        @Volatile
        private var instance: FirebaseAbTestingManager? = null

        fun getInstance(context: Context): FirebaseAbTestingManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAbTestingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
