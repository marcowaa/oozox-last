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
     * Initializes Android 8.0+ Notification Channels with vibration, sounds, and LED badges.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: Real-time Messages & Group Chats
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "رسائل المحادثات المباشرة ومجموعات العمل",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات فورية عند استلام رسائل جديدة أو إشارات في المحادثات"
                enableLights(true)
                lightColor = Color.parseColor("#00BFA5")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setShowBadge(true)
            }

            // Channel 2: Project Updates & Budget Alerts
            val projectsChannel = NotificationChannel(
                CHANNEL_PROJECTS,
                "تحديثات المشاريع والميزانيات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند اعتماد مصروف، تعديل ميزانية، أو تعيين مهام جديدة"
                enableLights(true)
                lightColor = Color.parseColor("#00BFA5")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
            }

            // Channel 3: Gold Deals & Invoices
            val dealsChannel = NotificationChannel(
                CHANNEL_DEALS,
                "صفقات الذهب وكشوفات الحسابات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات صفقات الذهب وتأكيدات الصاغة والتجار"
                enableLights(true)
                lightColor = Color.parseColor("#FFD700")
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 4: Real-time Custom Gold Price Threshold Alerts
            val priceAlertsChannel = NotificationChannel(
                CHANNEL_PRICE_ALERTS,
                "تنبيهات أسعار الذهب الفورية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية لحظة وصول سعر الذهب أو الفضة للهدف المحدد مسبقاً"
                enableLights(true)
                lightColor = Color.parseColor("#FFD700")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
                setShowBadge(true)
            }

            // Channel 5: System Announcements
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "إعلانات النظام والاشتراكات",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات عامة حول التطبيق وحالة الاشتراك"
                setShowBadge(false)
            }

            // Channel 6: Financial Alerts & Deficit / Surplus Auditing
            val financialChannel = NotificationChannel(
                CHANNEL_FINANCIAL_ALERTS,
                "تنبيهات العجز والزيادة والشكاوى المالية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات عاجلة للإدارة والمدير عند تسجيل عجز مالي، زيادة غير معتادة، أو شكاوى تؤثر بأثر رجعي"
                enableLights(true)
                lightColor = Color.parseColor("#EF4444")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setShowBadge(true)
            }

            val geofencingChannel = NotificationChannel(
                CHANNEL_GEOFENCING,
                "تنبيهات الموقع الجغرافي والدوام",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الموظفين والمديرين عند مغادرة مقر العمل أثناء الدوام"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, projectsChannel, dealsChannel, priceAlertsChannel, systemChannel, financialChannel, geofencingChannel)
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

        // Generate a crisp, elegant letter avatar with premium colors
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val palette = intArrayOf(
            Color.parseColor("#D4AF37"), // Gold Primary
            Color.parseColor("#00BFA5"), // Teal
            Color.parseColor("#3B82F6"), // Blue
            Color.parseColor("#8B5CF6"), // Purple
            Color.parseColor("#10B981"), // Emerald
            Color.parseColor("#E11D48")  // Rose
        )
        val colorIndex = Math.abs(name.hashCode()) % palette.size
        paint.color = palette[colorIndex]
        paint.style = Paint.Style.FILL

        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        // Draw initial letter with high contrast
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

    /**
     * Clears stacked notification messages for a chat when the user opens it.
     */
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

    /**
     * Formats message content into a clean, human-readable WhatsApp-style notification line.
     * Prevents encrypted payloads, raw JSON, or file URIs from leaking into notifications.
     */
    private fun formatWhatsAppMessagePreview(rawText: String): String {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return "رسالة جديدة"

        // Never expose raw ciphertext or JSON payloads in notification shade
        if (trimmed.startsWith("ENC_") || trimmed.startsWith("ENC_v")) {
            return "رسالة مشفرة جديدة"
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return "إشعار عملية جديدة"
        }

        val lower = trimmed.lowercase()
        return when {
            lower.startsWith("[صورة]") || lower.startsWith("[image]") ||
                (lower.startsWith("content://") && (lower.contains("image") || lower.contains("photo"))) ||
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") ->
                "📷 صورة"
            lower.startsWith("[فيديو]") || lower.startsWith("[video]") ||
                lower.endsWith(".mp4") || lower.endsWith(".mov") ->
                "🎥 فيديو"
            lower.startsWith("[صوت]") || lower.startsWith("[رسالة صوتية]") || lower.startsWith("[audio]") || lower.startsWith("[voice]") ||
                lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".mp3") || lower.endsWith(".ogg") ->
                "🎤 رسالة صوتية"
            lower.startsWith("[مستند]") || lower.startsWith("[ملف]") || lower.startsWith("[file]") || lower.startsWith("[document]") ||
                lower.endsWith(".pdf") || lower.endsWith(".xlsx") || lower.endsWith(".docx") ->
                "📄 مستند"
            lower.startsWith("[موقع]") || lower.startsWith("[location]") ->
                "📍 موقع"
            else -> trimmed
        }
    }

    /**
     * Dispatches a WhatsApp-style notification for a newly received chat message.
     * - 1-on-1 Chat: Title = Sender Name, Body = Exact Message Text (stacked if multiple).
     * - Group Chat: Title = Group Name, Body = "Sender Name: Message Text".
     * - Sub-Accounts: Discreet notification without sender name or message content.
     */
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
        val safeSenderName = senderName.trim().ifBlank { "OOZOX" }
        val cleanPreview = formatWhatsAppMessagePreview(messageText)

        // Case 1: Sub-Account (الحسابات الفرعية)
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
                .setContentText("رسالة جديدة")
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

        // Case 2: Main Account (الحساب الرئيسي) — WhatsApp MessagingStyle
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

        // Link incoming message notification directly to the floating bubble user heads
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

        // Local user ("أنت") must be the MessagingStyle root user; otherwise Android renders the sender as "You"
        val mePerson = Person.Builder()
            .setName("أنت")
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

        // Quick Direct Reply Action (WhatsApp style)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("رد")
            .build()

        val replyPendingIntent = PendingIntent.getActivity(
            context,
            if (chatId.isNotBlank()) ("reply_" + chatId).hashCode() else 1009,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "رد",
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

        // WhatsApp-style Group Summary when multiple chats have unread notifications
        if (activeChatMessages.size > 1) {
            val totalMessages = activeChatMessages.values.sumOf { it.size }
            val totalChats = activeChatMessages.size
            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("OOZOX")
                .setContentText("$totalMessages رسائل من $totalChats محادثات")
                .setColor(Color.parseColor("#D4AF37"))
                .setGroup(GROUP_KEY_MESSAGES)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setSilent(true)
                .build()
            notifySafe(context, SUMMARY_NOTIFICATION_ID, summaryNotification)
        }

        // Dispatch clean in-app toast notification alert
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

    /**
     * Dispatches a notification for a project update, budget burn warning, or task assignment.
     */
    fun showProjectUpdateNotification(
        context: Context,
        projectId: String = "",
        projectName: String,
        title: String = "تحديث في المشروع",
        updateMessage: String
    ) {
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
            .setContentTitle("🏗️ $projectName: $title")
            .setContentText(updateMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🏗️ $projectName")
                    .setSummaryText("تحديث مشروع")
                    .bigText(updateMessage)
            )
            .setColor(Color.parseColor("#00BFA5"))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("proj_" + (if (projectId.isNotBlank()) projectId else System.currentTimeMillis().toString())).hashCode(), notification)

        // Dispatch simple in-app toast notification alert
        com.example.ui.components.ToastNotificationBus.showProjectUpdate(
            projectName = projectName,
            updateDescription = "$title: $updateMessage"
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
        showProjectUpdateNotification(context, "", projectName, "تحديث", updateMessage)
    }

    /**
     * Dispatches a notification specifically when an invoice status changes (PAID, OVERDUE, CANCELLED, PENDING).
     */
    fun showInvoiceStatusNotification(
        context: Context,
        invoiceId: String = "",
        invoiceNumber: String,
        status: String,
        totalAmount: Double,
        counterpartName: String? = null
    ) {
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
            "PAID" -> "✅ مسددة بالكامل"
            "OVERDUE" -> "⚠️ متأخرة عن موعد السداد"
            "CANCELLED" -> "❌ ملغاة"
            "PENDING" -> "⏳ قيد الانتظار"
            else -> status
        }

        val statusColor = when (status.uppercase()) {
            "PAID" -> Color.parseColor("#10B981")
            "OVERDUE" -> Color.parseColor("#EF4444")
            "CANCELLED" -> Color.parseColor("#6B7280")
            else -> Color.parseColor("#FFD700")
        }

        val formattedAmount = String.format(java.util.Locale.US, "%,.2f", totalAmount)
        val title = "📄 تحديث فاتورة $invoiceNumber: $statusLabel"
        val body = StringBuilder().apply {
            append("تم تحديث حالة الفاتورة بقيمة $formattedAmount ج.م إلى: $statusLabel.")
            if (!counterpartName.isNullOrBlank()) {
                append("\nالطرف الآخر: $counterpartName")
            }
        }.toString()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_DEALS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("القيمة: $formattedAmount ج.م | الحالة: $statusLabel")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("📄 فاتورة $invoiceNumber")
                    .setSummaryText("حالة الفاتورة")
                    .bigText(body)
            )
            .setColor(statusColor)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("inv_" + (if (invoiceId.isNotBlank()) invoiceId else System.currentTimeMillis().toString())).hashCode(), notification)
    }

    /**
     * Dispatches a notification for a gold deal or invoice update.
     */
    fun showGoldDealNotification(
        context: Context,
        transactionId: String = "",
        title: String,
        message: String
    ) {
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
            .setContentTitle("⚡ $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSubText("صفقات الذهب")
            .setColor(Color.parseColor("#FFD700"))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("deal_" + (if (transactionId.isNotBlank()) transactionId else System.currentTimeMillis().toString())).hashCode(), notification)
    }

    fun showTransactionNotification(
        context: Context,
        title: String,
        message: String = "",
        body: String = message
    ) {
        showGoldDealNotification(context, "", title, if (body.isNotBlank()) body else message)
    }

    /**
     * Dispatches a high-priority push notification when the gold/silver market hits a user-defined price threshold.
     */
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

        val metalLabel = if (metalType == "SILVER" || metalType == "فضة") "الفضة" else "الذهب"
        val karatLabel = when (karat.uppercase()) {
            "24", "KARAT_24" -> "عيار 24"
            "21", "KARAT_21" -> "عيار 21"
            "18", "KARAT_18" -> "عيار 18"
            "14", "KARAT_14" -> "عيار 14"
            "POUND", "GOLD_POUND" -> "الجنيه الذهب"
            "OUNCE", "OUNCE_USD" -> "الأونصة العالمية"
            "999", "SILVER_999" -> "فضة 999"
            "925", "SILVER_925" -> "فضة 925"
            "800", "SILVER_800" -> "فضة 800"
            else -> karat
        }

        val directionIcon = if (direction == "ABOVE" || direction == "أعلى") "📈 صعود" else "📉 هبوط"
        val title = "🎯 تنبيه سعر فوري: $metalLabel $karatLabel"
        val targetFormatted = String.format(java.util.Locale.US, "%,.0f", targetPrice)
        val currentFormatted = String.format(java.util.Locale.US, "%,.0f", currentPrice)

        val bodyText = StringBuilder().apply {
            append("وصل السعر اللحظي في السوق إلى $currentFormatted ج.م ")
            append("(${directionIcon} وتجاوز هدفك المحدد $targetFormatted ج.م).")
            if (!note.isNullOrBlank()) {
                append("\nملاحظتك: $note")
            }
        }.toString()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_PRICE_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("السعر الحالي: $currentFormatted ج.م (الهدف: $targetFormatted ج.م)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText("تنبيه أهداف الذهب")
                    .bigText(bodyText)
            )
            .setColor(Color.parseColor("#FFD700")) // Gold
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("alert_" + alertId).hashCode(), notification)
    }

    /**
     * Dispatches a general system announcement notification.
     */
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

    /**
     * Dispatches an urgent notification to the manager when a financial deficit is recorded.
     */
    fun showFinancialDeficitNotification(
        context: Context,
        groupName: String,
        staffName: String,
        deficitAmount: Double,
        entryDate: String,
        notes: String? = null
    ) {
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

        val formattedAmount = String.format(java.util.Locale.US, "%,.2f", deficitAmount)
        val title = "🚨 تنبيه عجز مالي بالخزينة: $formattedAmount ج.م"
        val body = StringBuilder().apply {
            append("تم تسجيل عجز مالي بقيمة $formattedAmount ج.م في [$groupName].\n")
            append("الموظف المسؤول: $staffName | التاريخ: $entryDate")
            if (!notes.isNullOrBlank()) {
                append("\nالبيان: $notes")
            }
        }.toString()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_FINANCIAL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("عجز مالي $formattedAmount ج.م مسجل بواسطة $staffName في $entryDate")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText("رقابة الخزينة والعهدة")
                    .bigText(body)
            )
            .setColor(Color.parseColor("#EF4444")) // Red
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("deficit_" + System.currentTimeMillis()).hashCode(), notification)
    }

    /**
     * Dispatches a notification to the manager when a notable financial surplus/excess is recorded.
     */
    fun showFinancialSurplusNotification(
        context: Context,
        groupName: String,
        staffName: String,
        surplusAmount: Double,
        entryDate: String,
        notes: String? = null
    ) {
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

        val formattedAmount = String.format(java.util.Locale.US, "%,.2f", surplusAmount)
        val title = "💰 تنبيه زيادة مالية / إيراد مميز: $formattedAmount ج.م"
        val body = StringBuilder().apply {
            append("تم تسجيل تدفق مالي / فائض بقيمة $formattedAmount ج.م في [$groupName].\n")
            append("الموظف: $staffName | التاريخ: $entryDate")
            if (!notes.isNullOrBlank()) {
                append("\nالبيان: $notes")
            }
        }.toString()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_FINANCIAL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("زيادة مالية مسجلة بقيمة $formattedAmount ج.م في $entryDate")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText("رقابة التدفقات النقدية")
                    .bigText(body)
            )
            .setColor(Color.parseColor("#10B981")) // Green
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("surplus_" + System.currentTimeMillis()).hashCode(), notification)
    }

    /**
     * Dispatches an urgent alert to the manager when a retroactive employee complaint/adjustment results in a deficit on that target day.
     */
    fun showRetroactiveComplaintDeficitNotification(
        context: Context,
        groupName: String,
        staffName: String,
        complaintTitle: String,
        targetDate: String,
        adjustmentAmount: Double,
        netDayDeficit: Double
    ) {
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

        val formattedAdj = String.format(java.util.Locale.US, "%,.2f", adjustmentAmount)
        val formattedDeficit = String.format(java.util.Locale.US, "%,.2f", netDayDeficit)
        val title = "⚠️ إنذار إداري: عجز ناتج عن شكوى بأثر رجعي ($formattedDeficit ج.م)"
        val body = StringBuilder().apply {
            append("أدرج الموظف [$staffName] شكوى/تسوية مالية بقيمة $formattedAdj ج.م أثرت بأثر رجعي على يوم [$targetDate].\n")
            append("عنوان الشكوى: $complaintTitle\n")
            append("صافي عجز الخزينة المحسوب لذلك اليوم: $formattedDeficit ج.م في [$groupName].\n")
            append("يرجى فحص المعاملات المسببة للعجز واتخاذ الإجراء اللازم.")
        }.toString()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_FINANCIAL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("شكوى بأثر رجعي أحدثت عجزاً ($formattedDeficit ج.م) بتاريخ $targetDate")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText("متابعة الشكاوى المالية والعجز")
                    .bigText(body)
            )
            .setColor(Color.parseColor("#DC2626")) // Urgent Red
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()

        notifySafe(context, ("retro_deficit_" + System.currentTimeMillis()).hashCode(), notification)
    }

    fun showStaffGeofenceWarning(context: Context, staffName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCING)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ تنبيه: خارج نطاق العمل")
            .setContentText("يا $staffName، يرجى العودة إلى مقر العمل لتجنب تسجيل مخالفة إدارية.")
            .setColor(Color.RED)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
        notifySafe(context, 2001, notification)
    }

    fun showManagerGeofenceAlert(context: Context, staffName: String, durationMins: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "company_accounting_hub")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2002, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCING)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("🚨 إنذار إداري: انقطاع موظف")
            .setContentText("الموظف $staffName متواجد خارج مقر العمل منذ $durationMins دقيقة.")
            .setColor(Color.parseColor("#DC2626"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
        notifySafe(context, 2002, notification)
    }

    fun showManagerGpsDisabledAlert(context: Context, staffName: String, issueDetail: String = "تم إيقاف خدمة الموقع GPS أو تعذر تحديد النطاق") {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "company_accounting_hub")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2003, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCING)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("🚨 تحذير: إيقاف موقع العمل للموظف")
            .setContentText("الموظف $staffName: $issueDetail. تم إشعار الإدارة تلقائياً.")
            .setColor(Color.parseColor("#DC2626"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
        notifySafe(context, 2003, notification)
    }

    fun showShiftHandoverReceivedAlert(context: Context, senderName: String, receiverName: String, totalCash: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "business_management")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2004, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🤝 تأكيد استلام الشفت والعهدة")
            .setContentText("أكد $receiverName استلام الخزينة (${totalCash.toInt()} ج.م) والبضاعة والفواتير من $senderName.")
            .setColor(Color.parseColor("#10B981"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()
        notifySafe(context, 2004, notification)
    }

    fun showForensicLiabilityAlert(context: Context, complaintTitle: String, invoiceNo: String, primaryName: String, receiverName: String, liabilityType: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "business_management")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2005, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_FINANCIAL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚖️ تحليل الذكاء المالي: مسؤولية مشتركة")
            .setContentText("شكوى فاتورة #$invoiceNo: تم فحص إقرار الاستلام لـ $receiverName و $primaryName ($liabilityType).")
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
