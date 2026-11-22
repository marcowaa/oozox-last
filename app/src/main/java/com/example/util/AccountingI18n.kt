package com.example.util

import androidx.compose.runtime.Composable

/**
 * Concise, executive localization helper for the Company Accounting Hub,
 * its Bottom Taskbar, Side Drawer, Voice Assistant, and Accounting Dialogs.
 * Delegates all 14 languages to server-synced Translations ("acct.$key").
 */
object AccountingI18n {
    fun get(key: String, language: AppLanguage): String {
        val namespacedKey = if (key.startsWith("acct.")) key else "acct.$key"
        val resolved = Translations.get(namespacedKey, language)
        return if (resolved == namespacedKey) key else resolved
    }
}

@Composable
fun acctStr(key: String): String {
    @Suppress("UNUSED_VARIABLE")
    val catalogTick = Translations.catalogVersion
    val lang = LocalAppLanguage.current
    return AccountingI18n.get(key, lang)
}
