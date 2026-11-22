package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.GoldKarat

object NotificationHelper {

    const val CHANNEL_MESSAGES = "oozox_messages_channel"
    const val CHANNEL_PROJECTS = "oozox_projects_channel"
    const val CHANNEL_DEALS = "oozox_deals_channel"
    const val CHANNEL_PRICE_ALERTS = "oozox_price_alerts_channel"
    const val CHANNEL_SYSTEM = "oozox_system_channel"
    const val CHANNEL_FINANCIAL_ALERTS = "oozox_financial_alerts_channel"
    const val CHANNEL_GEOFENCING = "oozox_geofencing_channel"

    const val EXTRA_DESTINATION = "destination"
    const val EXTRA_CHAT_ID = "chat_id"
    const val EXTRA_PROJECT_ID = "project_id"
    const val EXTRA_TRANSACTION_ID = "transaction_id"
    const val KEY_TEXT_REPLY = "key_text_reply"

    const val DESTINATION_CHAT = "chat_detail"
    const val DESTINATION_PROJECT = "project_detail"
    const val DESTINATION_TRANSACTION = "transaction_detail"
    const val DESTINATION_GOLD = "gold_tab"

    /**
     * Initializes Android 8.0+ Notification Channels with localized names, descriptions, vibration, and LED badges.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val lang = Translations.activeLanguage(context)
            Translations.ensureLoaded(context, lang)

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                Translations.get("notif.channel_messages_name", lang),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Translations.get("notif.channel_messages_desc", lang)
                enableLights(true)
                lightColor = Color.parseColor("#00BFA5")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setShowBadge(true)
            }

            val projectsChannel = NotificationChannel(
                CHANNEL_PROJECTS,
                Translations.get("notif.channel_projects_name", lang),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Translations.get("notif.channel_projects_desc", lang)
                enableLights(true)
                lightColor = Color.parseColor("#00BFA5")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
            }

            val dealsChannel = NotificationChannel(
                CHANNEL_DEALS,
                Translations.get("notif.channel_deals_name", lang),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Translations.get("notif.channel_deals_desc", lang)
                enableLights(true)
                lightColor = Color.parseColor("#FFD700")
                enableVibration(true)
                setShowBadge(true)
            }

            val priceAlertsChannel = NotificationChannel(
                CHANNEL_PRICE_ALERTS,
                Translations.get("notif.channel_price_name", lang),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Translations.get("notif.channel_price_desc", lang)
                enableLights(true)
                lightColor = Color.parseColor("#FFD700")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
                setShowBadge(true)
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                Translations.get("notif.channel_system_name", lang),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = Translations.get("notif.channel_system_desc", lang)
                setShowBadge(false)
            }

            val financialChannel = NotificationChannel(
                CHANNEL_FINANCIAL_ALERTS,
                Translations.get("notif.channel_financial_name", lang),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Translations.get("notif.channel_financial_desc", lang)
                enableLights(true)
                lightColor = Color.parseColor("#EF4444")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setShowBadge(true)
            }

            val geofencingChannel = NotificationChannel(
                CHANNEL_GEOFENCING,
                Translations.get("notif.channel_geofence_name", lang),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = Translations.get("notif.channel_geofence_desc", lang)
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(
                    messagesChannel,
                    projectsChannel,
                    dealsChannel,
                    priceAlertsChannel,
                    systemChannel,
                    financialChannel,
                    geofencingChannel
                )
            )
        }
    }

    /**
     * Creates a high-quality circular avatar Bitmap from an image URI or generates a branded initial avatar.
     */
    fun createCircularAvatarBitmap(
        context: Context,
        name: String,
        avatarUri: String? = null,
        sizePx: Int = 140
    ): Bitmap {
        if (!avatarUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(avatarUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap != null) {
                    return getCircledBitmap(originalBitmap, sizePx)
                }
            } catch (_: Exception) {}
        }

        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val palette = intArrayOf(
            Color.parseColor("#D4AF37"),
            Color.parseColor("#00BFA5"),
            Color.parseColor("#3B82F6"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#10B981"),
            Color.parseColor("#E11D48")
        )
        val colorIndex = Math.abs(name.hashCode()) % palette.size
        paint.color = palette[colorIndex]
        paint.style = Paint.Style.FILL

        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        val initial = name.trim().take(1).uppercase()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = sizePx * 0.46f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val yPos = (radius - ((textPaint.descent() + textPaint.ascent()) / 2f))
        canvas.drawText(initial, radius, yPos, textPaint)

        return output
    }

