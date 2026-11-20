package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Model representing a built-in message notification tone.
 */
data class MessageNotificationTone(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val icon: String,
    val durationMs: Int
)

/**
 * Comprehensive helper for custom message notification sounds.
 * Synthesizes high-fidelity, crystalline alert tones dynamically using pure PCM AudioTrack
 * and generates corresponding WAV files for notification channels.
 */
object MessageNotificationSoundHelper {

    private const val TAG = "MsgSoundHelper"
    private const val PREFS_NAME = "oozox_message_sound_prefs"
    private const val KEY_SELECTED_TONE = "key_message_notification_tone"
    private const val KEY_SOUND_ENABLED = "key_message_sound_enabled"
    private const val KEY_VIBRATION_ENABLED = "key_message_vibration_enabled"

    const val TONE_GOLD = "gold_chime"
    const val TONE_CRYSTAL = "crystal_bell"
    const val TONE_MODERN_POP = "modern_pop"
    const val TONE_SMART_PING = "smart_ping"
    const val TONE_CASH_VAULT = "cash_vault"
    const val TONE_SUBTLE_PULSE = "subtle_pulse"
    const val TONE_SYSTEM_DEFAULT = "system_default"

    val AVAILABLE_TONES = listOf(
        MessageNotificationTone(
            id = TONE_GOLD,
            nameAr = "رنين الذهب الملكي 🪙",
            nameEn = "Royal Gold Chime",
            descriptionAr = "رنين نقي متألق بتوافقيات ذهبية مميزة وخاصة بالتطبيق",
            icon = "🪙",
            durationMs = 480
        ),
        MessageNotificationTone(
            id = TONE_CRYSTAL,
            nameAr = "جرس الكريستال 🔔",
            nameEn = "Crystal Bell",
            descriptionAr = "نغمات كريستالية ثلاثية متتالية فائقة النقاء والوضوح",
            icon = "🔔",
            durationMs = 520
        ),
        MessageNotificationTone(
            id = TONE_MODERN_POP,
            nameAr = "النقرة الحديثة 💬",
            nameEn = "Modern Chat Pop",
            descriptionAr = "صوت فقاعة دردشة عصري مدمج وسريع وممتع للأذن",
            icon = "💬",
            durationMs = 120
        ),
        MessageNotificationTone(
            id = TONE_SMART_PING,
            nameAr = "المحاسب الذكي ⚡",
            nameEn = "Smart AI Ping",
            descriptionAr = "تنبيه إلكتروني مزدوج بنغمة تقنية ذكية واحترافية",
            icon = "⚡",
            durationMs = 280
        ),
        MessageNotificationTone(
            id = TONE_CASH_VAULT,
            nameAr = "رنين الخزينة والصفقات 💰",
            nameEn = "Treasury & Deals",
            descriptionAr = "رنين عملات ذهبية صلبة وصفقات تجارية ناجحة",
            icon = "💰",
            durationMs = 450
        ),
        MessageNotificationTone(
            id = TONE_SUBTLE_PULSE,
            nameAr = "النبض الهادئ 🎵",
            nameEn = "Subtle Pulse",
            descriptionAr = "توافق نغمي هادئ وناعم للمكاتب والاجتماعات",
            icon = "🎵",
            durationMs = 260
        ),
        MessageNotificationTone(
            id = TONE_SYSTEM_DEFAULT,
            nameAr = "نغمة النظام الافتراضية 📱",
            nameEn = "Android Default",
            descriptionAr = "النغمة الرسمية الافتراضية لنظام الأندرويد للهاتف",
            icon = "📱",
            durationMs = 400
        )
    )

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private var activeAudioTrack: AudioTrack? = null
    private var activePlayJob: Job? = null
    private val playbackLock = Any()

    private val _currentPlayingToneId = MutableStateFlow<String?>(null)
    val currentPlayingToneId: StateFlow<String?> = _currentPlayingToneId.asStateFlow()

