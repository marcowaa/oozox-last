package com.example.util

import com.example.data.model.AdminConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiAiService {

    /**
     * Query Gemini API with prompt, system persona, and financial context.
     * Supports Thinking Mode with gemini-3.1-pro-preview and thinkingLevel: HIGH.
     */
    suspend fun queryGeminiAi(
        userQuery: String,
        contextInfo: String,
        adminConfig: AdminConfigEntity?,
        useThinkingMode: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(adminConfig)
        
        // When thinking mode is enabled, use gemini-3.1-pro-preview with HIGH thinking level
        val modelName = if (useThinkingMode) {
            "gemini-3.1-pro-preview"
        } else {
            adminConfig?.geminiModelName?.ifBlank { "gemini-3.5-flash" } ?: "gemini-3.5-flash"
        }
        
        val systemPrompt = adminConfig?.aiSystemPrompt?.ifBlank {
            "أنت المحاسب والمستشار المالي الذكي المعتمد لمنصة الصاغة، وخبير استراتيجيات تداول الذهب، والتحوط من التضخم، ومحاسبة تكاليف المقاولات وإدارة الديون والمقاصة. قدم إجابات مالية ومحاسبية دقيقة واحترافية باللغة العربية مع خطوات تفكير وتحليل تفصيلي وتوصيات واضحة."
        } ?: "أنت المحاسب والمستشار المالي الذكي المعتمد لمنصة الصاغة، وخبير استراتيجيات تداول الذهب، والتحوط من التضخم، ومحاسبة تكاليف المقاولات وإدارة الديون والمقاصة. قدم إجابات مالية ومحاسبية دقيقة واحترافية باللغة العربية مع خطوات تفكير وتحليل تفصيلي وتوصيات واضحة."

        if (apiKey.isNullOrBlank()) {
            return@withContext "⚠️ لم يتم إعداد مفتاح Gemini API. يرجى إدخال GEMINI_API_KEY في لوحة الإدارة أو إعدادات السيرفر لتفعيل المساعد الذكي."
        }

        try {
            val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val url = URL(endpointUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            // Construct JSON request for Gemini
            val fullPrompt = "$systemPrompt\n\n[سياق البيانات المالية الحالية في التطبيق]:\n$contextInfo\n\n[سؤال/طلب المستخدم]:\n$userQuery"

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", fullPrompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                // High Thinking Configuration for gemini-3.1-pro-preview
                if (useThinkingMode) {
                    val genConfig = JSONObject().apply {
                        val thinkingConfig = JSONObject().apply {
                            put("thinkingLevel", "HIGH")
                        }
                        put("thinkingConfig", thinkingConfig)
                    }
                    put("generationConfig", genConfig)
                }
            }

            val wr = OutputStreamWriter(conn.outputStream, "UTF-8")
            wr.write(jsonBody.toString())
            wr.flush()
            wr.close()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val jsonResponse = JSONObject(sb.toString())
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return@withContext text
                        }
                    }
                }
            } else {
                val errorStream = conn.errorStream
                val errReader = BufferedReader(InputStreamReader(errorStream ?: conn.inputStream, "UTF-8"))
                val errSb = StringBuilder()
                var errLine: String?
                while (errReader.readLine().also { errLine = it } != null) {
                    errSb.append(errLine)
                }
                errReader.close()
                return@withContext "⚠️ تعذر الاتصال بـ Gemini API (كود الاستجابة: $responseCode).\nالسبب: ${errSb.take(200)}"
            }
        } catch (e: Exception) {
            return@withContext "⚠️ تعذر الوصول إلى خدمة Gemini API: ${e.localizedMessage ?: "خطأ في الاتصال"}"
        }

        return@withContext "⚠️ لم يرجع نموذج Gemini أي استجابة نصية. يرجى المحاولة لاحقاً."
    }

    private fun getApiKey(adminConfig: AdminConfigEntity?): String? {
        if (!adminConfig?.geminiApiKey.isNullOrBlank()) {
            return adminConfig?.geminiApiKey
        }
        return try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String
            if (key.isNullOrBlank() || key.contains("PLACEHOLDER") || key.contains("CHANGE_ME")) null else key
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Contextual Chat Translation powered by Gemini AI.
     * Takes recent conversation history into account to produce fluent, natural,
     * nuance-aware translations (not word-for-word literal translations).
     */
    suspend fun translateContextually(
        targetMessage: String,
        surroundingContext: List<String> = emptyList(),
        targetLanguage: String = "auto",
        adminConfig: AdminConfigEntity? = null
    ): ContextualTranslationResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(adminConfig)
        val modelName = adminConfig?.geminiModelName?.ifBlank { "gemini-3.5-flash" } ?: "gemini-3.5-flash"

        val hasArabic = targetMessage.any { it in '\u0600'..'\u06FF' }
        val resolvedTargetLang = if (targetLanguage == "auto") {
            if (hasArabic) "en" else "ar"
        } else targetLanguage

        val targetLangLabel = if (resolvedTargetLang == "ar") "اللغة العربية الفصحى الطبيعية المعاصرة" else "Fluent natural English"

        if (apiKey.isNullOrBlank()) {
            return@withContext ContextualTranslationResult(
                originalText = targetMessage,
                translatedText = targetMessage,
                sourceLanguage = if (hasArabic) "ar" else "en",
                targetLanguage = resolvedTargetLang,
                contextualNotes = "الترجمة غير متاحة: مفتاح Gemini API غير مُعد",
                isAiPowered = false
            )
        }

        try {
            val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val url = URL(endpointUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            conn.connectTimeout = 20000
            conn.readTimeout = 20000

            val contextSnippet = if (surroundingContext.isNotEmpty()) {
                "[سياق المحادثة الأخيرة والحديث الدائر بين الطرفين]:\n" + surroundingContext.takeLast(5).joinToString("\n") { "• $it" }
            } else "[لا يوجد سياق إضافي مسبق]"

            val systemInstruction = """
                أنت خبير الترجمة الفورية والسياقية الاحترافية لمنصة OOZOX.
                مهمتك ترجمة الرسالة المحددة بدقة وفهم عميق لسياق المحادثة، ونية المتحدث، ومصطلحات الأعمال والتجارة والذهب والحياة اليومية.
                
                قواعد صارمة:
                1. ممنوع منعاً باتاً الترجمة الحرفية كلمة بكلمة.
                2. انقل المعنى والمقصد والروح بأسلوب سلس وطبيعي 100%.
                3. اللغة المستهدفة للترجمة هي: $targetLangLabel.
                4. قم بالرد فقط بالنص المترجم مباشرة بدون أي مقدمات أو شروحات إضافية أو علامات اقتباس.
            """.trimIndent()

            val fullPrompt = "$systemInstruction\n\n$contextSnippet\n\n[الرسالة المطلوب ترجمتها سياقياً]:\n\"$targetMessage\""

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", fullPrompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val wr = OutputStreamWriter(conn.outputStream, "UTF-8")
            wr.write(jsonBody.toString())
            wr.flush()
            wr.close()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val jsonResponse = JSONObject(sb.toString())
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val translatedText = parts.getJSONObject(0).optString("text", "").trim()
                            .removeSurrounding("\"")
                            .removeSurrounding("«", "»")
                            .trim()

                        if (translatedText.isNotBlank()) {
                            return@withContext ContextualTranslationResult(
                                originalText = targetMessage,
                                translatedText = translatedText,
                                sourceLanguage = if (hasArabic) "ar" else "en",
                                targetLanguage = resolvedTargetLang,
                                contextualNotes = if (surroundingContext.isNotEmpty()) "ترجمة ذكية وفق سياق المحادثة المباشر" else null,
                                isAiPowered = true
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return@withContext ContextualTranslationResult(
            originalText = targetMessage,
            translatedText = targetMessage,
            sourceLanguage = if (hasArabic) "ar" else "en",
            targetLanguage = resolvedTargetLang,
            contextualNotes = "تعذرت الترجمة عبر السيرفر حالياً",
            isAiPowered = false
        )
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

