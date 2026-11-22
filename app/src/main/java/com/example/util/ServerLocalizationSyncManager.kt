package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Coordinates server-backed i18n bundle synchronization with local SharedPreferences caching & ETag support.
 * Ensures immediate offline availability via bundled JSON assets + persisted server updates,
 * and pushes signed-in users' preferredLanguage to PATCH /api/auth/language.
 */
object ServerLocalizationSyncManager {

    private const val TAG = "OozoxI18nSync"
    private const val PREFS_NAME = "oozox_i18n_cache"
    private const val KEY_BUNDLE_PREFIX = "bundle_"
    private const val KEY_ETAG_PREFIX = "etag_"
    private const val KEY_VERSION_PREFIX = "version_"
    private const val KEY_LAST_SYNC_PREFIX = "synced_at_"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedVersion = MutableStateFlow<String?>(null)
    val lastSyncedVersion: StateFlow<String?> = _lastSyncedVersion.asStateFlow()

    fun loadCachedBundle(context: Context, langCode: String): Map<String, String> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val rawJson = prefs.getString("$KEY_BUNDLE_PREFIX$langCode", null) ?: return emptyMap()
            parseJsonStringMap(rawJson)
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading cached server i18n bundle for $langCode: ${e.message}")
            emptyMap()
        }
    }

    fun getCachedEtag(context: Context, langCode: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_ETAG_PREFIX$langCode", null)
    }

    fun getCachedVersion(context: Context, langCode: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_VERSION_PREFIX$langCode", null)
    }

    fun saveBundleToCache(
        context: Context,
        langCode: String,
        strings: Map<String, String>,
        etag: String?,
        version: String?
    ) {
        val json = JSONObject()
        strings.forEach { (k, v) -> json.put(k, v) }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("$KEY_BUNDLE_PREFIX$langCode", json.toString())
            if (!etag.isNullOrBlank()) putString("$KEY_ETAG_PREFIX$langCode", etag)
            if (!version.isNullOrBlank()) putString("$KEY_VERSION_PREFIX$langCode", version)
            putLong("$KEY_LAST_SYNC_PREFIX$langCode", System.currentTimeMillis())
        }.apply()
    }

    fun syncLanguageAsync(
        context: Context,
        language: AppLanguage,
        pushPreferenceToServer: Boolean = false
    ) {
        val appContext = context.applicationContext
        val langCode = language.code
        RetrofitClient.setActiveLanguage(langCode)

        // Hydrate immediately from any previously synced server bundle
        val cached = loadCachedBundle(appContext, langCode)
        if (cached.isNotEmpty()) {
            Translations.applyServerBundle(language, cached, getCachedVersion(appContext, langCode))
        }

        scope.launch {
            syncLanguageSuspend(appContext, language, pushPreferenceToServer)
        }
    }

    suspend fun syncLanguageSuspend(
        context: Context,
        language: AppLanguage,
        pushPreferenceToServer: Boolean = false,
        apiService: OozoxApiService = RetrofitClient.apiService()
    ): Boolean {
        val appContext = context.applicationContext
        val langCode = language.code
        RetrofitClient.setActiveLanguage(langCode)
        _isSyncing.value = true
        return try {
            val cachedEtag = getCachedEtag(appContext, langCode)
            val response = apiService.getLocalizationBundle(langCode, cachedEtag)
            when {
                response.code() == 304 -> {
                    val cached = loadCachedBundle(appContext, langCode)
                    if (cached.isNotEmpty()) {
                        Translations.applyServerBundle(language, cached, getCachedVersion(appContext, langCode))
                    }
                    true
                }
                response.isSuccessful -> {
                    val payload = response.body()?.data
                    val serverStrings = payload?.strings.orEmpty()
                    if (serverStrings.isNotEmpty()) {
                        val etag = response.headers()["ETag"]
                        val version = payload?.version
                        saveBundleToCache(appContext, langCode, serverStrings, etag, version)
                        Translations.applyServerBundle(language, serverStrings, version)
                        _lastSyncedVersion.value = version
                        true
                    } else {
                        false
                    }
                }
                else -> {
                    Log.w(TAG, "i18n bundle fetch returned HTTP ${response.code()} for $langCode")
                    false
                }
            }.also {
                if (pushPreferenceToServer && RetrofitClient.hasActiveSession()) {
                    try {
                        apiService.updatePreferredLanguage(ApiUpdateLanguageRequest(langCode))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed syncing preferredLanguage=$langCode to profile: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Offline or server unreachable while syncing i18n ($langCode): ${e.message}")
            false
        } finally {
            _isSyncing.value = false
        }
    }

    internal fun parseJsonStringMap(rawJson: String): Map<String, String> {
        val jsonObject = JSONObject(rawJson)
        val result = LinkedHashMap<String, String>(jsonObject.length())
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.optString(key, "")
            if (value.isNotEmpty()) {
                result[key] = value
            }
        }
        return result
    }
}