    fun getSelectedTone(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_TONE, TONE_GOLD) ?: TONE_GOLD
    }

    fun setSelectedTone(context: Context, toneId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_TONE, toneId).apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isVibrationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun getToneById(id: String): MessageNotificationTone {
        return AVAILABLE_TONES.find { it.id == id } ?: AVAILABLE_TONES.first()
    }

    /**
     * Previews a specific tone when tapped in the selection dialog.
     */
    fun playTonePreview(context: Context, toneId: String) {
        stopPlayback()
        _currentPlayingToneId.value = toneId

        if (toneId == TONE_SYSTEM_DEFAULT) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                ringtone?.play()
                audioScope.launch {
                    kotlinx.coroutines.delay(600)
                    _currentPlayingToneId.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play default system ringtone", e)
                _currentPlayingToneId.value = null
            }
            return
        }

        activePlayJob = audioScope.launch {
            try {
                val pcmData = generatePcmForTone(toneId)
                playPcmDirect(pcmData)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing tone preview: $toneId", e)
            } finally {
                _currentPlayingToneId.value = null
            }
        }
    }

    /**
     * Plays the sound for an incoming new message based on user preferences.
     */
    fun playNewMessageSound(context: Context) {
        if (!isSoundEnabled(context)) {
            // If sound disabled but vibration enabled, still trigger haptic vibration
            if (isVibrationEnabled(context)) {
                triggerMessageVibration(context)
            }
            return
        }

        val toneId = getSelectedTone(context)

        // Trigger vibration if enabled
        if (isVibrationEnabled(context)) {
            triggerMessageVibration(context)
        }

        if (toneId == TONE_SYSTEM_DEFAULT) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                ringtone?.play()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing default ringtone", e)
            }
            return
        }

        audioScope.launch {
            try {
                val pcmData = generatePcmForTone(toneId)
                playPcmDirect(pcmData)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing message sound: $toneId", e)
            }
        }
    }

    /**
     * Vibrates the device using modern Android Vibrator / VibratorManager.
     */
    fun triggerMessageVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 120, 80, 160),
                    intArrayOf(0, 180, 0, 240),
                    -1
                )
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 160), -1)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 120, 80, 160), -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }

    fun stopPlayback() {
        activePlayJob?.cancel()
        activePlayJob = null
        synchronized(playbackLock) {
            try {
                activeAudioTrack?.let {
                    if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        it.stop()
                    }
                    it.release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio track", e)
            }
            activeAudioTrack = null
        }
        _currentPlayingToneId.value = null
    }

    /**
     * Writes out synthesized tone to cache WAV file if needed for notifications.
     */
    fun getToneUri(context: Context, toneId: String): Uri? {
        return try {
            if (toneId == TONE_SYSTEM_DEFAULT) {
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val tonesDir = File(context.cacheDir, "notification_tones").apply {
                if (!exists()) mkdirs()
            }
            val wavFile = File(tonesDir, "tone_${toneId}.wav")
            if (!wavFile.exists() || wavFile.length() < 100) {
                val pcmData = generatePcmForTone(toneId)
                writeWavFile(wavFile, pcmData, 44100)
            }
            Uri.fromFile(wavFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tone URI", e)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    // ==========================================
    // Low-latency Direct AudioTrack Synthesizer
    // ==========================================

    private fun playPcmDirect(pcm: ShortArray) {
        synchronized(playbackLock) {
            try {
                activeAudioTrack?.release()
                val sampleRate = 44100
                val bufferSize = pcm.size * 2

                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                activeAudioTrack = track
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play PCM through AudioTrack", e)
            }
        }
    }

    /**
     * Synthesizes mathematical waveforms for each distinctive tone.
     */
    fun generatePcmForTone(toneId: String): ShortArray {
        val sampleRate = 44100
        return when (toneId) {
            TONE_GOLD -> {
                // Royal Gold Chime: Note 1 (E6 1318Hz) -> Note 2 (B6 1975Hz) with warm harmonic overtones
                val durationMs = 480
                val totalSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(totalSamples)

                val note1End = (sampleRate * 0.12).toInt()

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    var sampleVal = 0.0

                    if (i < note1End) {
                        val env = exp(-18.0 * (i.toDouble() / note1End))
                        val s1 = sin(2.0 * PI * 1318.5 * t)
                        val s2 = 0.45 * sin(2.0 * PI * 2637.0 * t)
                        val s3 = 0.20 * sin(2.0 * PI * 3955.5 * t)
                        sampleVal = (s1 + s2 + s3) * env * 0.75
                    }

                    if (i >= (sampleRate * 0.08).toInt()) {
                        val t2 = (i - (sampleRate * 0.08).toInt()).toDouble() / sampleRate
                        val env2 = exp(-8.5 * t2)
                        val tremolo = 1.0 + 0.08 * sin(2.0 * PI * 12.0 * t2)
                        val s1 = sin(2.0 * PI * 1975.5 * t2)
                        val s2 = 0.40 * sin(2.0 * PI * 3951.0 * t2)
                        val s3 = 0.25 * sin(2.0 * PI * 5926.5 * t2)
                        sampleVal += (s1 + s2 + s3) * env2 * tremolo * 0.85
                    }

                    pcm[i] = (sampleVal.coerceIn(-1.0, 1.0) * 32000.0).toInt().toShort()
                }
                pcm
            }

            TONE_CRYSTAL -> {
                // Crystal Bell: C6 (1046Hz) -> G6 (1568Hz) -> C7 (2093Hz) cascading bell chime
                val durationMs = 520
                val totalSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(totalSamples)

                val t1 = (sampleRate * 0.0).toInt()
                val t2 = (sampleRate * 0.07).toInt()
                val t3 = (sampleRate * 0.14).toInt()

                for (i in 0 until totalSamples) {
                    var sampleVal = 0.0

                    if (i >= t1) {
                        val time = (i - t1).toDouble() / sampleRate
                        val env = exp(-16.0 * time)
                        sampleVal += (sin(2.0 * PI * 1046.5 * time) + 0.3 * sin(2.0 * PI * 2093.0 * time)) * env * 0.5
                    }
                    if (i >= t2) {
                        val time = (i - t2).toDouble() / sampleRate
                        val env = exp(-14.0 * time)
                        sampleVal += (sin(2.0 * PI * 1568.0 * time) + 0.35 * sin(2.0 * PI * 3136.0 * time)) * env * 0.65
                    }
                    if (i >= t3) {
                        val time = (i - t3).toDouble() / sampleRate
                        val env = exp(-9.0 * time)
                        sampleVal += (sin(2.0 * PI * 2093.0 * time) + 0.25 * sin(2.0 * PI * 4186.0 * time)) * env * 0.85
                    }

                    pcm[i] = (sampleVal.coerceIn(-1.0, 1.0) * 32000.0).toInt().toShort()
                }
                pcm
            }

            TONE_MODERN_POP -> {
                // Modern Chat Bubble Pop: Fast downwards frequency sweep from 950Hz to 220Hz
                val durationMs = 120
                val totalSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(totalSamples)

                var phase = 0.0
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples
                    val freq = 950.0 - 730.0 * (progress * progress)
                    phase += 2.0 * PI * freq / sampleRate

                    val env = if (progress < 0.08) progress / 0.08 else exp(-16.0 * (progress - 0.08))
                    val sample = sin(phase) * env * 0.95
                    pcm[i] = (sample.coerceIn(-1.0, 1.0) * 32000.0).toInt().toShort()
                }
                pcm
            }

            TONE_SMART_PING -> {
                // Smart AI Tech Ping: Ping 1 (880Hz) -> Ping 2 (1760Hz)
                val durationMs = 280
                val totalSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(totalSamples)

                val p1Start = 0
                val p2Start = (sampleRate * 0.06).toInt()

                for (i in 0 until totalSamples) {
                    var sampleVal = 0.0
                    if (i >= p1Start) {
                        val time = (i - p1Start).toDouble() / sampleRate
                        val env = exp(-35.0 * time)
                        sampleVal += sin(2.0 * PI * 880.0 * time) * env * 0.6
                    }
                    if (i >= p2Start) {
                        val time = (i - p2Start).toDouble() / sampleRate
                        val env = exp(-14.0 * time)
                        sampleVal += sin(2.0 * PI * 1760.0 * time) * env * 0.85
                    }
                    pcm[i] = (sampleVal.coerceIn(-1.0, 1.0) * 32000.0).toInt().toShort()
                }
                pcm
            }

            TONE_CASH_VAULT -> {
                // Treasury & Deals: Metallic Gold Coin & Cash Register Clink
                val durationMs = 450
                val totalSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(totalSamples)

                val c1Start = 0
                val c2Start = (sampleRate * 0.06).toInt()

                for (i in 0 until totalSamples) {
                    var sampleVal = 0.0
                    if (i >= c1Start) {
                        val time = (i - c1Start).toDouble() / sampleRate
                        val env = exp(-28.0 * time)
                        val metal = sin(2.0 * PI * 2349.0 * time) + 0.6 * sin(2.0 * PI * 3136.0 * time)
                        sampleVal += metal * env * 0.55
                    }
                    if (i >= c2Start) {
                        val time = (i - c2Start).toDouble() / sampleRate
                        val env = exp(-9.0 * time)
                        val metal = sin(2.0 * PI * 2637.0 * time) +
                                0.5 * sin(2.0 * PI * 3951.0 * time) +
                                0.3 * sin(2.0 * PI * 5274.0 * time)
                        sampleVal += metal * env * 0.85
                    }
                    pcm[i] = (sampleVal.coerceIn(-1.0, 1.0) * 32000.0).toInt().toShort()
                }
                pcm
            }

            TONE_SUBTLE_PULSE -> {
                // Subtle Pulse: Gentle warm chord (C5 523Hz + E5 659Hz + G5 784Hz)
                val durationMs = 260
                val totalSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / totalSamples
                    val attack = if (progress < 0.12) progress / 0.12 else 1.0
                    val decay = exp(-10.0 * (progress - 0.12).coerceAtLeast(0.0))
                    val env = attack * decay

                    val s1 = sin(2.0 * PI * 523.25 * t)
                    val s2 = 0.8 * sin(2.0 * PI * 659.25 * t)
                    val s3 = 0.6 * sin(2.0 * PI * 783.99 * t)

                    val sampleVal = (s1 + s2 + s3) * env * 0.55
                    pcm[i] = (sampleVal.coerceIn(-1.0, 1.0) * 32000.0).toInt().toShort()
                }
                pcm
            }

            else -> {
                // Default fallback: soft 880Hz beep
                val totalSamples = (sampleRate * 0.2).toInt()
                val pcm = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-12.0 * t)
                    val sample = sin(2.0 * PI * 880.0 * t) * env * 0.7
                    pcm[i] = (sample.coerceIn(-1.0, 1.0) * 32000.0).toInt().toShort()
                }
                pcm
            }
        }
    }

    /**
     * Encodes 16-bit Mono PCM samples into a standard RIFF/WAVE file format.
     */
    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = (pcmData.size * 2).toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = (sampleRate * channels * 2).toLong()

        val header = ByteArray(44)
        // "RIFF"
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        // Chunk size
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        // "WAVE"
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // "fmt "
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        // Subchunk1 size (16 for PCM)
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        // AudioFormat (1 = PCM)
        header[20] = 1
        header[21] = 0
        // Channels (1 = mono)
        header[22] = channels.toByte()
        header[23] = 0
        // SampleRate
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        // ByteRate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        // BlockAlign
        header[32] = (channels * 2).toByte()
        header[33] = 0
        // BitsPerSample (16)
        header[34] = 16
        header[35] = 0
        // "data"
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        // Data length
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        FileOutputStream(file).use { out ->
            out.write(header)
            val byteBuffer = ByteArray(pcmData.size * 2)
            var bIdx = 0
            for (sample in pcmData) {
                byteBuffer[bIdx++] = (sample.toInt() and 0xff).toByte()
                byteBuffer[bIdx++] = ((sample.toInt() shr 8) and 0xff).toByte()
            }
            out.write(byteBuffer)
        }
    }
}
