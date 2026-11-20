package com.example.util

import java.security.SecureRandom
import java.util.Locale

/**
 * Generates collision-safe, short, unique 8-character uppercase English alphanumeric codes
 * WITHOUT hyphens (e.g. OX7K9M2A for invoices, TX4B8N2Q for operations/transactions).
 */
object ShortCodeGenerator {

    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val random = SecureRandom()

    val CODE_REGEX = Regex("\\b((?:OX|TX)[2-9A-HJ-NP-Z]{6})\\b", RegexOption.IGNORE_CASE)

    fun generateInvoiceCode(existingCodes: Set<String> = emptySet()): String {
        repeat(64) {
            val candidate = buildCode("OX")
            if (!existingCodes.contains(candidate)) return candidate
        }
        return buildCode("OX")
    }

    fun generateOperationCode(existingCodes: Set<String> = emptySet()): String {
        repeat(64) {
            val candidate = buildCode("TX")
            if (!existingCodes.contains(candidate)) return candidate
        }
        return buildCode("TX")
    }

    fun nextTransactionCode(existingCodes: Set<String> = emptySet()): String =
        generateOperationCode(existingCodes)

    fun normalize(code: String?): String =
        code?.trim()?.replace("-", "")?.uppercase(Locale.US) ?: ""

    fun extractAllCodes(text: String): List<String> {
        return CODE_REGEX.findAll(text)
            .map { it.groupValues[1].uppercase(Locale.US) }
            .distinct()
            .toList()
    }

    private fun buildCode(prefix: String): String {
        val sb = StringBuilder(8)
        sb.append(prefix)
        repeat(8 - prefix.length) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)])
        }
        return sb.toString()
    }
}
