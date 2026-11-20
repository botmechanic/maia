package com.example.feature.chat

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class MaiaVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech: ${e.message}", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Warm, grounded, calm cadence
                    engine.setPitch(0.92f)
                    engine.setSpeechRate(0.96f)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                        }
                    })
                    isInitialized = true
                    Log.d(TAG, "TextToSpeech initialized successfully with Maia voice profile.")
                }
            }
        } else {
            Log.w(TAG, "TextToSpeech initialization returned status: $status")
        }
    }

    fun speak(text: String) {
        if (!isInitialized || tts == null) return
        stop()
        val cleaned = sanitizeForSpeech(text)
        if (cleaned.isNotBlank()) {
            val utteranceId = "maia_${System.currentTimeMillis()}"
            tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "MaiaVoiceManager"

        fun sanitizeForSpeech(text: String): String {
            return text
                .replace(Regex("[*#_~`>]"), "")
                .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
                .replace(Regex("(?m)^[-*+]\\s+"), "")
                .replace("...", ".")
                .replace("  ", " ")
                .trim()
        }
    }
}

