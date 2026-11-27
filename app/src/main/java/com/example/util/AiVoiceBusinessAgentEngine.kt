package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.*
import com.example.data.repository.GoldChatRepository
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

/**
 * Supported Action Types that the AI Autonomous Voice Agent can execute.
 */
enum class AiActionType(val displayName: String, val badgeColorHex: Long) {
    DEDUCT_BALANCE("خصم من الرصيد وهاشتاج", 0xFFEF4444),
    ADD_BALANCE("إضافة للرصيد وهاشتاج", 0xFF10B981),
    SEND_HASHTAG_MESSAGE("إرسال رسالة وهاشتاج", 0xFF3B82F6),
    CREATE_ACCOUNT("إدراج / إضافة حساب جديد", 0xFF8B5CF6),
    ADD_GOLD_TRANSACTION("تسجيل حركة ذهب / بضاعة", 0xFFF59E0B),
    GENERAL_CONSULTATION("استفسار واستشارة مالية", 0xFF6366F1)
}

/**
 * Structured Action descriptor extracted from the user's spoken or written command.
 */
data class AiBusinessAction(
    val type: AiActionType,
    val targetContactName: String,
    val amount: Double = 0.0,
    val weightGrams: Double = 0.0,
    val karat: Int? = null,
    val unitPrice: Double? = null,
    val suggestedHashtag: String = "",
    val customDataOrNotes: String = "",
    val confirmationQuestionSpeech: String = "",
    val executionConfirmationSpeech: String = "",
    val cancellationSpeech: String = "تم إلغاء تنفيذ العملية بناءً على طلبك.",
    val rawUserPrompt: String = "",
    val requiresConfirmation: Boolean = true,
    val targetPhoneNumber: String? = null
)

/**
 * Preferences Manager for AI Voice Business Action Agent
 */
object AiVoiceAgentPreferences {
    private const val PREFS_NAME = "ai_voice_agent_settings"
    private const val KEY_VOICE_AGENT_ENABLED = "key_voice_agent_enabled"
    private const val KEY_SPEECH_CONFIRMATION_ENABLED = "key_speech_confirmation_enabled"
    private const val KEY_AUTO_LISTEN_CONFIRMATION = "key_auto_listen_confirmation"

    fun isVoiceAgentEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VOICE_AGENT_ENABLED, true)
    }

    fun setVoiceAgentEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VOICE_AGENT_ENABLED, enabled).apply()
    }

    fun isSpeechConfirmationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SPEECH_CONFIRMATION_ENABLED, true)
    }

    fun setSpeechConfirmationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SPEECH_CONFIRMATION_ENABLED, enabled).apply()
    }

    fun isAutoListenConfirmationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_LISTEN_CONFIRMATION, true)
    }

    fun setAutoListenConfirmationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_LISTEN_CONFIRMATION, enabled).apply()
    }
}

/**
 * Intelligent AI Engine for autonomous conversational business execution.
 */
object AiVoiceBusinessAgentEngine {

    private const val TAG = "AiVoiceBusinessAgent"

