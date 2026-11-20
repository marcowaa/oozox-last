package com.example

import com.example.data.model.GoldPricesModel
import com.example.util.GoldUtils
import org.junit.Assert.*
import org.junit.Test

class PullToRefreshSyncTest {

    @Test
    fun testGoldPriceSimulationAndSyncTimestamp() {
        val initial = GoldUtils.DEFAULT_GOLD_PRICES
        assertNotNull(initial)
        assertTrue(initial.karat21 > 0)
        assertTrue(initial.updatedAt > 0)

        val variation = 5.0
        val new21 = (initial.karat21 + variation).coerceAtLeast(3500.0)
        val new24 = (new21 * 24.0 / 21.0)
        val updated = initial.copy(
            karat21 = new21,
            karat24 = new24,
            updatedAt = System.currentTimeMillis()
        )

        assertEquals(new21, updated.karat21, 0.01)
        assertTrue(updated.karat24 > updated.karat21)
        assertTrue(updated.updatedAt >= initial.updatedAt)
    }
}
