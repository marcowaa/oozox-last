package com.example

import com.example.data.model.ChatAccountEntity
import com.example.data.model.HashtagAccountEntity
import com.example.data.model.HashtagEntryEntity
import com.example.data.model.HashtagEntryType
import com.example.data.model.HashtagEntryDirection
import com.example.util.SmartAccountantEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAccountantEngineTest {

    @Test
    fun generateSmartAlerts_withEmptyData_returnsEmptyList() {
        val alerts = SmartAccountantEngine.generateSmartAlerts(
            accounts = emptyList(),
            hashtags = emptyList(),
            entries = emptyList(),
            chatName = "محادثة تجريبية"
        )

        assertNotNull(alerts)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun generateSmartAlerts_withOverpayment_detectsOverpayAlert() {
        val accounts = listOf(
            ChatAccountEntity(
                id = "acc1",
                chatId = "c1",
                name = "حساب الذهب",
                creatorId = "u1"
            )
        )
        val hashtags = listOf(
            HashtagAccountEntity(
                id = "h1",
                chatId = "c1",
                accountId = "acc1",
                tag = "ذهب",
                creatorId = "u1"
            )
        )
        val entries = listOf(
            HashtagEntryEntity(
                id = "e1",
                hashtagId = "h1",
                accountId = "acc1",
                type = HashtagEntryType.INVOICE.name,
                description = "فاتورة",
                amount = 10000.0,
                direction = HashtagEntryDirection.DEBIT.name,
                createdBy = "u1"
            ),
            HashtagEntryEntity(
                id = "e2",
                hashtagId = "h1",
                accountId = "acc1",
                type = HashtagEntryType.PAYMENT.name,
                description = "دفعة زائدة",
                amount = 15000.0,
                direction = HashtagEntryDirection.CREDIT.name,
                createdBy = "u2"
            )
        )

        val alerts = SmartAccountantEngine.generateSmartAlerts(
            accounts = accounts,
            hashtags = hashtags,
            entries = entries,
            chatName = "محادثة الصاغة"
        )

        assertNotNull(alerts)
        assertTrue(alerts.any { it.id.contains("overpay") })
    }
}
