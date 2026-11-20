package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

enum class ClientCardLinkKind(val labelAr: String, val iconTag: String) {
    APP_DEEP_LINK("ديب لينك التطبيق", "app"),
    DOMAIN_CARD("دومين المشروع", "web"),
    WHATSAPP("واتساب", "wa"),
    INSTAGRAM("إنستغرام", "ig"),
    WEBSITE("رابط خارجي", "link")
}

data class ClientCardLinkItem(
    val id: String = UUID.randomUUID().toString().take(8),
    val keyword: String,
    val url: String,
    val kind: ClientCardLinkKind = ClientCardLinkKind.WEBSITE
)

enum class ClientCardPresetTheme(
    val id: String,
    val titleAr: String,
    val startHex: String,
    val endHex: String,
    val defaultTextHex: String,
    val defaultAccentHex: String
) {
    EXECUTIVE_DARK("EXECUTIVE_DARK", "تنفيذي داكن", "#0F172A", "#1E293B", "#FFFFFF", "#D4AF37"),
    ROYAL_NAVY("ROYAL_NAVY", "أزرق ملكي", "#0B1D3A", "#1E3A8A", "#F8FAFC", "#38BDF8"),
    EMERALD_PRO("EMERALD_PRO", "زمردي", "#064E3B", "#0F766E", "#FFFFFF", "#34D399"),
    BURGUNDY_LUX("BURGUNDY_LUX", "عنابي فاخر", "#3B0764", "#701A75", "#FFFFFF", "#F472B6"),
    CLEAN_LIGHT("CLEAN_LIGHT", "فاتح عصري", "#FFFFFF", "#F1F5F9", "#0F172A", "#1E3A8A")
}

enum class SocialSharePlatform(val labelAr: String, val packageNames: List<String>) {
    WHATSAPP("واتساب", listOf("com.whatsapp", "com.whatsapp.w4b")),
    TELEGRAM("تيليجرام", listOf("org.telegram.messenger", "org.thunderdog.challegram")),
    INSTAGRAM("إنستغرام", listOf("com.instagram.android")),
    FACEBOOK("فيسبوك", listOf("com.facebook.katana", "com.facebook.orca")),
    ALL("المزيد", emptyList())
}

data class ClientCardStyleConfig(
    val displayName: String,
    val roleTitle: String,
    val bioText: String,
    val storeText: String,
    val phoneText: String,
    val backgroundImageUri: String? = null,
    val presetTheme: ClientCardPresetTheme = ClientCardPresetTheme.EXECUTIVE_DARK,
    val overlayAlpha: Float = 0.58f,
    val textColorHex: String = "#FFFFFF",
    val accentColorHex: String = "#D4AF37",
    val fontScaleLevel: Int = 1, // 0: Compact, 1: Medium, 2: Large
    val includeStore: Boolean = true,
    val includeBio: Boolean = true,
    val includeReliability: Boolean = true,
    val includePhone: Boolean = false,
    val customLinks: List<ClientCardLinkItem> = emptyList()
)

object ClientCardDeepLinkRouter {
    private val _pendingCardHandle = MutableStateFlow<String?>(null)
    val pendingCardHandle = _pendingCardHandle.asStateFlow()

    fun handleIncomingUri(uri: Uri?) {
        if (uri == null) return
        val scheme = uri.scheme?.lowercase() ?: return
        val host = uri.host?.lowercase() ?: ""
        val segments = uri.pathSegments.orEmpty()

        val extractedHandle = when {
            scheme == "oozox" && (host == "card" || host == "u" || host == "cv") -> {
                segments.firstOrNull()
            }
            (scheme == "https" || scheme == "http") && (host == "oozox.com" || host == "www.oozox.com") -> {
                when (segments.firstOrNull()?.lowercase()) {
                    "card", "u", "cv" -> segments.getOrNull(1)
                    else -> null
                }
            }
            (scheme == "https" || scheme == "http") && host == "cv.oozox.com" -> {
                segments.firstOrNull()
            }
            else -> null
        }?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() }

