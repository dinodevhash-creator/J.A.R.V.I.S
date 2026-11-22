package com.example.system

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "VoiceManager"

class VoiceManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var currentLanguageCode: String = "id-ID"
    private var currentSpeechRate: Float = 1.05f

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitude: StateFlow<Float> = _voiceAmplitude.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _ttsReady = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    private val _utteranceCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val utteranceCompleted: SharedFlow<Unit> = _utteranceCompleted.asSharedFlow()

    val isSpeechRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    applyTtsConfiguration(currentLanguageCode, currentSpeechRate)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            _utteranceCompleted.tryEmit(Unit)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                            _utteranceCompleted.tryEmit(Unit)
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                            _utteranceCompleted.tryEmit(Unit)
                        }
                    })
                    _ttsReady.value = true
                } else {
                    Log.w(TAG, "TextToSpeech initialization failed with status=$status")
                    _ttsReady.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TTS", e)
        }
    }

    fun updateSettings(languageCode: String, speechRate: Float) {
        currentLanguageCode = languageCode
        currentSpeechRate = speechRate
        if (_ttsReady.value) {
            applyTtsConfiguration(languageCode, speechRate)
        }
    }

    private fun applyTtsConfiguration(languageCode: String, speechRate: Float) {
        val engine = tts ?: return
        val targetLocale = Locale.forLanguageTag(languageCode)
        val result = engine.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setSpeechRate(speechRate)
    }

    fun speak(rawText: String) {
        val engine = tts
        if (engine == null || !_ttsReady.value) {
            _utteranceCompleted.tryEmit(Unit)
            return
        }
        val cleanText = rawText
            .replace(Regex("```[\\s\\S]*?```"), " blok kode ")
            .replace(Regex("[*#_`~>]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleanText.isEmpty()) {
            _utteranceCompleted.tryEmit(Unit)
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun clearVoiceError() {
        _voiceError.value = null
    }

    fun startListening(
        languageCode: String,
        onFinalResult: (String) -> Unit
    ) {
        if (!isSpeechRecognitionAvailable) {
            _voiceError.value = "Layanan pengenalan suara tidak tersedia di perangkat ini."
            return
        }

        stopSpeaking()
        _voiceError.value = null
        _partialTranscript.value = ""

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _voiceAmplitude.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _voiceAmplitude.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _voiceAmplitude.value = 0f
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Gagal merekam audio mikrofon."
                        SpeechRecognizer.ERROR_NO_MATCH -> "Suara tidak dikenali. Silakan coba lagi."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tidak ada suara terdeteksi."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Koneksi pengenalan suara bermasalah."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon belum diberikan."
                        else -> null
                    }
                    if (msg != null) {
                        _voiceError.value = msg
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _voiceAmplitude.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    _partialTranscript.value = ""
                    if (best.isNotEmpty()) {
                        onFinalResult(best)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            _voiceError.value = "Tidak dapat memulai mikrofon: ${e.localizedMessage}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _isListening.value = false
        _voiceAmplitude.value = 0f
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {
        }
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {
        }
    }
}
