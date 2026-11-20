package com.example

import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.HiddenProfileEntity
import com.example.data.model.WorkGroupEntity
import com.example.util.AccountingSpeechToTextEngine
import com.example.util.AccountingVoiceCommand
import com.example.util.AiVoiceBusinessAgentEngine
import com.example.util.SmartVoiceAccountingParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountingVoiceFlowTest {

    @Test
    fun parseVoiceAccountingText_expenseInput_parsesDebitAndAmount() {
        val input = "سجل مصروف 500 جنيه بوفيه وضيافة"
        val parsed = SmartVoiceAccountingParser.parseVoiceAccountingText(input, "خزينة الفرع")

        assertEquals(500.0, parsed.amount, 0.01)
        assertEquals("DEBIT", parsed.direction)
        assertTrue(parsed.operationCategory.contains("مصروف") || parsed.suggestedTag.contains("مصروف"))
    }

    @Test
    fun parseVoiceAccountingText_incomingInput_parsesCreditAndAmount() {
        val input = "استلمت 10000 من شركة النور"
        val parsed = SmartVoiceAccountingParser.parseVoiceAccountingText(input, "خزينة المركز")

        assertEquals(10000.0, parsed.amount, 0.01)
        assertEquals("CREDIT", parsed.direction)
        assertTrue(parsed.counterPartyName.contains("النور") || parsed.notes.contains("النور") || parsed.suggestedTag.contains("توريد") || parsed.suggestedTag.contains("قبض") || parsed.suggestedTag.contains("سداد"))
    }

    @Test
    fun parseVoiceAccountingText_goldTransaction_parsesWeightAndKarat() {
        val input = "بعت 25 جرام عيار 21 بسعر 85000"
        val parsed = SmartVoiceAccountingParser.parseVoiceAccountingText(input, "محل الصاغة")

        assertEquals(25.0, parsed.weightGrams, 0.01)
        assertEquals(21, parsed.karat)
        assertEquals(85000.0, parsed.amount, 0.01)
    }

    @Test
    fun normalizeSpokenHashtags_convertsSpokenArabicTriggersToHashtagSymbol() {
        assertEquals("#توريد 5000 جنيه", SmartVoiceAccountingParser.normalizeSpokenHashtags("هاشتاج توريد 5000 جنيه"))
        assertEquals("أضف #مقاولات 12000", SmartVoiceAccountingParser.normalizeSpokenHashtags("أضف وسم مقاولات 12000"))
        assertEquals("#ذهب_21 عيار 21", SmartVoiceAccountingParser.normalizeSpokenHashtags("هاشتاغ جديد ذهب_21 عيار 21"))

        val parsed = SmartVoiceAccountingParser.parseVoiceAccountingText("هاشتاج توريد_حديد 15000 جنيه للعميل سالم", "سالم")
        assertTrue(parsed.suggestedTag.contains("#توريد_حديد"))
        assertEquals(15000.0, parsed.amount, 0.01)
    }

    @Test
    fun voiceAgentEngine_extractsSpokenHashtagCommandCleanly() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val action = AiVoiceBusinessAgentEngine.analyzeUserVoiceCommand(
            context = context,
            rawInput = "أرسل هاشتاج صيانة_مارس بمبلغ 3500 جنيه لحساب أحمد",
            defaultContact = "أحمد"
        )
        assertEquals("#صيانة_مارس", action.suggestedHashtag)
        assertEquals(3500.0, action.amount, 0.01)
    }
}
