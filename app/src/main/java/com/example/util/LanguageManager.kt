package com.example.util

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class AppLanguage(val code: String, val displayName: String, val flag: String, val direction: LayoutDirection) {
    ARABIC("ar", "العربية", "AR", LayoutDirection.Rtl),
    ENGLISH("en", "English", "EN", LayoutDirection.Ltr),
    FRENCH("fr", "Français", "FR", LayoutDirection.Ltr),
    SPANISH("es", "Español", "ES", LayoutDirection.Ltr),
    TURKISH("tr", "Türkçe", "TR", LayoutDirection.Ltr),
    GERMAN("de", "Deutsch", "DE", LayoutDirection.Ltr),
    CHINESE("zh", "中文", "ZH", LayoutDirection.Ltr),
    HINDI("hi", "हिन्दी", "HI", LayoutDirection.Ltr),
    RUSSIAN("ru", "Русский", "RU", LayoutDirection.Ltr),
    JAPANESE("ja", "日本語", "JA", LayoutDirection.Ltr),
    PORTUGUESE("pt", "Português", "PT", LayoutDirection.Ltr),
    ITALIAN("it", "Italiano", "IT", LayoutDirection.Ltr),
    URDU("ur", "اردو", "UR", LayoutDirection.Rtl),
    PERSIAN("fa", "فارسی", "FA", LayoutDirection.Rtl);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            val normalized = code?.trim()?.lowercase()?.split('-', '_')?.firstOrNull()
            return entries.find { it.code == normalized } ?: ARABIC
        }
    }
}

object Translations {

    @Volatile
    private var appContext: Context? = null

    @Volatile
    var currentAppLanguage: AppLanguage = AppLanguage.ARABIC
        private set

    private val assetBundles = ConcurrentHashMap<String, Map<String, String>>()
    private val serverOverrides = ConcurrentHashMap<String, Map<String, String>>()
    private val serverVersions = ConcurrentHashMap<String, String>()

