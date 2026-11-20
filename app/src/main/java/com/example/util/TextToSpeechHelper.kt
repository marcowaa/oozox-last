package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * TextToSpeechHelper provides accessible, clear Arabic voice output
 * for announcing accounting audit confirmations, voice actions, and safety alerts.
 */
class TextToSpeechHelper(private val context: Context) {

    companion object {
        private const val TAG = "TextToSpeechHelper"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onDoneCallback: (() -> Unit)? = null
    private var onStartCallback: (() -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("ar"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Arabic TTS language missing or not supported, falling back to default locale")
                        tts?.language = Locale.getDefault()
                    }
                    tts?.setSpeechRate(0.95f) // Natural, clear articulation
                    tts?.setPitch(1.0f)
                    
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "TTS onStart: $utteranceId")
                            onStartCallback?.invoke()
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "TTS onDone: $utteranceId")
                            onDoneCallback?.invoke()
                            onStartCallback = null
                            onDoneCallback = null
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            Log.w(TAG, "TTS onError: $utteranceId")
                            onDoneCallback?.invoke()
                            onStartCallback = null
                            onDoneCallback = null
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            Log.w(TAG, "TTS onError: $utteranceId code: $errorCode")
                            onDoneCallback?.invoke()
                            onStartCallback = null
                            onDoneCallback = null
                        }
                    })

                    isInitialized = true
                } else {
                    Log.w(TAG, "TTS Initialization failed: $status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        speakText(text = text, languageCode = null, onStart = null, onDone = onDone)
    }

    /**
     * Multilingual speech synthesis supporting Arabic, English, and other languages
     * with language detection, onStart and onDone event callbacks.
     */
    fun speakText(
        text: String,
        languageCode: String? = null,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        onStartCallback = onStart
        onDoneCallback = onDone
        if (!isInitialized || tts == null) {
            onStart?.invoke()
            onDone?.invoke()
            return
        }
        try {
            val targetLocale = when (languageCode?.lowercase()) {
                "en" -> Locale.ENGLISH
                "ar" -> Locale("ar")
                "fr" -> Locale.FRENCH
                "tr" -> Locale("tr")
                "es" -> Locale("es")
                "de" -> Locale.GERMAN
                else -> {
                    val hasArabic = text.any { it in '\u0600'..'\u06FF' }
                    if (hasArabic) Locale("ar") else Locale.ENGLISH
                }
            }
            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            tts?.setSpeechRate(0.95f)
            val utteranceId = "tts_chat_${System.currentTimeMillis()}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak error", e)
            onDone?.invoke()
        }
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun stop() {
        try {
            onStartCallback = null
            onDoneCallback = null
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            onStartCallback = null
            onDoneCallback = null
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}