    /**
     * Checks if spoken reply is affirmative (Yes, Confirm, Execute, OK).
     */
    fun isConfirmationAffirmative(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.getDefault())
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ة", "ه")
        val keywords = listOf(
            "نعم", "تاكيد", "اكيد", "نفذ", "موافق", "تمام", "ايوة", "ايوه", "اوكي",
            "توكل علي الله", "توكل على الله", "سجل", "صح", "صحيح", "ارسل", "ابعت",
            "yes", "confirm", "ok", "sure", "proceed", "execute"
        )
        return keywords.any { normalized.contains(it) }
    }

    /**
     * Checks if spoken reply is negative (No, Cancel, Abort).
     */
    fun isConfirmationNegative(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.getDefault())
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ة", "ه")
        val keywords = listOf(
            "لا", "الغاء", "تراجع", "كلا", "بلاش", "وقف", "خطا", "غلط",
            "no", "cancel", "abort", "stop", "never"
        )
        return keywords.any { normalized.contains(it) }
    }

    /**
     * Analyze user voice/text command and produce structured AiBusinessAction.
     */
    suspend fun analyzeUserVoiceCommand(
        context: Context,
        rawInput: String,
        defaultContact: String = "",
        adminConfig: AdminConfigEntity? = null
    ): AiBusinessAction = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return@withContext AiBusinessAction(
                type = AiActionType.GENERAL_CONSULTATION,
                targetContactName = defaultContact,
                rawUserPrompt = rawInput,
                confirmationQuestionSpeech = "",
                requiresConfirmation = false
            )
        }

        // 1. First, normalize spoken hashtags (e.g. "هاشتاج توريد" -> "#توريد") & run local Arabic NLP parser
        val hashtagNormalized = SmartVoiceAccountingParser.normalizeSpokenHashtags(trimmed)
        val parsed = SmartVoiceAccountingParser.parseVoiceAccountingText(hashtagNormalized, defaultContact)
        val normalized = hashtagNormalized.lowercase(Locale.getDefault())
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ة", "ه").replace("ى", "ي")

        // 2. Identify Intent & Action Type
        val isDeduct = normalized.contains("اخصم") || normalized.contains("خصم") ||
                normalized.contains("سحب من") || normalized.contains("عليه") || normalized.contains("مدين")

        val isAdd = normalized.contains("اضف") || normalized.contains("اضافة") ||
                normalized.contains("زود") || normalized.contains("سداد") || normalized.contains("دفعة") ||
                normalized.contains("له") || normalized.contains("استلمت") || normalized.contains("قبضت")

        val isCreateAccount = normalized.contains("اضف حساب") || normalized.contains("انشئ حساب") ||
                normalized.contains("ادراج حساب") || normalized.contains("حساب جديد") ||
                normalized.contains("عميل جديد") || normalized.contains("تاجر جديد")

        val isSendMessageOnly = (normalized.contains("ابعت") || normalized.contains("ارسل") || normalized.contains("بلغ") || hashtagNormalized.contains("#")) &&
                !isDeduct && !isAdd

        val isGoldTransaction = (normalized.contains("ذهب") || normalized.contains("عيار") || normalized.contains("جرام") || normalized.contains("سبيكة")) &&
                (parsed.weightGrams > 0.0 || parsed.karat != null)

        // Extract Counterparty / Target Contact Name
        var contactName = parsed.counterPartyName.ifBlank { defaultContact }
        if (contactName.isBlank()) {
            contactName = extractContactFromText(hashtagNormalized)
        }
        if (contactName.isBlank()) {
            contactName = if (defaultContact.isNotBlank()) defaultContact else "العميل"
        }

        // Extract phone number if creating account
        val phone = extractPhoneFromText(hashtagNormalized)

        // Extract custom hashtag (either typed #tag or spoken "هاشتاج <name>") or build standard hashtag
        val hashtag = when {
            hashtagNormalized.contains("#") -> {
                val tagRegex = Regex("#([\\u0600-\\u06FF\\w]+)")
                tagRegex.find(hashtagNormalized)?.value ?: if (isDeduct) "#خصم" else "#سداد"
            }
            isDeduct -> {
                if (parsed.amount > 0.0) "#خصم_${parsed.amount.toInt()}_جم" else "#خصم"
            }
            isAdd -> {
                if (parsed.amount > 0.0) "#سداد_${parsed.amount.toInt()}_جم" else "#سداد"
            }
            isGoldTransaction -> "#ذهب"
            else -> "#متابعة"
        }

        // Extract extra user notes or data to send along
        val extraData = extractExtraNotes(hashtagNormalized, contactName, hashtag, parsed.amount)

        // Build Confirmation Speech & Execution Speech in crystal clear Arabic
        val (actionType, questionSpeech, successSpeech) = when {
            isCreateAccount -> {
                val ph = if (phone != null) " برقم هاتف $phone" else ""
                Triple(
                    AiActionType.CREATE_ACCOUNT,
                    "هل تؤكد إضافة حساب جديد باسم $contactName$ph في سجل الحسابات؟",
                    "تم بنجاح إنشاء حساب $contactName وحفظه في النظام."
                )
            }
            isDeduct -> {
                val amtStr = if (parsed.amount > 0.0) "${parsed.amount.toInt()} جنيه" else "المبلغ المحدد"
                val extraStr = if (extraData.isNotBlank()) " مع بيان: $extraData" else ""
                Triple(
                    AiActionType.DEDUCT_BALANCE,
                    "هل تؤكد خصم مبلغ $amtStr من حساب $contactName وإرسال هاشتاج ($hashtag) إلى محادثته$extraStr؟",
                    "تم بنجاح خصم $amtStr من حساب $contactName وإرسال الهاشتاج وتحديث الدفتر المحاسبي."
                )
            }
            isAdd -> {
                val amtStr = if (parsed.amount > 0.0) "${parsed.amount.toInt()} جنيه" else "المبلغ المحدد"
                val extraStr = if (extraData.isNotBlank()) " مع بيان: $extraData" else ""
                Triple(
                    AiActionType.ADD_BALANCE,
                    "هل تؤكد إضافة وتوريد مبلغ $amtStr لحساب $contactName وإرسال هاشتاج ($hashtag) إلى محادثته$extraStr؟",
                    "تم بنجاح إضافة $amtStr لحساب $contactName وإرسال الهاشتاج وتحديث الرصيد."
                )
            }
            isGoldTransaction -> {
                val weightStr = "${parsed.weightGrams} جرام"
                val karatStr = if (parsed.karat != null) "عيار ${parsed.karat}" else ""
                Triple(
                    AiActionType.ADD_GOLD_TRANSACTION,
                    "هل تؤكد تسجيل حركة ذهب بوزن $weightStr $karatStr لحساب $contactName وإرسال هاشتاج ($hashtag)؟",
                    "تم بنجاح تسجيل حركة الذهب بوزن $weightStr لحساب $contactName وتوثيقها بالدفتر العام."
                )
            }
            isSendMessageOnly -> {
                Triple(
                    AiActionType.SEND_HASHTAG_MESSAGE,
                    "هل تؤكد إرسال الهاشتاج والرسالة التالية إلى محادثة $contactName: ($hashtag $extraData)؟",
                    "تم بنجاح إرسال الهاشتاج والرسالة إلى محادثة $contactName."
                )
            }
            else -> {
                // If amount exists, default to debit or credit
                if (parsed.amount > 0.0) {
                    val amtStr = "${parsed.amount.toInt()} جنيه"
                    Triple(
                        AiActionType.DEDUCT_BALANCE,
                        "هل تؤكد خصم وتثبيت مبلغ $amtStr لحساب $contactName ووسم الهاشتاج ($hashtag)؟",
                        "تم بنجاح تنفيذ القيد بمبلغ $amtStr لحساب $contactName."
                    )
                } else {
                    Triple(
                        AiActionType.GENERAL_CONSULTATION,
                        "",
                        ""
                    )
                }
            }
        }

        return@withContext AiBusinessAction(
            type = actionType,
            targetContactName = contactName,
            amount = parsed.amount,
            weightGrams = parsed.weightGrams,
            karat = parsed.karat,
            unitPrice = parsed.unitPrice,
            suggestedHashtag = hashtag,
            customDataOrNotes = extraData,
            confirmationQuestionSpeech = questionSpeech,
            executionConfirmationSpeech = successSpeech,
            rawUserPrompt = rawInput,
            requiresConfirmation = (actionType != AiActionType.GENERAL_CONSULTATION),
            targetPhoneNumber = phone
        )
    }

    /**
     * Executes the confirmed action on the database, contacts, chats, messages, and ledger.
     */
    suspend fun executeConfirmedAction(
        context: Context,
        repository: GoldChatRepository,
        mainViewModel: MainViewModel,
        action: AiBusinessAction
    ): ActionResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = repository.currentUser.first()
            val senderId = currentUser?.id ?: "me"
            val senderName = currentUser?.name ?: "المدير العام"

            val contactName = action.targetContactName.ifBlank { "عميل عام" }

            // Step 1: Find or Create Contact & Chat
            val allContacts: List<ContactEntity> = repository.allContacts.first()
            var matchedContact: ContactEntity? = allContacts.find {
                it.name.equals(contactName, ignoreCase = true) ||
                        it.name.contains(contactName, ignoreCase = true) ||
                        contactName.contains(it.name, ignoreCase = true)
            }

            if (matchedContact == null) {
                // Create a new contact automatically
                val newContact = ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = contactName,
                    phone = action.targetPhoneNumber ?: "010${(10000000..99999999).random()}",
                    username = null,
                    type = UserType.BUSINESSMAN.name,
                    isVerified = false,
                    fromPhoneBook = false
                )
                repository.db.contactDao().insertContact(newContact)
                matchedContact = newContact
            }

            // Find or Create Chat for this contact
            val allChats: List<ChatEntity> = repository.allChats.first()
            var targetChat: ChatEntity? = allChats.find { it.contactId == matchedContact!!.id || it.contactName.equals(matchedContact!!.name, ignoreCase = true) }
            if (targetChat == null) {
                targetChat = repository.createChat(matchedContact!!)
            }

            // Step 2: Handle Specific Action Type
            val formattedMessageText = buildString {
                append("${action.suggestedHashtag} ")
                if (action.amount > 0.0) {
                    append("${String.format(Locale.US, "%,.2f", action.amount)} ج.م ")
                }
                if (action.weightGrams > 0.0) {
                    append("• ${action.weightGrams} جرام ")
                }
                if (action.karat != null) {
                    append("• عيار ${action.karat} ")
                }
                if (action.customDataOrNotes.isNotBlank()) {
                    append("• ${action.customDataOrNotes} ")
                }
                append("[⚡ عبر الوكيل الذكي]")
            }.trim()

            when (action.type) {
                AiActionType.DEDUCT_BALANCE,
                AiActionType.ADD_BALANCE -> {
                    if (action.amount > 0.0) {
                        val direction = if (action.type == AiActionType.DEDUCT_BALANCE) {
                            HashtagEntryDirection.DEBIT
                        } else {
                            HashtagEntryDirection.CREDIT
                        }
                        val entryType = if (action.type == AiActionType.DEDUCT_BALANCE) {
                            HashtagEntryType.INVOICE
                        } else {
                            HashtagEntryType.PAYMENT
                        }
                        repository.recordChatHashtagOperation(
                            chatId = targetChat.id,
                            tag = action.suggestedHashtag.ifBlank { if (action.type == AiActionType.DEDUCT_BALANCE) "خصم" else "سداد" },
                            entryType = entryType,
                            description = action.customDataOrNotes.ifBlank { formattedMessageText },
                            amount = action.amount,
                            direction = direction,
                            actorId = senderId,
                            actorName = senderName,
                            paymentMethod = "كاش",
                            postChatMessage = true
                        )
                    } else {
                        repository.sendMessage(
                            chatId = targetChat.id,
                            senderId = senderId,
                            senderName = senderName,
                            content = formattedMessageText,
                            type = MessageType.TEXT.name
                        )
                    }
                }

                AiActionType.SEND_HASHTAG_MESSAGE -> {
                    repository.sendMessage(
                        chatId = targetChat.id,
                        senderId = senderId,
                        senderName = senderName,
                        content = formattedMessageText,
                        type = MessageType.TEXT.name
                    )
                    repository.autoProcessHashtagFromMessage(
                        chatId = targetChat.id,
                        content = formattedMessageText,
                        senderId = senderId,
                        senderName = senderName
                    )
                }

                AiActionType.ADD_GOLD_TRANSACTION -> {
                    val unitPrice = action.unitPrice ?: 3500.0
                    val weight = if (action.weightGrams > 0.0) action.weightGrams else 10.0
                    val totalPrice = unitPrice * weight
                    val karatName = when (action.karat) {
                        18 -> GoldKarat.KARAT_18.name
                        24 -> GoldKarat.KARAT_24.name
                        else -> GoldKarat.KARAT_21.name
                    }
                    val txCode = ShortCodeGenerator.nextTransactionCode()
                    val tx = TransactionEntity(
                        id = txCode,
                        chatId = targetChat.id,
                        currentUserId = senderId,
                        currentUserName = senderName,
                        currentUserType = currentUser?.type ?: UserType.BUSINESSMAN.name,
                        contactId = matchedContact!!.id,
                        contactName = matchedContact!!.name,
                        goldKarat = karatName,
                        goldType = "مصاغ",
                        weight = weight,
                        pricePerGram = unitPrice,
                        manufacturingCostPerGram = 0.0,
                        totalGoldPrice = totalPrice,
                        totalManufacturing = 0.0,
                        totalPrice = totalPrice,
                        notes = "${action.suggestedHashtag} ${action.customDataOrNotes}".trim()
                    )
                    repository.addGoldTransaction(tx)
                    repository.sendMessage(
                        chatId = targetChat.id,
                        senderId = senderId,
                        senderName = senderName,
                        content = "$formattedMessageText • كود $txCode",
                        type = MessageType.TEXT.name
                    )
                }

                AiActionType.CREATE_ACCOUNT -> {
                    repository.sendMessage(
                        chatId = targetChat.id,
                        senderId = senderId,
                        senderName = senderName,
                        content = "#افتتاح_حساب تم تسجيل وتفعيل حساب $contactName بنجاح",
                        type = MessageType.TEXT.name
                    )
                }

                AiActionType.GENERAL_CONSULTATION -> {
                    // Answered directly in UI via SmartAccountantEngine
                }
            }

            Log.i(TAG, "Successfully executed action: ${action.type} for $contactName in chat: ${targetChat.id}")

            ActionResult.Success(
                message = action.executionConfirmationSpeech.ifBlank { "تم تنفيذ العملية بنجاح." },
                chatId = targetChat.id,
                contactName = contactName,
                hashtag = action.suggestedHashtag,
                amount = action.amount,
                formattedMessage = formattedMessageText
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action: ${e.message}", e)
            ActionResult.Failure(
                error = AppErrorMapper.fromThrowable(
                    e,
                    fallbackAr = "تعذر تنفيذ العملية، حاول مجدداً",
                    fallbackEn = "Couldn't complete action"
                )
            )
        }
    }

    private fun extractContactFromText(text: String): String {
        val pattern = Regex("(?:لحساب|من حساب|رصيد|للعميل|للاستاذ|للأستاذ|من|مع|طرف|باسم|بإسم|إلى|الي)\\s+([\\u0600-\\u06FF\\w]{2,18})")
        val match = pattern.find(text)
        if (match != null) {
            val name = match.groupValues[1].trim()
            val stopWords = listOf("حساب", "رصيد", "مبلغ", "جنيه", "دفعة", "خصم", "سداد", "جديد")
            if (!stopWords.contains(name) && name.length >= 2) {
                return name
            }
        }
        return ""
    }

    private fun extractPhoneFromText(text: String): String? {
        val phoneRegex = Regex("(?:01[0125][0-9]{8})")
        return phoneRegex.find(text)?.value
    }

    private fun extractExtraNotes(text: String, contactName: String, hashtag: String, amount: Double): String {
        var cleaned = text
            .replace(hashtag, "")
            .replace(contactName, "")
            .replace("اخصم", "")
            .replace("خصم", "")
            .replace("اضف", "")
            .replace("أضف", "")
            .replace("لحساب", "")
            .replace("من حساب", "")
            .replace("رصيد", "")
            .replace("جنيه", "")
            .replace("ج.م", "")
            .replace("وابعت له", "")
            .replace("وارسل له", "")
            .replace("واكتب له", "")
            .replace("مع الهاشتاج", "")
            .replace("هاشتاج", "")
            .replace("هشتاج", "")
            .replace("بيان", "")
            .trim()

        if (amount > 0.0) {
            cleaned = cleaned.replace("${amount.toInt()}", "").trim()
        }

        return cleaned.replace(Regex("\\s+"), " ").trim().take(120)
    }

    sealed class ActionResult {
        data class Success(
            val message: String,
            val chatId: String,
            val contactName: String,
            val hashtag: String,
            val amount: Double,
            val formattedMessage: String
        ) : ActionResult()

        data class Failure(val error: String) : ActionResult()
    }
}
