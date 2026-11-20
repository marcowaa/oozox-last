package com.example

import com.example.data.model.HashtagEntryDirection
import com.example.data.model.HashtagEntryEntity
import com.example.data.model.HashtagEntryType
import com.example.util.HashtagUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HashtagUtilsTest {

    @Test
    fun parseHashtag_extractsTagCorrectly() {
        assertEquals("ذهب", HashtagUtils.parseHashtag("#ذهب"))
        assertEquals("توريد_2026", HashtagUtils.parseHashtag("تم استلام الدفعة #توريد_2026 شكراً لك"))
        assertEquals("فاتورة", HashtagUtils.parseHashtag("#فاتورة شراء ذهب عيار 21"))
    }

    @Test
    fun parseHashtag_returnsNullWhenNoHashtag() {
        assertNull(HashtagUtils.parseHashtag("رسالة عادية بدون وسوم"))
        assertNull(HashtagUtils.parseHashtag(""))
    }

    @Test
    fun calculateHashtagTotals_aggregatesInvoicesAndPaymentsAccurately() {
        val entries = listOf(
            HashtagEntryEntity(
                id = "1",
                hashtagId = "h1",
                accountId = "a1",
                type = HashtagEntryType.INVOICE.name,
                description = "فاتورة مشغولات لازوردي",
                amount = 50000.0,
                direction = HashtagEntryDirection.DEBIT.name,
                createdBy = "User1"
            ),
            HashtagEntryEntity(
                id = "2",
                hashtagId = "h1",
                accountId = "a1",
                type = HashtagEntryType.INVOICE.name,
                description = "فاتورة سبيكة 10 جرام",
                amount = 40000.0,
                direction = HashtagEntryDirection.DEBIT.name,
                createdBy = "User1"
            ),
            HashtagEntryEntity(
                id = "3",
                hashtagId = "h1",
                accountId = "a1",
                type = HashtagEntryType.PAYMENT.name,
                description = "تحويل بنكي دفعة أولى",
                amount = 30000.0,
                direction = HashtagEntryDirection.CREDIT.name,
                createdBy = "User2"
            )
        )

        val totals = HashtagUtils.calculateHashtagTotals(entries)

        assertEquals(90000.0, totals.totalDebit, 0.001)
        assertEquals(30000.0, totals.totalCredit, 0.001)
        assertEquals(60000.0, totals.remaining, 0.001)
    }

    @Test
    fun calculateHashtagTotals_emptyList_returnsZeroTotals() {
        val totals = HashtagUtils.calculateHashtagTotals(emptyList())

        assertEquals(0.0, totals.totalDebit, 0.001)
        assertEquals(0.0, totals.totalCredit, 0.001)
        assertEquals(0.0, totals.remaining, 0.001)
    }

    @Test
    fun calculateHashtagTotals_exactPayment_returnsZeroRemaining() {
        val entries = listOf(
            HashtagEntryEntity(
                id = "1",
                hashtagId = "h1",
                accountId = "a1",
                type = HashtagEntryType.INVOICE.name,
                description = "فاتورة",
                amount = 25000.0,
                direction = HashtagEntryDirection.DEBIT.name,
                createdBy = "User"
            ),
            HashtagEntryEntity(
                id = "2",
                hashtagId = "h1",
                accountId = "a1",
                type = HashtagEntryType.PAYMENT.name,
                description = "سداد كامل",
                amount = 25000.0,
                direction = HashtagEntryDirection.CREDIT.name,
                createdBy = "User"
            )
        )

        val totals = HashtagUtils.calculateHashtagTotals(entries)

        assertEquals(25000.0, totals.totalDebit, 0.001)
        assertEquals(25000.0, totals.totalCredit, 0.001)
        assertEquals(0.0, totals.remaining, 0.001)
    }
}