    var catalogVersion by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        val ctx = context.applicationContext ?: context
        appContext = ctx
        val lang = activeLanguage(ctx)
        currentAppLanguage = lang
        ensureLoaded(ctx, lang)
        RetrofitClient.setActiveLanguage(lang.code)
        val cachedServer = ServerLocalizationSyncManager.loadCachedBundle(ctx, lang.code)
        if (cachedServer.isNotEmpty()) {
            applyServerBundle(lang, cachedServer, ServerLocalizationSyncManager.getCachedVersion(ctx, lang.code))
        }
    }

    fun ensureLoaded(context: Context?, language: AppLanguage) {
        if (context != null && appContext == null) {
            appContext = context.applicationContext ?: context
        }
        if (!assetBundles.containsKey(language.code)) {
            loadAssetBundle(language)
        }
    }

    fun applyServerBundle(language: AppLanguage, bundle: Map<String, String>, version: String? = null) {
        if (bundle.isEmpty()) return
        serverOverrides[language.code] = HashMap(bundle)
        if (!version.isNullOrBlank()) {
            serverVersions[language.code] = version
        }
        catalogVersion++
    }

    fun getServerVersion(language: AppLanguage): String? = serverVersions[language.code]

    fun clearServerOverridesForTesting() {
        serverOverrides.clear()
        serverVersions.clear()
        catalogVersion++
    }

    private fun loadAssetBundle(language: AppLanguage): Map<String, String> {
        assetBundles[language.code]?.let { return it }
        val fileName = "i18n/${language.code}.json"
        var raw: String? = null

        val ctx = appContext
        if (ctx != null) {
            raw = try {
                ctx.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: Exception) {
                null
            }
        }

        if (raw == null) {
            raw = try {
                Translations::class.java.classLoader
                    ?.getResourceAsStream(fileName)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
            } catch (_: Exception) {
                null
            }
        }

        if (raw == null) {
            // Pure JVM fallback if executed from workspace or app module without Android assets mounted
            val candidates = listOf(
                File("app/src/main/assets/$fileName"),
                File("src/main/assets/$fileName")
            )
            raw = candidates.firstOrNull { it.isFile }?.readText(Charsets.UTF_8)
        }

        val parsed = if (raw != null) parseJsonMap(raw) else emptyMap()
        if (parsed.isNotEmpty()) {
            assetBundles[language.code] = parsed
        }
        return parsed
    }

    private fun parseJsonMap(rawJson: String): Map<String, String> {
        return try {
            val json = JSONObject(rawJson)
            val map = HashMap<String, String>(json.length())
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optString(key, key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun get(key: String, language: AppLanguage): String {
        serverOverrides[language.code]?.get(key)?.let { if (it.isNotEmpty()) return it }
        loadAssetBundle(language)[key]?.let { if (it.isNotEmpty()) return it }

        if (language != AppLanguage.ENGLISH) {
            serverOverrides[AppLanguage.ENGLISH.code]?.get(key)?.let { if (it.isNotEmpty()) return it }
            loadAssetBundle(AppLanguage.ENGLISH)[key]?.let { if (it.isNotEmpty()) return it }
        }
        if (language != AppLanguage.ARABIC) {
            serverOverrides[AppLanguage.ARABIC.code]?.get(key)?.let { if (it.isNotEmpty()) return it }
            loadAssetBundle(AppLanguage.ARABIC)[key]?.let { if (it.isNotEmpty()) return it }
        }
        return key
    }

    fun format(key: String, language: AppLanguage, vararg args: Pair<String, Any?>): String {
        var result = get(key, language)
        for ((name, value) in args) {
            result = result.replace("{$name}", value?.toString() ?: "")
        }
        return result
    }

    fun activeLanguage(context: Context?): AppLanguage {
        val ctx = context ?: appContext ?: return currentAppLanguage
        val prefs = ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_language", "ar") ?: "ar"
        val resolved = AppLanguage.fromCode(langCode)
        currentAppLanguage = resolved
        return resolved
    }

    fun getFromContext(context: Context?, key: String): String {
        val lang = activeLanguage(context)
        ensureLoaded(context, lang)
        return get(key, lang)
    }

    fun formatFromContext(context: Context?, key: String, vararg args: Pair<String, Any?>): String {
        val lang = activeLanguage(context)
        ensureLoaded(context, lang)
        return format(key, lang, *args)
    }

    fun applyLocale(context: Context, language: AppLanguage) {
        try {
            currentAppLanguage = language
            ensureLoaded(context, language)
            RetrofitClient.setActiveLanguage(language.code)
            val locale = Locale(language.code)
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ARABIC }

@Composable
fun AppLocalizationProvider(
    currentLanguage: AppLanguage,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Preload bundle synchronously during composition if needed
    Translations.ensureLoaded(context, currentLanguage)

    LaunchedEffect(currentLanguage) {
        Translations.applyLocale(context, currentLanguage)
        NotificationHelper.createNotificationChannels(context)
        (context as? Activity)?.window?.decorView?.layoutDirection =
            if (currentLanguage.direction == LayoutDirection.Rtl) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
        ServerLocalizationSyncManager.syncLanguageAsync(context, currentLanguage, pushPreferenceToServer = false)
    }

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalLayoutDirection provides currentLanguage.direction,
        content = content
    )
}

@Composable
fun tr(key: String, fallback: String = ""): String {
    @Suppress("UNUSED_VARIABLE")
    val catalogTick = Translations.catalogVersion
    val currentLang = LocalAppLanguage.current
    val translated = Translations.get(key, currentLang)
    return if (translated == key && fallback.isNotBlank()) fallback else translated
}

@Composable
fun trFmt(key: String, vararg args: Pair<String, Any?>): String {
    @Suppress("UNUSED_VARIABLE")
    val catalogTick = Translations.catalogVersion
    val currentLang = LocalAppLanguage.current
    return Translations.format(key, currentLang, *args)
}
