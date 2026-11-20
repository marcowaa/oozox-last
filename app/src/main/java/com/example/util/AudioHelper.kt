package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

object AudioRecorderManager {
    private const val TAG = "AudioRecorderManager"
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var _isRecording = false
    val isRecording: Boolean get() = _isRecording

    fun startRecording(context: Context): File? {
        try {
            stopRecording() // Clean up any existing session

            val audioDir = File(context.cacheDir, "voice_notes").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording = true
            Log.d(TAG, "Audio recording started successfully: ${outputFile.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            _isRecording = false
            currentOutputFile = null
            mediaRecorder?.release()
            mediaRecorder = null
            return null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (_isRecording) {
                mediaRecorder?.maxAmplitude ?: 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording && mediaRecorder == null) return null
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w(TAG, "MediaRecorder stop failed (possibly too short)", e)
                }
                reset()
                release()
            }
            mediaRecorder = null
            _isRecording = false
            val file = currentOutputFile
            currentOutputFile = null
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
            mediaRecorder?.release()
            mediaRecorder = null
            _isRecording = false
            null
        }
    }

    fun cancelRecording() {
        try {
            val file = stopRecording()
            if (file != null && file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling recording", e)
        }
    }
}

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var currentlyPlayingPath: String? = null
        private set
    var isPlaying: Boolean = false
        private set

    fun play(
        context: Context,
        pathOrUri: String,
        onProgress: (currentMs: Int, totalMs: Int) -> Unit,
        onComplete: () -> Unit
    ) {
        try {
            stop()

            val player = MediaPlayer()
            if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                player.setDataSource(context, Uri.parse(pathOrUri))
            } else {
                val file = File(pathOrUri)
                if (file.exists()) {
                    player.setDataSource(file.absolutePath)
                } else {
                    player.setDataSource(context, Uri.parse(pathOrUri))
                }
            }

            player.prepare()
            val totalDuration = player.duration.coerceAtLeast(1000)
            player.start()

            mediaPlayer = player
            currentlyPlayingPath = pathOrUri
            isPlaying = true

            player.setOnCompletionListener {
                stop()
                onComplete()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                stop()
                onComplete()
                true
            }

            playbackJob = scope.launch {
                while (isActive && isPlaying && mediaPlayer != null) {
                    try {
                        val current = mediaPlayer?.currentPosition ?: 0
                        onProgress(current, totalDuration)
                    } catch (e: Exception) {
                        break
                    }
                    delay(100L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio playback for $pathOrUri", e)
            stop()
            onComplete()
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media player", e)
        }
        mediaPlayer = null
        currentlyPlayingPath = null
        isPlaying = false
    }
}
