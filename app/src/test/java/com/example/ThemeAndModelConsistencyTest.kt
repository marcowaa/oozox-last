package com.example

import com.example.data.model.*
import com.example.ui.spatial.ChatSpatialMode
import com.example.ui.theme.AppThemePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeAndModelConsistencyTest {

    @Test
    fun appThemePalette_resolution_worksForKnownAndUnknownIds() {
        assertEquals(AppThemePalette.OOZOX_INFINITY, AppThemePalette.fromId("OOZOX_INFINITY"))
        assertEquals(AppThemePalette.ROYAL_GOLD, AppThemePalette.fromId("ROYAL_GOLD"))
        assertEquals(AppThemePalette.VELVET_PURPLE, AppThemePalette.fromId("VELVET_PURPLE"))
        assertEquals(AppThemePalette.CYBER_SAPPHIRE, AppThemePalette.fromId("CYBER_SAPPHIRE"))
        assertEquals(AppThemePalette.EMERALD_GOLD, AppThemePalette.fromId("EMERALD_GOLD"))

        // Fallback for unknown ID should be default OOZOX_INFINITY
        assertEquals(AppThemePalette.OOZOX_INFINITY, AppThemePalette.fromId("UNKNOWN_PALETTE_ID"))
        assertEquals(AppThemePalette.OOZOX_INFINITY, AppThemePalette.fromId(""))
    }

    @Test
    fun userTypes_haveConsistentNames() {
        assertEquals("BUSINESSMAN", UserType.BUSINESSMAN.name)
        assertEquals("GOLD_TRADER", UserType.GOLD_TRADER.name)
    }

    @Test
    fun transactionStatuses_haveConsistentNames() {
        assertNotNull(TransactionStatus.PURCHASED)
        assertNotNull(TransactionStatus.RETURNED)
        assertNotNull(TransactionStatus.RESOLD)
    }

    @Test
    fun silverKarat_labels_areAccurate() {
        assertEquals("عيار 999", SilverKarat.KARAT_999.displayLabel())
        assertEquals("عيار 925", SilverKarat.KARAT_925.displayLabel())
        assertEquals("عيار 800", SilverKarat.KARAT_800.displayLabel())
    }

    @Test
    fun chatSpatialMode_labels_areNotEmpty() {
        ChatSpatialMode.entries.forEach { mode ->
            assertNotNull(mode.label)
            assert(mode.label.isNotEmpty())
        }
    }
}
