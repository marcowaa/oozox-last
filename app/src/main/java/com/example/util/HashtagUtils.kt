package com.example.util

import com.example.data.model.HashtagEntryDirection
import com.example.data.model.HashtagEntryEntity

object HashtagUtils {

    fun parseHashtag(text: String): String? {
        val regex = Regex("#([\\u0600-\\u06FF\\w]+)")
        return regex.find(text)?.groupValues?.get(1)
    }

    fun isHashtagInput(text: String): Boolean {
        return text.trim().startsWith("#")
    }

    fun calculateHashtagTotals(entries: List<HashtagEntryEntity>): HashtagTotals {
        val totalDebit = entries.filter { it.direction == HashtagEntryDirection.DEBIT.name }.sumOf { it.amount }
        val totalCredit = entries.filter { it.direction == HashtagEntryDirection.CREDIT.name }.sumOf { it.amount }
        val remaining = totalDebit - totalCredit
        return HashtagTotals(totalDebit, totalCredit, remaining)
    }

    data class HashtagTotals(
        val totalDebit: Double,
        val totalCredit: Double,
        val remaining: Double
    )
}