        if (extractedHandle != null) {
            _pendingCardHandle.value = extractedHandle
        }
    }

    fun openExternalDeepLinkOrDomain(context: Context, rawUrl: String, fallbackUsername: String) {
        val clean = rawUrl.trim().ifBlank { domainCardUrl(fallbackUsername) }
        val normalized = if (clean.startsWith("http://") || clean.startsWith("https://") ||
            clean.startsWith("oozox://") || clean.startsWith("tel:") || clean.startsWith("mailto:")
        ) clean else "https://$clean"

        try {
            if (normalized.startsWith("oozox://")) {
                handleIncomingUri(Uri.parse(normalized))
                return
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to project domain download/card landing page
            try {
                val webFallback = Intent(Intent.ACTION_VIEW, Uri.parse(domainDownloadUrl(fallbackUsername))).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webFallback)
            } catch (_: Exception) {
                Toast.makeText(context, "رابط الكارت: ${domainCardUrl(fallbackUsername)}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun domainCardUrl(username: String): String {
        val handle = username.trim().removePrefix("@").ifBlank { "member" }
        return "https://oozox.com/card/$handle"
    }

    fun domainDownloadUrl(username: String): String {
        val handle = username.trim().removePrefix("@").ifBlank { "member" }
        return "https://oozox.com/download?card=$handle"
    }

    fun appDeepLinkUrl(username: String): String {
        val handle = username.trim().removePrefix("@").ifBlank { "member" }
        return "oozox://card/$handle"
    }

    fun clearPendingHandle() {
        _pendingCardHandle.value = null
    }
}

object ClientCardPrefsStore {
    private const val PREFS_NAME = "oozox_client_card_studio"

    fun load(context: Context, user: UserEntity): ClientCardStyleConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("card_${user.id}", null)
        val defaultLinks = listOf(
            ClientCardLinkItem(
                keyword = "فتح الكارت في تطبيق OOZOX",
                url = ClientCardDeepLinkRouter.appDeepLinkUrl(user.username.ifBlank { user.id.take(8) }),
                kind = ClientCardLinkKind.APP_DEEP_LINK
            ),
            ClientCardLinkItem(
                keyword = "زيارة الكارت على موقع OOZOX",
                url = ClientCardDeepLinkRouter.domainCardUrl(user.username.ifBlank { user.id.take(8) }),
                kind = ClientCardLinkKind.DOMAIN_CARD
            )
        )

        if (raw.isNullOrBlank()) {
            return ClientCardStyleConfig(
                displayName = user.name,
                roleTitle = user.jobTitle?.takeIf { it.isNotBlank() } ?: "عضو معتمد",
                bioText = user.merchantBio ?: "",
                storeText = user.traderShopName?.takeIf { it.isNotBlank() } ?: "متجر ${user.name}",
                phoneText = user.phone,
                customLinks = defaultLinks
            )
        }

        return try {
            val obj = JSONObject(raw)
            val themeId = obj.optString("presetTheme", ClientCardPresetTheme.EXECUTIVE_DARK.id)
            val theme = ClientCardPresetTheme.entries.firstOrNull { it.id == themeId } ?: ClientCardPresetTheme.EXECUTIVE_DARK
            val linksArr = obj.optJSONArray("customLinks")
            val parsedLinks = mutableListOf<ClientCardLinkItem>()
            if (linksArr != null) {
                for (i in 0 until linksArr.length()) {
                    val item = linksArr.optJSONObject(i) ?: continue
                    val kw = item.optString("keyword")
                    val url = item.optString("url")
                    val kindStr = item.optString("kind", ClientCardLinkKind.WEBSITE.name)
                    val kind = ClientCardLinkKind.entries.firstOrNull { it.name == kindStr } ?: ClientCardLinkKind.WEBSITE
                    if (kw.isNotBlank() && url.isNotBlank()) {
                        parsedLinks.add(
                            ClientCardLinkItem(
                                id = item.optString("id", UUID.randomUUID().toString().take(8)),
                                keyword = kw,
                                url = url,
                                kind = kind
                            )
                        )
                    }
                }
            }
            ClientCardStyleConfig(
                displayName = obj.optString("displayName", user.name),
                roleTitle = obj.optString("roleTitle", user.jobTitle ?: "عضو معتمد"),
                bioText = obj.optString("bioText", user.merchantBio ?: ""),
                storeText = obj.optString("storeText", user.traderShopName ?: "متجر ${user.name}"),
                phoneText = obj.optString("phoneText", user.phone),
                backgroundImageUri = obj.optString("backgroundImageUri", "").ifBlank { null },
                presetTheme = theme,
                overlayAlpha = obj.optDouble("overlayAlpha", 0.58).toFloat().coerceIn(0.1f, 0.9f),
                textColorHex = obj.optString("textColorHex", theme.defaultTextHex),
                accentColorHex = obj.optString("accentColorHex", theme.defaultAccentHex),
                fontScaleLevel = obj.optInt("fontScaleLevel", 1).coerceIn(0, 2),
                includeStore = obj.optBoolean("includeStore", true),
                includeBio = obj.optBoolean("includeBio", true),
                includeReliability = obj.optBoolean("includeReliability", true),
                includePhone = obj.optBoolean("includePhone", false),
                customLinks = if (parsedLinks.isNotEmpty()) parsedLinks else defaultLinks
            )
        } catch (_: Exception) {
            ClientCardStyleConfig(
                displayName = user.name,
                roleTitle = user.jobTitle ?: "عضو معتمد",
                bioText = user.merchantBio ?: "",
                storeText = user.traderShopName ?: "متجر ${user.name}",
                phoneText = user.phone,
                customLinks = defaultLinks
            )
        }
    }

    fun persistBackgroundPhoto(context: Context, uri: Uri, userId: String): String {
        return try {
            val dest = File(context.filesDir, "card_bg_${userId}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            if (dest.exists() && dest.length() > 0L) Uri.fromFile(dest).toString() else uri.toString()
        } catch (_: Exception) {
            uri.toString()
        }
    }

    fun save(context: Context, userId: String, config: ClientCardStyleConfig) {
        try {
            val obj = JSONObject().apply {
                put("displayName", config.displayName)
                put("roleTitle", config.roleTitle)
                put("bioText", config.bioText)
                put("storeText", config.storeText)
                put("phoneText", config.phoneText)
                put("backgroundImageUri", config.backgroundImageUri ?: "")
                put("presetTheme", config.presetTheme.id)
                put("overlayAlpha", config.overlayAlpha.toDouble())
                put("textColorHex", config.textColorHex)
                put("accentColorHex", config.accentColorHex)
                put("fontScaleLevel", config.fontScaleLevel)
                put("includeStore", config.includeStore)
                put("includeBio", config.includeBio)
                put("includeReliability", config.includeReliability)
                put("includePhone", config.includePhone)
                val linksArr = JSONArray()
                config.customLinks.forEach { link ->
                    linksArr.put(JSONObject().apply {
                        put("id", link.id)
                        put("keyword", link.keyword)
                        put("url", link.url)
                        put("kind", link.kind.name)
                    })
                }
                put("customLinks", linksArr)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("card_$userId", obj.toString())
                .apply()
        } catch (_: Exception) {
        }
    }
}

object ClientCardRenderer {

    fun buildSocialCaption(
        config: ClientCardStyleConfig,
        username: String,
        trustScore: Int,
        reliabilitySummary: String,
        projectsCount: Int = 0,
        completedDealsCount: Int = 0,
        skills: String? = null
    ): String {
        val handle = username.ifBlank { "member" }
        val cardDomainUrl = ClientCardDeepLinkRouter.domainCardUrl(handle)
        val appDeepUrl = ClientCardDeepLinkRouter.appDeepLinkUrl(handle)
        val downloadUrl = ClientCardDeepLinkRouter.domainDownloadUrl(handle)

        return buildString {
            appendLine("✨ بطاقة الأعمال الموثقة على منصة OOZOX — ${config.displayName}")
            appendLine("💼 ${config.roleTitle} | موثوقية معتمدة: $trustScore%")
            if (projectsCount > 0 || completedDealsCount > 0) {
                appendLine("🏆 سجل الإنجازات: $projectsCount مشاريع • $completedDealsCount صفقة وفاتورة موثقة")
            }
            if (!skills.isNullOrBlank()) {
                appendLine("🛠️ التخصص والمهارات: ${skills.lines().take(3).joinToString(" • ").take(100)}")
            }
            if (config.includeBio && config.bioText.isNotBlank()) {
                appendLine("❝ ${config.bioText.take(140)} ❞")
            }
            if (config.includeStore && config.storeText.isNotBlank()) {
                appendLine("🏢 النشاط / المتجر: ${config.storeText}")
            }
            if (config.includePhone && config.phoneText.isNotBlank()) {
                appendLine("📞 التواصل المباشر: ${config.phoneText}")
            }
            if (config.includeReliability) {
                appendLine("✅ $reliabilitySummary")
            }
            if (config.customLinks.isNotEmpty()) {
                appendLine()
                appendLine("🔗 روابط الوصول السريع:")
                config.customLinks.forEach { link ->
                    appendLine("• ${link.keyword}: ${link.url}")
                }
            }
            appendLine()
            appendLine("🚀 شاهد أعمالي الموثقة وتواصل معي مباشرة عبر تطبيق OOZOX:")
            appendLine("◀ ملف الكارت والأعمال: $cardDomainUrl")
            appendLine("◀ حمّل تطبيق OOZOX للأعمال والحسابات والمشاريع:")
            appendLine("$downloadUrl")
            appendLine("◀ فتح مباشر بالتطبيق: $appDeepUrl")
            append("#OOZOX #أعمال_موثقة #إدارة_مشاريع #فواتير")
        }
    }

    fun renderClientCardPngFile(
        context: Context,
        user: UserEntity,
        config: ClientCardStyleConfig,
        trustScore: Int,
        reliabilitySummary: String,
        projectsCount: Int = 0,
        completedDealsCount: Int = 0,
        skills: String? = user.skills
    ): File? {
        return try {
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            val startColor = safeColor(config.presetTheme.startHex, Color.parseColor("#0F172A"))
            val endColor = safeColor(config.presetTheme.endHex, Color.parseColor("#1E293B"))
            val textColor = safeColor(config.textColorHex, Color.WHITE)
            val accentColor = safeColor(config.accentColorHex, Color.parseColor("#D4AF37"))

            val bgLoaded = decodeUriBitmap(context, config.backgroundImageUri)
            if (bgLoaded != null) {
                val srcRatio = bgLoaded.width.toFloat() / bgLoaded.height.toFloat()
                val dstRatio = width.toFloat() / height.toFloat()
                val srcRect = if (srcRatio > dstRatio) {
                    val cropW = (bgLoaded.height * dstRatio).toInt()
                    val left = (bgLoaded.width - cropW) / 2
                    Rect(left, 0, left + cropW, bgLoaded.height)
                } else {
                    val cropH = (bgLoaded.width / dstRatio).toInt()
                    val top = (bgLoaded.height - cropH) / 2
                    Rect(0, top, bgLoaded.width, top + cropH)
                }
                canvas.drawBitmap(bgLoaded, srcRect, Rect(0, 0, width, height), paint)
                bgLoaded.recycle()

                val lum = (0.299f * Color.red(textColor) + 0.587f * Color.green(textColor) + 0.114f * Color.blue(textColor)) / 255f
                val scrimBase = if (lum > 0.5f) Color.BLACK else Color.WHITE
                paint.color = Color.argb(
                    (config.overlayAlpha * 255).toInt().coerceIn(30, 235),
                    Color.red(scrimBase),
                    Color.green(scrimBase),
                    Color.blue(scrimBase)
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            } else {
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    startColor, endColor, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null
            }

            // Outer Frame
            val frameRect = RectF(44f, 44f, (width - 44).toFloat(), (height - 44).toFloat())
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = adjustAlpha(accentColor, 0.55f)
            canvas.drawRoundRect(frameRect, 42f, 42f, paint)
            paint.style = Paint.Style.FILL

            // Top Brand + Trust Pill Row
            textPaint.color = accentColor
            textPaint.textSize = 28f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("OOZOX VERIFIED PRO", 92f, 122f, textPaint)

            val trustText = "$trustScore% موثوق ✓"
            textPaint.textSize = 25f
            val pillW = textPaint.measureText(trustText) + 54f
            val pillRect = RectF(width - 92f - pillW, 86f, width - 92f, 138f)
            paint.color = adjustAlpha(accentColor, 0.22f)
            canvas.drawRoundRect(pillRect, 26f, 26f, paint)
            textPaint.color = accentColor
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(trustText, pillRect.centerX(), pillRect.centerY() + 8f, textPaint)

            // Avatar Circle (renders merchant/user photo if present, otherwise initials)
            val avatarCenterX = width - 166f
            val avatarCenterY = 248f
            val avatarRadius = 72f
            val avatarBitmap = decodeUriBitmap(context, user.avatarUri)
            if (avatarBitmap != null) {
                val clipPath = Path().apply {
                    addCircle(avatarCenterX, avatarCenterY, avatarRadius, Path.Direction.CW)
                }
                val srcSize = minOf(avatarBitmap.width, avatarBitmap.height)
                val srcLeft = (avatarBitmap.width - srcSize) / 2
                val srcTop = (avatarBitmap.height - srcSize) / 2
                val srcRect = Rect(srcLeft, srcTop, srcLeft + srcSize, srcTop + srcSize)
                val dstRect = RectF(
                    avatarCenterX - avatarRadius,
                    avatarCenterY - avatarRadius,
                    avatarCenterX + avatarRadius,
                    avatarCenterY + avatarRadius
                )
                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(avatarBitmap, srcRect, dstRect, paint)
                canvas.restore()
                avatarBitmap.recycle()

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = accentColor
                canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, paint)
                paint.style = Paint.Style.FILL
            } else {
                paint.color = adjustAlpha(accentColor, 0.24f)
                canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = accentColor
                canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, paint)
                paint.style = Paint.Style.FILL

                textPaint.color = textColor
                textPaint.textSize = 52f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textAlign = Paint.Align.CENTER
                val initial = config.displayName.trim().take(2).ifBlank { "O" }
                canvas.drawText(initial, avatarCenterX, avatarCenterY + 18f, textPaint)
            }

            val scaleFactor = when (config.fontScaleLevel) {
                0 -> 0.88f
                2 -> 1.12f
                else -> 1.0f
            }
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = textColor
            textPaint.textSize = 46f * scaleFactor
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(config.displayName, width - 270f, 226f, textPaint)

            textPaint.color = accentColor
            textPaint.textSize = 29f * scaleFactor
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(config.roleTitle, width - 270f, 270f, textPaint)

            textPaint.color = adjustAlpha(textColor, 0.78f)
            textPaint.textSize = 24f
            canvas.drawText("@${user.username}  •  ★ ${user.ratingScore} (${user.totalReviewsCount} تقييم)", width - 270f, 312f, textPaint)

            // Metrics Strip: Projects + Deals + Experience
            val metricsTop = 352f
            val metricsRect = RectF(92f, metricsTop, (width - 92).toFloat(), metricsTop + 76f)
            paint.color = adjustAlpha(accentColor, 0.14f)
            canvas.drawRoundRect(metricsRect, 18f, 18f, paint)
            textPaint.color = textColor
            textPaint.textSize = 24f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textAlign = Paint.Align.CENTER
            val statsSummary = "المشاريع: $projectsCount   |   الصفقات والفواتير: $completedDealsCount   |   الخبرة: ${user.experienceYears} سنوات"
            canvas.drawText(statsSummary, metricsRect.centerX(), metricsRect.centerY() + 8f, textPaint)

            var curY = 478f

            if (config.includeBio && config.bioText.isNotBlank()) {
                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.color = adjustAlpha(textColor, 0.92f)
                textPaint.textSize = 27f * scaleFactor
                textPaint.typeface = Typeface.DEFAULT
                wrapWords(config.bioText, 44).take(3).forEach { line ->
                    canvas.drawText(line, (width - 92).toFloat(), curY, textPaint)
                    curY += 38f * scaleFactor
                }
                curY += 16f
            }

            if (config.includeStore && config.storeText.isNotBlank()) {
                curY = drawCardInfoLine(canvas, textPaint, accentColor, textColor, width, curY, "المتجر / النشاط: ${config.storeText}", scaleFactor)
            }
            if (!skills.isNullOrBlank()) {
                val topSkills = skills.split(",", "،", "\n").map { it.trim() }.filter { it.isNotBlank() }.take(4).joinToString(" • ")
                if (topSkills.isNotBlank()) {
                    curY = drawCardInfoLine(canvas, textPaint, accentColor, textColor, width, curY, "المهارات: $topSkills", scaleFactor)
                }
            }
            if (config.includePhone && config.phoneText.isNotBlank()) {
                curY = drawCardInfoLine(canvas, textPaint, accentColor, textColor, width, curY, "الهاتف: ${config.phoneText}", scaleFactor)
            }
            if (config.includeReliability) {
                curY = drawCardInfoLine(canvas, textPaint, accentColor, textColor, width, curY, reliabilitySummary, scaleFactor)
            }

            if (config.customLinks.isNotEmpty()) {
                curY += 12f
                textPaint.color = accentColor
                textPaint.textSize = 24f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("الروابط والوصول السريع:", (width - 92).toFloat(), curY, textPaint)
                curY += 20f

                config.customLinks.take(3).forEach { link ->
                    if (curY < height - 350f) {
                        val rowRect = RectF(92f, curY, (width - 92).toFloat(), curY + 64f)
                        paint.color = adjustAlpha(accentColor, 0.14f)
                        canvas.drawRoundRect(rowRect, 18f, 18f, paint)
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1.5f
                        paint.color = adjustAlpha(accentColor, 0.45f)
                        canvas.drawRoundRect(rowRect, 18f, 18f, paint)
                        paint.style = Paint.Style.FILL

                        textPaint.color = textColor
                        textPaint.textSize = 24f * scaleFactor
                        textPaint.typeface = Typeface.DEFAULT_BOLD
                        textPaint.textAlign = Paint.Align.RIGHT
                        canvas.drawText(link.keyword.take(34), (width - 120).toFloat(), curY + 40f, textPaint)

                        textPaint.color = accentColor
                        textPaint.textSize = 20f
                        textPaint.typeface = Typeface.DEFAULT
                        textPaint.textAlign = Paint.Align.LEFT
                        canvas.drawText(link.kind.labelAr, 120f, curY + 39f, textPaint)
                        curY += 76f
                    }
                }
            }

            // Mutual OOZOX Promotional Footer + QR Code
            val footerTop = height - 292f
            paint.color = adjustAlpha(accentColor, 0.18f)
            canvas.drawRoundRect(RectF(76f, footerTop, (width - 76).toFloat(), (height - 76).toFloat()), 28f, 28f, paint)

            val handle = user.username.ifBlank { user.id.take(8) }
            val domainUrl = ClientCardDeepLinkRouter.domainCardUrl(handle)
            drawQrCodeOnCanvas(canvas, domainUrl, 104f, footerTop + 28f, 148f, accentColor)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = accentColor
            textPaint.textSize = 24f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("مدعوم من منصة OOZOX للأعمال الذكية", (width - 108).toFloat(), footerTop + 56f, textPaint)

            textPaint.color = textColor
            textPaint.textSize = 25f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("حمّل تطبيق OOZOX لتصفح أعمالي وتنفيذ صفقاتك", (width - 108).toFloat(), footerTop + 98f, textPaint)

            textPaint.color = accentColor
            textPaint.textSize = 22f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(domainUrl, (width - 108).toFloat(), footerTop + 140f, textPaint)

            textPaint.color = adjustAlpha(textColor, 0.78f)
            textPaint.textSize = 19f
            canvas.drawText("oozox://card/$handle  •  امسح الباركود لفتح الكارت", (width - 108).toFloat(), footerTop + 178f, textPaint)

            val file = File(context.cacheDir, "OOZOX_ClientCard_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 96, out)
            }
            bitmap.recycle()
            file
        } catch (_: Exception) {
            null
        }
    }

    fun shareCardToSocialPlatform(
        context: Context,
        user: UserEntity,
        config: ClientCardStyleConfig,
        trustScore: Int,
        reliabilitySummary: String,
        platform: SocialSharePlatform,
        projectsCount: Int = 0,
        completedDealsCount: Int = 0
    ) {
        val handle = user.username.ifBlank { user.id.take(8) }
        val caption = buildSocialCaption(
            config = config,
            username = handle,
            trustScore = trustScore,
            reliabilitySummary = reliabilitySummary,
            projectsCount = projectsCount,
            completedDealsCount = completedDealsCount,
            skills = user.skills
        )
        val imageFile = renderClientCardPngFile(
            context = context,
            user = user,
            config = config,
            trustScore = trustScore,
            reliabilitySummary = reliabilitySummary,
            projectsCount = projectsCount,
            completedDealsCount = completedDealsCount,
            skills = user.skills
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, caption)
            if (imageFile != null && imageFile.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }

        if (platform != SocialSharePlatform.ALL) {
            val pm = context.packageManager
            val matchedPkg = platform.packageNames.firstOrNull { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            if (matchedPkg != null) {
                try {
                    val targeted = Intent(shareIntent).apply { setPackage(matchedPkg) }
                    context.startActivity(targeted)
                    return
                } catch (_: Exception) {
                }
            }
        }

        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الكارت والأعمال (${platform.labelAr})"))
    }

    private fun drawCardInfoLine(
        canvas: Canvas,
        textPaint: Paint,
        accentColor: Int,
        textColor: Int,
        width: Int,
        y: Float,
        text: String,
        scaleFactor: Float
    ): Float {
        textPaint.color = accentColor
        textPaint.textSize = 26f * scaleFactor
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("◆", (width - 96).toFloat(), y, textPaint)

        textPaint.color = textColor
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(text.take(46), (width - 130).toFloat(), y, textPaint)
        return y + (44f * scaleFactor)
    }

    private fun drawQrCodeOnCanvas(
        canvas: Canvas,
        content: String,
        left: Float,
        top: Float,
        size: Float,
        accentColor: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(RectF(left, top, left + size, top + size), 18f, 18f, bgPaint)

        val pad = 10f
        val inner = size - (pad * 2f)
        val matrixSize = 21
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        val grid = Array(matrixSize) { BooleanArray(matrixSize) }
        for (i in 0 until matrixSize) {
            for (j in 0 until matrixSize) {
                val byteIdx = (i * matrixSize + j) % hashBytes.size
                val bitIdx = (i + j) % 8
                grid[i][j] = ((hashBytes[byteIdx].toInt() ushr bitIdx) and 1) == 1
            }
        }
        fun setFinder(sr: Int, sc: Int) {
            for (r in 0 until 7) for (c in 0 until 7) {
                grid[sr + r][sc + c] = (r == 0 || r == 6 || c == 0 || c == 6) || (r in 2..4 && c in 2..4)
            }
        }
        setFinder(0, 0)
        setFinder(0, matrixSize - 7)
        setFinder(matrixSize - 7, 0)

        val cell = inner / matrixSize
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (grid[r][c]) {
                    val isCorner = (r < 7 && c < 7) || (r < 7 && c >= matrixSize - 7) || (r >= matrixSize - 7 && c < 7)
                    cellPaint.color = if (isCorner) accentColor else Color.parseColor("#0F172A")
                    val x = left + pad + c * cell
                    val y = top + pad + r * cell
                    canvas.drawRect(x, y, x + cell * 0.9f, y + cell * 0.9f, cellPaint)
                }
            }
        }
    }

    private fun decodeUriBitmap(context: Context, uriStr: String?): Bitmap? {
        if (uriStr.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriStr)
            if (uri.scheme == "file" && uri.path != null) {
                BitmapFactory.decodeFile(uri.path)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun wrapWords(text: String, maxChars: Int): List<String> {
        val result = mutableListOf<String>()
        var current = ""
        text.split(" ").forEach { word ->
            if ((current.length + word.length + 1) <= maxChars) {
                current = if (current.isEmpty()) word else "$current $word"
            } else {
                if (current.isNotEmpty()) result.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) result.add(current)
        return result
    }

    private fun safeColor(hex: String, fallback: Int): Int =
        try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }

    private fun adjustAlpha(color: Int, factor: Float): Int =
        Color.argb((Color.alpha(color) * factor).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
