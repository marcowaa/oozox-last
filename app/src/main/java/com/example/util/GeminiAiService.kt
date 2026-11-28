package com.example.util

import com.example.data.model.AdminConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object GeminiAiService {

    private fun isEnglish(): Boolean = Translations.currentAppLanguage == AppLanguage.ENGLISH

    private fun parseProxyError(rawBody: String?, statusCode: Int): String {
        val code = try {
            if (!rawBody.isNullOrBlank()) JSONObject(rawBody).optString("code", "").uppercase() else ""
        } catch (_: Exception) {
            ""
        }
        val en = isEnglish()
        return when (code) {
            "AI_DAILY_QUOTA_EXCEEDED" -> if (en) {
                "Daily AI request limit reached. Try again tomorrow."
            } else {
                "وصلت إلى الحد اليومي لاستخدام المساعد الذكي، حاول غداً."
            }
            "AI_MONTHLY_QUOTA_EXCEEDED" -> if (en) {
                "Monthly AI quota reached."
            } else {
                "تم استنفاد الحصة الشهرية للمساعد الذكي."
            }
            "AI_MONTHLY_BUDGET_EXCEEDED" -> if (en) {
                "AI service is temporarily paused until the next billing cycle."
            } else {
                "خدمة المساعد الذكي متوقفة مؤقتاً لبلوغ سقف التشغيل الشهري."
            }
            "AI_NOT_CONFIGURED" -> if (en) {
                "AI service is not configured on the server yet."
            } else {
                "خدمة المساعد الذكي غير مفعّلة على الخادم حالياً."
            }
            else -> AppErrorMapper.fromApiError(
                code = code,
                serverMessage = null,
                statusCode = statusCode,
                fallbackAr = "تعذر الحصول على رد المساعد الذكي، حاول مجدداً",
                fallbackEn = "Couldn't get a response right now"
            )
        }
    }

    /**
     * Query OOZOX server-side Gemini proxy (/api/ai/chat).
     * API keys and monthly budgets are strictly enforced on the backend.
     */
    suspend fun queryGeminiAi(
        userQuery: String,
        contextInfo: String,
        adminConfig: AdminConfigEntity?,
        useThinkingMode: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        if (!RetrofitClient.hasActiveSession()) {
            return@withContext if (isEnglish()) {
                "Please sign in to use the AI assistant."
            } else {
                "يرجى تسجيل الدخول لاستخدام المساعد الذكي."
            }
        }

        val modelName = if (useThinkingMode) {
            "gemini-2.5-pro"
        } else {
            adminConfig?.geminiModelName?.takeIf { it.isNotBlank() }.orEmpty()
        }

        val systemPrompt = adminConfig?.aiSystemPrompt?.ifBlank {
            "أنت المحاسب والمستشار المالي الذكي المعتمد لمنصة الصاغة، وخبير استراتيجيات تداول الذهب، والتحوط من التضخم، ومحاسبة تكاليف المقاولات وإدارة الديون والمقاصة."
        } ?: ""

        try {
            val response = RetrofitClient.apiService().postAiChat(
                ApiAiChatRequest(
                    prompt = userQuery.trim(),
                    contextInfo = contextInfo.take(15000),
                    systemPrompt = systemPrompt.take(3500),
                    model = modelName,
                    useThinkingMode = useThinkingMode,
                    mode = "ACCOUNTANT"
                )
            )
            if (response.isSuccessful) {
                val text = response.body()?.data?.text.orEmpty().trim()
                if (text.isNotBlank()) return@withContext text
            }
            return@withContext parseProxyError(response.errorBody()?.string(), response.code())
        } catch (e: Exception) {
            return@withContext AppErrorMapper.fromThrowable(
                e,
                fallbackAr = "تعذر الاتصال بالمساعد الذكي، تحقق من الإنترنت",
                fallbackEn = "Couldn't reach assistant. Check internet"
            )
        }
    }

    /**
     * Multimodal image analysis via server-side Gemini proxy (/api/ai/vision).
     */
    suspend fun analyzeVisionImage(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        adminConfig: AdminConfigEntity? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!RetrofitClient.hasActiveSession()) {
            val msg = if (isEnglish()) "Please sign in first." else "يرجى تسجيل الدخول أولاً."
            return@withContext Result.failure(IllegalStateException(msg))
        }
        try {
            val response = RetrofitClient.apiService().postAiVision(
                ApiAiVisionRequest(
                    prompt = prompt.trim(),
                    imageBase64 = imageBase64,
                    mimeType = mimeType,
                    model = adminConfig?.geminiModelName.orEmpty(),
                    systemPrompt = adminConfig?.aiSystemPrompt.orEmpty()
                )
            )
            if (response.isSuccessful) {
                val text = response.body()?.data?.text.orEmpty().trim()
                if (text.isNotBlank()) return@withContext Result.success(text)
            }
            Result.failure(IllegalStateException(parseProxyError(response.errorBody()?.string(), response.code())))
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    AppErrorMapper.fromThrowable(
                        e,
                        fallbackAr = "تعذر تحليل الصورة عبر الخادم، حاول مجدداً",
                        fallbackEn = "Image analysis unavailable right now"
                    )
                )
            )
        }
    }

    /**
     * Contextual Chat Translation powered by server-side Gemini proxy (/api/ai/chat).
     */
    suspend fun translateContextually(
        targetMessage: String,
        surroundingContext: List<String> = emptyList(),
        targetLanguage: String = "auto",
        adminConfig: AdminConfigEntity? = null
    ): ContextualTranslationResult = withContext(Dispatchers.IO) {
        val hasArabic = targetMessage.any { it in '\u0600'..'\u06FF' }
        val resolvedTargetLang = if (targetLanguage == "auto") {
            if (hasArabic) "en" else "ar"
        } else targetLanguage

        if (!RetrofitClient.hasActiveSession()) {
            return@withContext ContextualTranslationResult(
                originalText = targetMessage,
                translatedText = targetMessage,
                sourceLanguage = if (hasArabic) "ar" else "en",
                targetLanguage = resolvedTargetLang,
                contextualNotes = if (isEnglish()) "Sign in to translate" else "سجّل الدخول لتفعيل الترجمة",
                isAiPowered = false
            )
        }

        val contextSnippet = if (surroundingContext.isNotEmpty()) {
            surroundingContext.takeLast(5).joinToString("\n") { "• $it" }
        } else ""

        try {
            val response = RetrofitClient.apiService().postAiChat(
                ApiAiChatRequest(
                    prompt = targetMessage,
                    contextInfo = contextSnippet,
                    model = adminConfig?.geminiModelName.orEmpty(),
                    useThinkingMode = false,
                    mode = "TRANSLATE",
                    targetLanguage = resolvedTargetLang
                )
            )
            if (response.isSuccessful) {
                val translatedText = response.body()?.data?.text.orEmpty().trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("«", "»")
                    .trim()
                if (translatedText.isNotBlank()) {
                    return@withContext ContextualTranslationResult(
                        originalText = targetMessage,
                        translatedText = translatedText,
                        sourceLanguage = if (hasArabic) "ar" else "en",
                        targetLanguage = resolvedTargetLang,
                        contextualNotes = if (surroundingContext.isNotEmpty()) {
                            "ترجمة ذكية وفق سياق المحادثة المباشر"
                        } else null,
                        isAiPowered = true
                    )
                }
            }
            val errMsg = parseProxyError(response.errorBody()?.string(), response.code())
            return@withContext ContextualTranslationResult(
                originalText = targetMessage,
                translatedText = targetMessage,
                sourceLanguage = if (hasArabic) "ar" else "en",
                targetLanguage = resolvedTargetLang,
                contextualNotes = errMsg,
                isAiPowered = false
            )
        } catch (e: Exception) {
            return@withContext ContextualTranslationResult(
                originalText = targetMessage,
                translatedText = targetMessage,
                sourceLanguage = if (hasArabic) "ar" else "en",
                targetLanguage = resolvedTargetLang,
                contextualNotes = AppErrorMapper.fromThrowable(
                    e,
                    fallbackAr = "تعذرت الترجمة عبر الخادم حالياً",
                    fallbackEn = "Translation unavailable right now"
                ),
                isAiPowered = false
            )
        }
    }
}

data class ContextualTranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "ar",
    val contextualNotes: String? = null,
    val isAiPowered: Boolean = true
)
