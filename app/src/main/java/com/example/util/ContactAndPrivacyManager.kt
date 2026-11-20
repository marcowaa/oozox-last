package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.example.data.model.ContactEntity
import com.example.data.model.UserType
import java.util.UUID

enum class CallPrivacy(val key: String, val titleAr: String, val descAr: String) {
    EVERYONE(
        key = "EVERYONE",
        titleAr = "العامة (الجميع)",
        descAr = "استقبال المكالمات الصوتية والمرئية من أي مستخدم عبر المنصة"
    ),
    CONTACTS_ONLY(
        key = "CONTACTS_ONLY",
        titleAr = "جهات اتصالي فقط",
        descAr = "السماح بالمكالمات من جهات الاتصال المحفوظة في حسابك فقط"
    ),
    NOBODY(
        key = "NOBODY",
        titleAr = "مغلق تماماً (تعطيل المكالمات)",
        descAr = "إيقاف وحظر جميع المكالمات الصوتية والمرئية تماماً"
    );

    companion object {
        fun fromKey(key: String?): CallPrivacy {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: EVERYONE
        }
    }
}

object CallPrivacyManager {
    private const val PREFS_NAME = "oozox_call_privacy_prefs"
    private const val KEY_PRIVACY = "call_privacy_mode"

    fun getCallPrivacy(context: Context): CallPrivacy {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_PRIVACY, CallPrivacy.EVERYONE.key)
        return CallPrivacy.fromKey(saved)
    }

    fun setCallPrivacy(context: Context, privacy: CallPrivacy) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PRIVACY, privacy.key).apply()
    }

    /**
     * Checks if an incoming or outgoing call is permissible based on user settings.
     * Returns Pair(isAllowed, reasonIfNotAllowed)
     */
    fun checkCallAllowed(
        context: Context,
        isContactSaved: Boolean,
        callerName: String = "الطرف الآخر"
    ): Pair<Boolean, String?> {
        val mode = getCallPrivacy(context)
        return when (mode) {
            CallPrivacy.EVERYONE -> Pair(true, null)
            CallPrivacy.CONTACTS_ONLY -> {
                if (isContactSaved) {
                    Pair(true, null)
                } else {
                    Pair(false, "المكالمات محددة لجهات الاتصال المسجلة في حسابك فقط. ليس مسجلاً في جهات اتصالك.")
                }
            }
            CallPrivacy.NOBODY -> {
                Pair(false, "المكالمات الصوتية والمرئية مغلقة تماماً وفق إعدادات الخصوصية لحسابك.")
            }
        }
    }
}

/**
 * Intelligent Discreet Blocking Engine (الحظر الذكي غير المرئي للطرف الآخر)
 * Ensures that the blocked party sees NO block badge or error,
 * but blocker's updated details (profile photo, status, bio) are gracefully hidden from the blocked party.
 */
object BlockManager {
    private const val PREFS_NAME = "oozox_block_prefs"
    private const val KEY_BLOCKED_USERS = "blocked_users_set"
    private const val KEY_MUTUAL_MASKS = "discreet_blocked_mask_set"

    fun isUserBlocked(context: Context, contactOrUserId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blocked = prefs.getStringSet(KEY_BLOCKED_USERS, emptySet()) ?: emptySet()
        return blocked.contains(contactOrUserId)
    }

    fun blockUser(context: Context, contactOrUserId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blocked = (prefs.getStringSet(KEY_BLOCKED_USERS, emptySet()) ?: emptySet()).toMutableSet()
        blocked.add(contactOrUserId)
        prefs.edit().putStringSet(KEY_BLOCKED_USERS, blocked).apply()
    }

    fun unblockUser(context: Context, contactOrUserId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blocked = (prefs.getStringSet(KEY_BLOCKED_USERS, emptySet()) ?: emptySet()).toMutableSet()
        blocked.remove(contactOrUserId)
        prefs.edit().putStringSet(KEY_BLOCKED_USERS, blocked).apply()
    }

    fun getBlockedUserIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_BLOCKED_USERS, emptySet()) ?: emptySet()
    }

    /**
     * If user is blocked or has blocked someone, determines whether private data
     * (such as updated avatar or status) should be hidden/masked.
     * Silent behavior: The blocked person sees NO indicator, but the blocker's photo/details are hidden.
     */
    fun shouldMaskProfileData(context: Context, contactOrUserId: String): Boolean {
        return isUserBlocked(context, contactOrUserId)
    }
}

/**
 * Android Device Contacts Sync Engine
 * Imports contacts from the system phonebook and saves them permanently in Room.
 */
object ContactSyncEngine {
    private const val PREFS_NAME = "oozox_contacts_sync"
    private const val KEY_LAST_SYNC = "last_contacts_sync_timestamp"
    private const val KEY_DAILY_SYNC_ENABLED = "daily_sync_enabled"

    fun isDailySyncEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DAILY_SYNC_ENABLED, true)
    }

    fun setDailySyncEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DAILY_SYNC_ENABLED, enabled)
            .apply()
    }

    fun shouldSyncToday(context: Context): Boolean {
        if (!isDailySyncEnabled(context)) return false
        val lastSync = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SYNC, 0L)
        val now = System.currentTimeMillis()
        return (now - lastSync) > 86_400_000L
    }

    fun markSyncedToday(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun extractContactFromUri(context: Context, contactUri: Uri): Pair<String, String> {
        var name = ""
        var phone = ""
        try {
            val cursor = context.contentResolver.query(contactUri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val hasPhoneIdx = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    if (nameIdx != -1) name = it.getString(nameIdx) ?: ""
                    val contactId = if (idIdx != -1) it.getString(idIdx) ?: "" else ""
                    val hasPhone = if (hasPhoneIdx != -1) it.getInt(hasPhoneIdx) > 0 else true

                    if (hasPhone && contactId.isNotBlank()) {
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )
                        phoneCursor?.use { pCur ->
                            if (pCur.moveToFirst()) {
                                val pIdx = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (pIdx != -1) phone = pCur.getString(pIdx) ?: ""
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactSyncEngine", "Error picking contact", e)
        }

        val cleanNum = phone
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .trim()

        return Pair(name.trim(), cleanNum)
    }

    fun importPhoneContacts(context: Context): List<ContactEntity> {
        val imported = mutableListOf<ContactEntity>()
        val seenPhones = mutableSetOf<String>()

        try {
            val contentResolver = context.contentResolver
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val rawName = if (nameIdx != -1) it.getString(nameIdx) ?: "" else ""
                    val rawNum = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                    val contactId = if (idIdx != -1) it.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()

                    val cleanNum = rawNum
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("(", "")
                        .replace(")", "")
                        .trim()

                    if (cleanNum.isNotBlank() && rawName.isNotBlank() && !seenPhones.contains(cleanNum)) {
                        seenPhones.add(cleanNum)
                        val entity = ContactEntity(
                            id = "contact_device_${contactId}_${cleanNum.takeLast(9)}",
                            name = rawName.trim(),
                            phone = cleanNum,
                            username = rawName.trim().lowercase().replace("\\s+".toRegex(), "_").take(24),
                            type = "", // Leave empty so the user can enter or edit any job freely
                            isVerified = false,
                            fromPhoneBook = true
                        )
                        imported.add(entity)
                    }
                }
            }
            markSyncedToday(context)
        } catch (e: Exception) {
            android.util.Log.e("ContactSyncEngine", "Error querying contacts", e)
        }

        return imported
    }
}