    private fun getCircledBitmap(bitmap: Bitmap, sizePx: Int): Bitmap {
        val scaled = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, sizePx, sizePx)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        paint.isAntiAlias = true
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, rect, rect, paint)
        return output
    }

    private const val GROUP_KEY_MESSAGES = "oozox_messages_group"
    private const val SUMMARY_NOTIFICATION_ID = 1000

    private data class QueuedNotificationMessage(
        val senderName: String,
        val text: String,
        val timestamp: Long
    )

    private val activeChatMessages = java.util.concurrent.ConcurrentHashMap<String, MutableList<QueuedNotificationMessage>>()

    fun clearChatNotification(context: Context, chatId: String) {
        if (chatId.isBlank()) return
        activeChatMessages.remove(chatId)
        try {
            NotificationManagerCompat.from(context).cancel(chatId.hashCode())
            if (activeChatMessages.isEmpty()) {
                NotificationManagerCompat.from(context).cancel(SUMMARY_NOTIFICATION_ID)
            }
        } catch (_: Exception) {}
    }

    private fun formatWhatsAppMessagePreview(context: Context, rawText: String): String {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return Translations.getFromContext(context, "notif.new_message")

        if (trimmed.startsWith("ENC_") || trimmed.startsWith("ENC_v")) {
            return Translations.getFromContext(context, "notif.encrypted_message")
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return Translations.getFromContext(context, "notif.operation_notice")
        }

        val lower = trimmed.lowercase()
        return when {
            lower.startsWith("[صورة]") || lower.startsWith("[image]") ||
                (lower.startsWith("content://") && (lower.contains("image") || lower.contains("photo"))) ||
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") ->
                Translations.getFromContext(context, "notif.preview_image")
            lower.startsWith("[فيديو]") || lower.startsWith("[video]") ||
                lower.endsWith(".mp4") || lower.endsWith(".mov") ->
                Translations.getFromContext(context, "notif.preview_video")
            lower.startsWith("[صوت]") || lower.startsWith("[رسالة صوتية]") || lower.startsWith("[audio]") || lower.startsWith("[voice]") ||
                lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".mp3") || lower.endsWith(".ogg") ->
                Translations.getFromContext(context, "notif.preview_voice")
            lower.startsWith("[مستند]") || lower.startsWith("[ملف]") || lower.startsWith("[file]") || lower.startsWith("[document]") ||
                lower.endsWith(".pdf") || lower.endsWith(".xlsx") || lower.endsWith(".docx") ->
                Translations.getFromContext(context, "notif.preview_doc")
            lower.startsWith("[موقع]") || lower.startsWith("[location]") ->
                Translations.getFromContext(context, "notif.preview_location")
            else -> trimmed
        }
    }

    fun showChatMessageNotification(
        context: Context,
        chatId: String = "",
        senderName: String,
        messageText: String,
        senderAvatarUri: String? = null,
        isSubAccount: Boolean = false,
        isSubAccountNotificationEnabled: Boolean = false,
        isGroup: Boolean = false,
        groupName: String? = null
    ) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)
        val safeSenderName = senderName.trim().ifBlank { "OOZOX" }
        val cleanPreview = formatWhatsAppMessagePreview(context, messageText)

        if (isSubAccount) {
            if (!isSubAccountNotificationEnabled) {
                return
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (chatId.isNotBlank()) {
                    putExtra(EXTRA_DESTINATION, DESTINATION_CHAT)
                    putExtra(EXTRA_CHAT_ID, chatId)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                if (chatId.isNotBlank()) ("sub_" + chatId).hashCode() else 1002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val isSoundEnabled = MessageNotificationSoundHelper.isSoundEnabled(context)
            val selectedToneId = MessageNotificationSoundHelper.getSelectedTone(context)
            val soundUri = if (isSoundEnabled) {
                if (selectedToneId == MessageNotificationSoundHelper.TONE_SYSTEM_DEFAULT) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                } else {
                    MessageNotificationSoundHelper.getToneUri(context, selectedToneId)
                }
            } else null

            MessageNotificationSoundHelper.playNewMessageSound(context)

            val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("OOZOX")
                .setContentText(Translations.get("notif.new_message", lang))
                .setColor(Color.parseColor("#D4AF37"))
                .setAutoCancel(true)
                .apply {
                    if (soundUri != null) {
                        setSound(soundUri)
                    } else {
                        setSilent(true)
                    }
                }
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build()

            notifySafe(context, if (chatId.isNotBlank()) ("sub_" + chatId).hashCode() else 1002, notification)
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (chatId.isNotBlank()) {
                putExtra(EXTRA_DESTINATION, DESTINATION_CHAT)
                putExtra(EXTRA_CHAT_ID, chatId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (chatId.isNotBlank()) chatId.hashCode() else System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (chatId.isNotBlank()) {
            MessengerBubbleManager.initialize(context.applicationContext)
            MessengerBubbleManager.showBubble(
                chatId = chatId,
                contactName = safeSenderName,
                contactAvatarUri = senderAvatarUri,
                messageText = cleanPreview,
                autoExpand = false
            )
        }

        val isSoundEnabled = MessageNotificationSoundHelper.isSoundEnabled(context)
        val selectedToneId = MessageNotificationSoundHelper.getSelectedTone(context)
        val isVibrationEnabled = MessageNotificationSoundHelper.isVibrationEnabled(context)
        val soundUri = if (isSoundEnabled) {
            if (selectedToneId == MessageNotificationSoundHelper.TONE_SYSTEM_DEFAULT) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                MessageNotificationSoundHelper.getToneUri(context, selectedToneId)
            }
        } else null

        MessageNotificationSoundHelper.playNewMessageSound(context)

        val avatarBitmap = try {
            createCircularAvatarBitmap(context, safeSenderName, senderAvatarUri)
        } catch (_: Exception) {
            null
        }

        val mePerson = Person.Builder()
            .setName(Translations.get("common.you", lang))
            .setKey("local_me")
            .build()

        val now = System.currentTimeMillis()
        val key = chatId.ifBlank { safeSenderName }
        val messageQueue = activeChatMessages.getOrPut(key) { mutableListOf() }
        synchronized(messageQueue) {
            messageQueue.add(QueuedNotificationMessage(safeSenderName, cleanPreview, now))
            if (messageQueue.size > 5) {
                messageQueue.removeAt(0)
            }
        }

        val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
            .setGroupConversation(isGroup)
        if (isGroup) {
            messagingStyle.setConversationTitle(groupName?.takeIf { it.isNotBlank() } ?: safeSenderName)
        }

        val snapshot = synchronized(messageQueue) { messageQueue.toList() }
        snapshot.forEach { queued ->
            val msgPersonBuilder = Person.Builder()
                .setName(queued.senderName)
                .setKey(if (chatId.isNotBlank()) "${chatId}_${queued.senderName}" else queued.senderName)
                .setBot(chatId.contains("ai") || chatId.contains("bot"))
            if (avatarBitmap != null && queued.senderName == safeSenderName) {
                msgPersonBuilder.setIcon(IconCompat.createWithBitmap(avatarBitmap))
            }
            messagingStyle.addMessage(queued.text, queued.timestamp, msgPersonBuilder.build())
        }

        val replyLabel = Translations.get("common.reply", lang)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val replyPendingIntent = PendingIntent.getActivity(
            context,
            if (chatId.isNotBlank()) ("reply_" + chatId).hashCode() else 1009,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            replyLabel,
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        var defaultFlags = NotificationCompat.DEFAULT_LIGHTS
        if (isVibrationEnabled) {
            defaultFlags = defaultFlags or NotificationCompat.DEFAULT_VIBRATE
        }

        val displayTitle = if (isGroup && !groupName.isNullOrBlank()) groupName else safeSenderName
        val collapsedText = if (isGroup) "$safeSenderName: $cleanPreview" else cleanPreview

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(displayTitle)
            .setContentText(collapsedText)
            .setStyle(messagingStyle)
            .setColor(Color.parseColor("#D4AF37"))
            .setWhen(now)
            .setShowWhen(true)
            .setNumber(snapshot.size)
            .setGroup(GROUP_KEY_MESSAGES)
            .setAutoCancel(true)
            .apply {
                if (soundUri != null) {
                    setSound(soundUri)
                } else {
                    setSilent(true)
                }
            }
            .setDefaults(defaultFlags)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .addAction(replyAction)

        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }

        val notificationId = if (chatId.isNotBlank()) chatId.hashCode() else 1001
        notifySafe(context, notificationId, builder.build())

        if (activeChatMessages.size > 1) {
            val totalMessages = activeChatMessages.values.sumOf { it.size }
            val totalChats = activeChatMessages.size
            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("OOZOX")
                .setContentText(
                    Translations.format(
                        "notif.summary_fmt",
                        lang,
                        "messages" to totalMessages,
                        "chats" to totalChats
                    )
                )
                .setColor(Color.parseColor("#D4AF37"))
                .setGroup(GROUP_KEY_MESSAGES)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setSilent(true)
                .build()
            notifySafe(context, SUMMARY_NOTIFICATION_ID, summaryNotification)
        }

        com.example.ui.components.ToastNotificationBus.showNewMessage(
            senderName = displayTitle,
            messageText = collapsedText
        )
    }

    fun showChatMessageNotification(
        context: Context,
        senderName: String,
        messageText: String
    ) {
        showChatMessageNotification(
            context = context,
            chatId = "",
            senderName = senderName,
            messageText = messageText,
            senderAvatarUri = null,
            isSubAccount = false,
            isSubAccountNotificationEnabled = false
        )
    }

    fun showProjectUpdateNotification(
        context: Context,
        projectId: String = "",
        projectName: String,
        title: String = "",
        updateMessage: String
    ) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)
        val resolvedTitle = title.ifBlank { Translations.get("notif.proj_default_title", lang) }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (projectId.isNotBlank()) {
                putExtra(EXTRA_DESTINATION, DESTINATION_PROJECT)
                putExtra(EXTRA_PROJECT_ID, projectId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (projectId.isNotBlank()) projectId.hashCode() else System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_PROJECTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(
                Translations.format(
                    "notif.proj_update_title_fmt",
                    lang,
                    "project" to projectName,
                    "title" to resolvedTitle
                )
            )
            .setContentText(updateMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(
                        Translations.format("notif.proj_big_title_fmt", lang, "project" to projectName)
                    )
                    .setSummaryText(Translations.get("notif.proj_summary", lang))
                    .bigText(updateMessage)
            )
            .setColor(Color.parseColor("#00BFA5"))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(
            context,
            ("proj_" + (if (projectId.isNotBlank()) projectId else System.currentTimeMillis().toString())).hashCode(),
            notification
        )

        com.example.ui.components.ToastNotificationBus.showProjectUpdate(
            projectName = projectName,
            updateDescription = "$resolvedTitle: $updateMessage"
        )
    }

    fun showProjectUpdateNotification(
        context: Context,
        projectName: String,
        updateTitle: String,
        updateDetails: String
    ) {
        showProjectUpdateNotification(
            context = context,
            projectId = "",
            projectName = projectName,
            title = updateTitle,
            updateMessage = updateDetails
        )
    }

    fun showProjectUpdateNotification(
        context: Context,
        projectName: String,
        updateMessage: String
    ) {
        val simpleTitle = Translations.getFromContext(context, "notif.proj_simple_title")
        showProjectUpdateNotification(context, "", projectName, simpleTitle, updateMessage)
    }

    fun showInvoiceStatusNotification(
        context: Context,
        invoiceId: String = "",
        invoiceNumber: String,
        status: String,
        totalAmount: Double,
        counterpartName: String? = null
    ) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_TRANSACTION)
            if (invoiceId.isNotBlank()) {
                putExtra(EXTRA_TRANSACTION_ID, invoiceId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (invoiceId.isNotBlank()) invoiceId.hashCode() else System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusLabel = when (status.uppercase()) {
            "PAID" -> Translations.get("notif.inv_paid", lang)
            "OVERDUE" -> Translations.get("notif.inv_overdue", lang)
            "CANCELLED" -> Translations.get("notif.inv_cancelled", lang)
            "PENDING" -> Translations.get("notif.inv_pending", lang)
            else -> status
        }

        val statusColor = when (status.uppercase()) {
            "PAID" -> Color.parseColor("#10B981")
            "OVERDUE" -> Color.parseColor("#EF4444")
            "CANCELLED" -> Color.parseColor("#6B7280")
            else -> Color.parseColor("#FFD700")
        }

        val formattedAmount = GoldUtils.formatCurrency(totalAmount, lang)
        val title = Translations.format("notif.inv_title_fmt", lang, "num" to invoiceNumber, "status" to statusLabel)
        val body = buildString {
            append(Translations.format("notif.inv_body_fmt", lang, "amount" to formattedAmount, "status" to statusLabel))
            if (!counterpartName.isNullOrBlank()) {
                append("\n")
                append(Translations.format("notif.inv_counterpart_fmt", lang, "name" to counterpartName))
            }
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_DEALS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(
                Translations.format("notif.inv_short_fmt", lang, "amount" to formattedAmount, "status" to statusLabel)
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(Translations.format("notif.inv_big_title_fmt", lang, "num" to invoiceNumber))
                    .setSummaryText(Translations.get("notif.inv_summary", lang))
                    .bigText(body)
            )
            .setColor(statusColor)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(
            context,
            ("inv_" + (if (invoiceId.isNotBlank()) invoiceId else System.currentTimeMillis().toString())).hashCode(),
            notification
        )
    }

    fun showGoldDealNotification(
        context: Context,
        transactionId: String = "",
        title: String,
        message: String
    ) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (transactionId.isNotBlank()) {
                putExtra(EXTRA_DESTINATION, DESTINATION_TRANSACTION)
                putExtra(EXTRA_TRANSACTION_ID, transactionId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (transactionId.isNotBlank()) transactionId.hashCode() else System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_DEALS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(Translations.format("notif.deal_title_fmt", lang, "title" to title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSubText(Translations.get("notif.gold_subtext", lang))
            .setColor(Color.parseColor("#FFD700"))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(
            context,
            ("deal_" + (if (transactionId.isNotBlank()) transactionId else System.currentTimeMillis().toString())).hashCode(),
            notification
        )
    }

    fun showTransactionNotification(
        context: Context,
        title: String,
        message: String = "",
        body: String = message
    ) {
        showGoldDealNotification(context, "", title, if (body.isNotBlank()) body else message)
    }

    fun showGoldPriceThresholdNotification(
        context: Context,
        alertId: String,
        metalType: String,
        karat: String,
        targetPrice: Double,
        currentPrice: Double,
        direction: String,
        note: String? = null
    ) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_GOLD)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ("price_alert_" + alertId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val metalLabel = if (metalType.equals("SILVER", ignoreCase = true) || metalType == "فضة") {
            Translations.get("notif.metal_silver", lang)
        } else {
            Translations.get("notif.metal_gold", lang)
        }
        val karatLabel = when (karat.uppercase()) {
            "24", "KARAT_24" -> GoldKarat.KARAT_24.localizedLabel(lang)
            "21", "KARAT_21" -> GoldKarat.KARAT_21.localizedLabel(lang)
            "18", "KARAT_18" -> GoldKarat.KARAT_18.localizedLabel(lang)
            "14", "KARAT_14" -> GoldKarat.KARAT_14.localizedLabel(lang)
            "POUND", "GOLD_POUND" -> Translations.get("notif.gold_pound", lang)
            "OUNCE", "OUNCE_USD" -> Translations.get("notif.gold_ounce", lang)
            "999", "SILVER_999" -> Translations.get("silver.karat_999", lang)
            "925", "SILVER_925" -> Translations.get("silver.karat_925", lang)
            "800", "SILVER_800" -> Translations.get("silver.karat_800", lang)
            else -> karat
        }

        val directionLabel = if (direction.equals("ABOVE", ignoreCase = true) || direction == "أعلى") {
            Translations.get("notif.dir_above", lang)
        } else {
            Translations.get("notif.dir_below", lang)
        }
        val title = Translations.format("notif.price_title_fmt", lang, "metal" to metalLabel, "karat" to karatLabel)
        val targetFormatted = GoldUtils.formatCurrency(targetPrice, lang)
        val currentFormatted = GoldUtils.formatCurrency(currentPrice, lang)

        val bodyText = buildString {
            append(
                Translations.format(
                    "notif.price_body_fmt",
                    lang,
                    "current" to currentFormatted,
                    "dir" to directionLabel,
                    "target" to targetFormatted
                )
            )
            if (!note.isNullOrBlank()) {
                append("\n")
                append(Translations.format("notif.price_note_fmt", lang, "note" to note))
            }
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_PRICE_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(
                Translations.format("notif.price_short_fmt", lang, "current" to currentFormatted, "target" to targetFormatted)
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(Translations.get("notif.price_summary", lang))
                    .bigText(bodyText)
            )
            .setColor(Color.parseColor("#FFD700"))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("alert_" + alertId).hashCode(), notification)
    }

    fun showSystemNotification(
        context: Context,
        title: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(Color.parseColor("#00BFA5"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, System.currentTimeMillis().toInt(), notification)
    }

    fun showFinancialDeficitNotification(
        context: Context,
        groupName: String,
        staffName: String,
        deficitAmount: Double,
        entryDate: String,
        notes: String? = null
    ) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "company_accounting_hub")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ("deficit_" + System.currentTimeMillis()).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = GoldUtils.formatCurrency(deficitAmount, lang)
        val title = Translations.format("notif.deficit_title_fmt", lang, "amount" to formattedAmount)
        val body = buildString {
            append(
                Translations.format(
                    "notif.deficit_body_fmt",
                    lang,
                    "amount" to formattedAmount,
                    "group" to groupName,
                    "staff" to staffName,
                    "date" to entryDate
                )
            )
            if (!notes.isNullOrBlank()) {
                append("\n")
                append(Translations.format("notif.notes_fmt", lang, "notes" to notes))
            }
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_FINANCIAL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(
                Translations.format(
                    "notif.deficit_short_fmt",
                    lang,
                    "amount" to formattedAmount,
                    "staff" to staffName,
                    "date" to entryDate
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(Translations.get("notif.deficit_summary", lang))
                    .bigText(body)
            )
            .setColor(Color.parseColor("#EF4444"))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("deficit_" + System.currentTimeMillis()).hashCode(), notification)
    }

    fun showFinancialSurplusNotification(
        context: Context,
        groupName: String,
        staffName: String,
        surplusAmount: Double,
        entryDate: String,
        notes: String? = null
    ) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "company_accounting_hub")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ("surplus_" + System.currentTimeMillis()).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = GoldUtils.formatCurrency(surplusAmount, lang)
        val title = Translations.format("notif.surplus_title_fmt", lang, "amount" to formattedAmount)
        val body = buildString {
            append(
                Translations.format(
                    "notif.surplus_body_fmt",
                    lang,
                    "amount" to formattedAmount,
                    "group" to groupName,
                    "staff" to staffName,
                    "date" to entryDate
                )
            )
            if (!notes.isNullOrBlank()) {
                append("\n")
                append(Translations.format("notif.notes_fmt", lang, "notes" to notes))
            }
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_FINANCIAL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(
                Translations.format("notif.surplus_short_fmt", lang, "amount" to formattedAmount, "date" to entryDate)
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(Translations.get("notif.surplus_summary", lang))
                    .bigText(body)
            )
            .setColor(Color.parseColor("#10B981"))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("surplus_" + System.currentTimeMillis()).hashCode(), notification)
    }

    fun showRetroactiveComplaintDeficitNotification(
        context: Context,
        groupName: String,
        staffName: String,
        complaintTitle: String,
        targetDate: String,
        adjustmentAmount: Double,
        netDayDeficit: Double
    ) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "company_accounting_hub")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ("retro_deficit_" + System.currentTimeMillis()).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAdj = GoldUtils.formatCurrency(adjustmentAmount, lang)
        val formattedDeficit = GoldUtils.formatCurrency(netDayDeficit, lang)
        val title = Translations.format("notif.retro_title_fmt", lang, "amount" to formattedDeficit)
        val body = Translations.format(
            "notif.retro_body_fmt",
            lang,
            "staff" to staffName,
            "adj" to formattedAdj,
            "date" to targetDate,
            "title" to complaintTitle,
            "deficit" to formattedDeficit,
            "group" to groupName
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_FINANCIAL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(
                Translations.format("notif.retro_short_fmt", lang, "amount" to formattedDeficit, "date" to targetDate)
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(Translations.get("notif.retro_summary", lang))
                    .bigText(body)
            )
            .setColor(Color.parseColor("#DC2626"))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("retro_deficit_" + System.currentTimeMillis()).hashCode(), notification)
    }

    fun showStaffGeofenceWarning(context: Context, staffName: String) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCING)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(Translations.get("notif.geofence_staff_title", lang))
            .setContentText(Translations.format("notif.geofence_staff_body_fmt", lang, "staff" to staffName))
            .setColor(Color.RED)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
        notifySafe(context, 2001, notification)
    }

    fun showManagerGeofenceAlert(context: Context, staffName: String, durationMins: Int) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "company_accounting_hub")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2002, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCING)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(Translations.get("notif.geofence_mgr_title", lang))
            .setContentText(
                Translations.format("notif.geofence_mgr_body_fmt", lang, "staff" to staffName, "mins" to durationMins)
            )
            .setColor(Color.parseColor("#DC2626"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
        notifySafe(context, 2002, notification)
    }

    fun showManagerGpsDisabledAlert(context: Context, staffName: String, issueDetail: String = "") {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)
        val resolvedDetail = issueDetail.ifBlank { Translations.get("notif.gps_issue_default", lang) }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "company_accounting_hub")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2003, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCING)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(Translations.get("notif.gps_disabled_title", lang))
            .setContentText(
                Translations.format(
                    "notif.gps_disabled_body_fmt",
                    lang,
                    "staff" to staffName,
                    "detail" to resolvedDetail
                )
            )
            .setColor(Color.parseColor("#DC2626"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
        notifySafe(context, 2003, notification)
    }

    fun showShiftHandoverReceivedAlert(context: Context, senderName: String, receiverName: String, totalCash: Double) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "business_management")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2004, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(Translations.get("notif.shift_handover_title", lang))
            .setContentText(
                Translations.format(
                    "notif.shift_handover_body_fmt",
                    lang,
                    "receiver" to receiverName,
                    "amount" to GoldUtils.formatCurrency(totalCash, lang),
                    "sender" to senderName
                )
            )
            .setColor(Color.parseColor("#10B981"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()
        notifySafe(context, 2004, notification)
    }

    fun showForensicLiabilityAlert(
        context: Context,
        complaintTitle: String,
        invoiceNo: String,
        primaryName: String,
        receiverName: String,
        liabilityType: String
    ) {
        val lang = Translations.activeLanguage(context)
        Translations.ensureLoaded(context, lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "business_management")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2005, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_FINANCIAL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(Translations.get("notif.forensic_title", lang))
            .setContentText(
                Translations.format(
                    "notif.forensic_body_fmt",
                    lang,
                    "invoice" to invoiceNo,
                    "receiver" to receiverName,
                    "primary" to primaryName,
                    "type" to liabilityType
                )
            )
            .setColor(Color.parseColor("#DC2626"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
        notifySafe(context, 2005, notification)
    }

    private fun notifySafe(context: Context, id: Int, notification: android.app.Notification) {
        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManagerCompat.notify(id, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
