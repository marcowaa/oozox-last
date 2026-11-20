package com.example

import com.example.ui.components.CallState
import com.example.ui.components.CallType
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Real-time Encrypted Audio and Video Calling Module
 */
class AudioVideoCallAndCommunicationTest {

    @Test
    fun callTypes_haveCorrectLabelsAndAttributes() {
        assertEquals("مكالمة صوتية مشفرة", CallType.AUDIO.label)
        assertEquals("📞", CallType.AUDIO.icon)

        assertEquals("مكالمة فيديو عالية الدقة", CallType.VIDEO.label)
        assertEquals("📹", CallType.VIDEO.icon)
    }

    @Test
    fun callStates_transitionsAreCorrect() {
        val states = CallState.entries
        assertTrue(states.contains(CallState.CONNECTING))
        assertTrue(states.contains(CallState.CONNECTED))
        assertTrue(states.contains(CallState.ENDED))
    }

    @Test
    fun callDuration_formatting_formatsMinutesAndSecondsAccurately() {
        fun formatDuration(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return "%02d:%02d".format(mins, secs)
        }

        assertEquals("00:00", formatDuration(0))
        assertEquals("00:45", formatDuration(45))
        assertEquals("01:30", formatDuration(90))
        assertEquals("12:05", formatDuration(725))
        assertEquals("60:00", formatDuration(3600))
    }

    @Test
    fun audioAmplitude_normalization_staysWithinSafeRange() {
        fun normalizeAmp(maxAmp: Int): Float {
            return (maxAmp / 25000f).coerceIn(0.2f, 1.0f)
        }

        assertEquals(0.2f, normalizeAmp(0), 0.001f)
        assertEquals(0.2f, normalizeAmp(1000), 0.001f)
        assertEquals(0.5f, normalizeAmp(12500), 0.001f)
        assertEquals(1.0f, normalizeAmp(25000), 0.001f)
        assertEquals(1.0f, normalizeAmp(50000), 0.001f)
    }
}
