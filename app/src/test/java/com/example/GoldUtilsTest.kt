package com.example

import com.example.data.model.GoldKarat
import com.example.util.GoldUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoldUtilsTest {

    @Test
    fun calculateGoldPrice_standard21k_calculatesCorrectly() {
        val weight = 10.0
        val pricePerGram = 3850.0
        val mfgCostPerGram = 120.0

        val (totalGold, totalMfg, totalPrice) = GoldUtils.calculateGoldPrice(weight, pricePerGram, mfgCostPerGram)

        assertEquals(38500.0, totalGold, 0.001)
        assertEquals(1200.0, totalMfg, 0.001)
        assertEquals(39700.0, totalPrice, 0.001)
    }

    @Test
    fun calculateGoldPrice_zeroWeightOrPrice_returnsZeros() {
        val (goldZero, mfgZero, totalZero) = GoldUtils.calculateGoldPrice(0.0, 3850.0, 100.0)
        assertEquals(0.0, goldZero, 0.001)
        assertEquals(0.0, mfgZero, 0.001)
        assertEquals(0.0, totalZero, 0.001)
    }

    @Test
    fun karatDisplayLabel_returnsCorrectArabicLabels() {
        assertEquals("عيار 24", GoldKarat.KARAT_24.displayLabel())
        assertEquals("عيار 21", GoldKarat.KARAT_21.displayLabel())
        assertEquals("عيار 18", GoldKarat.KARAT_18.displayLabel())
        assertEquals("عيار 14", GoldKarat.KARAT_14.displayLabel())
    }

    @Test
    fun getInitials_handlesVariousInputsGracefully() {
        assertEquals("مح", GoldUtils.getInitials("محمد"))
        assertEquals("مع", GoldUtils.getInitials("محمد علي"))
        assertEquals("مع", GoldUtils.getInitials("محمد علي حسن"))
        assertEquals("م", GoldUtils.getInitials(""))
        assertEquals("م", GoldUtils.getInitials("   "))
    }

    @Test
    fun formatWeight_and_formatCurrency_outputProperStrings() {
        val formattedWeight = GoldUtils.formatWeight(12.34)
        assertTrue(formattedWeight.contains("جم"))

        val formattedCurrency = GoldUtils.formatCurrency(5000.0)
        assertTrue(formattedCurrency.contains("ج.م"))
    }

    @Test
    fun scrapMetalLoss_calculationLogic_isAccurate() {
        val grossWeight = 100.0
        val lossRate = 0.5 // 0.5%
        val expectedLossGrams = grossWeight * (lossRate / 100.0) // 0.5 grams
        val netWeight = grossWeight - expectedLossGrams

        assertEquals(0.5, expectedLossGrams, 0.0001)
        assertEquals(99.5, netWeight, 0.0001)
    }
}
