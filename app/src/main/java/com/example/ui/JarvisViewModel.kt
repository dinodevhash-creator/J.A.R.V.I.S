package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.ai.ActionType
import com.example.ai.FirebaseJarvisAiRepository
import com.example.ai.JarvisAiRepository
import com.example.ai.JarvisImageRepository
import com.example.ai.StudioAspectRatio
import com.example.data.cloud.CloudDirective
import com.example.data.cloud.CloudMessage
import com.example.data.cloud.JarvisCloudRepository
import com.example.data.preferences.JarvisPersona
import com.example.data.preferences.JarvisPreferencesRepository
import com.example.data.preferences.JarvisSettings
import com.example.system.BriefingScheduler
import com.example.system.DeviceActionExecutor
import com.example.system.DeviceTelemetry
import com.example.system.ImageUtils
import com.example.system.LocationInfo
import com.example.system.LocationManager
import com.example.system.VoiceManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "JarvisViewModel"

enum class JarvisCoreStatus {
    STANDBY,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

sealed interface ImageStudioState {
    data object Idle : ImageStudioState
    data object Loading : ImageStudioState
    data class Success(val bitmap: Bitmap, val prompt: String) : ImageStudioState
    data class Error(val message: String) : ImageStudioState
}

data class JarvisUiState(
    val userEmail: String = "",
    val userDisplayName: String = "",
    val messages: List<CloudMessage> = emptyList(),
    val directives: List<CloudDirective> = emptyList(),
    val settings: JarvisSettings = JarvisSettings(),
    val telemetry: DeviceTelemetry = DeviceTelemetry(100, false, "ONLINE", "ANDROID"),
    val locationInfo: LocationInfo? = null,
    val isFetchingLocation: Boolean = false,
    val dailyBriefingText: String? = null,
    val isGeneratingBriefing: Boolean = false,
    val studioState: ImageStudioState = ImageStudioState.Idle,
    val studioReferenceUri: Uri? = null,
    val coreStatus: JarvisCoreStatus = JarvisCoreStatus.STANDBY,
    val streamingText: String = "",
    val isGenerating: Boolean = false,
    val isListening: Boolean = false,
    val isHandsFreeMode: Boolean = false,
    val partialTranscript: String = "",
    val voiceAmplitude: Float = 0f,
    val isSpeaking: Boolean = false,
    val ttsReady: Boolean = false,
    val attachedImageUri: Uri? = null,
    val errorMessage: String? = null,
    val statusBanner: String? = null
)

class JarvisViewModel(
    private val appContext: Context,
    private val currentUserId: String,
    private val userEmail: String,
    private val userDisplayName: String,
    private val cloudRepo: JarvisCloudRepository,
    private val prefsRepo: JarvisPreferencesRepository,
    private val aiRepo: JarvisAiRepository,
    private val imageRepo: JarvisImageRepository,
    private val voiceManager: VoiceManager
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    private val _streamingText = MutableStateFlow("")
    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _statusBanner = MutableStateFlow<String?>(null)
    private val _telemetry = MutableStateFlow(DeviceActionExecutor.readTelemetry(appContext))
    private val _isHandsFreeMode = MutableStateFlow(false)
    private val _locationInfo = MutableStateFlow<LocationInfo?>(null)
    private val _isFetchingLocation = MutableStateFlow(false)
    private val _dailyBriefingText = MutableStateFlow<String?>(null)
    private val _isGeneratingBriefing = MutableStateFlow(false)
    private val _studioState = MutableStateFlow<ImageStudioState>(ImageStudioState.Idle)
    private val _studioReferenceUri = MutableStateFlow<Uri?>(null)

    private var activeGenerationJob: Job? = null

    private val messagesFlow = cloudRepo.observeMessages(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing cloud messages", rootError)
            _errorMessage.value = rootError.localizedMessage ?: "Gagal menyinkronkan riwayat chat."
            emit(emptyList())
        }

    private val directivesFlow = cloudRepo.observeDirectives(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing cloud directives", rootError)
            _errorMessage.value = rootError.localizedMessage ?: "Gagal menyinkronkan direktif."
            emit(emptyList())
        }

    private val voiceSnapshotFlow = combine(
        voiceManager.isListening,
        voiceManager.partialTranscript,
        voiceManager.voiceAmplitude,
        voiceManager.isSpeaking,
        voiceManager.ttsReady
    ) { listening, partial, amplitude, speaking, ttsReady ->
        VoiceSnapshot(listening, partial, amplitude, speaking, ttsReady)
    }

    private val interactionSnapshotFlow = combine(
        _isGenerating,
        _streamingText,
        _attachedImageUri,
        _errorMessage,
        _statusBanner
    ) { generating, streaming, imageUri, error, banner ->
        InteractionSnapshot(generating, streaming, imageUri, error, banner)
    }

    private val extendedSnapshotFlow = combine(
        _isHandsFreeMode,
        _locationInfo,
        _isFetchingLocation,
        _dailyBriefingText,
        _isGeneratingBriefing
    ) { handsFree, location, fetchingLoc, briefing, genBriefing ->
        ExtendedSnapshot(handsFree, location, fetchingLoc, briefing, genBriefing)
    }

    private val studioSnapshotFlow = combine(
        _studioState,
        _studioReferenceUri,
        _telemetry
    ) { studio, refUri, telemetry ->
        StudioSnapshot(studio, refUri, telemetry)
    }

    val uiState: StateFlow<JarvisUiState> = combine(
        messagesFlow,
        directivesFlow,
        prefsRepo.settings,
        voiceSnapshotFlow,
        interactionSnapshotFlow,
        extendedSnapshotFlow,
        studioSnapshotFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val messages = values[0] as List<CloudMessage>
        @Suppress("UNCHECKED_CAST")
        val directives = values[1] as List<CloudDirective>
        val settings = values[2] as JarvisSettings
        val voice = values[3] as VoiceSnapshot
        val interaction = values[4] as InteractionSnapshot
        val extended = values[5] as ExtendedSnapshot
        val studio = values[6] as StudioSnapshot

        val status = when {
            voice.isListening -> JarvisCoreStatus.LISTENING
            interaction.isGenerating -> JarvisCoreStatus.THINKING
            voice.isSpeaking -> JarvisCoreStatus.SPEAKING
            interaction.errorMessage != null -> JarvisCoreStatus.ERROR
            else -> JarvisCoreStatus.STANDBY
        }

        JarvisUiState(
            userEmail = userEmail,
            userDisplayName = userDisplayName,
            messages = messages,
            directives = directives,
            settings = settings,
            telemetry = studio.telemetry,
            locationInfo = extended.locationInfo,
            isFetchingLocation = extended.isFetchingLocation,
            dailyBriefingText = extended.dailyBriefingText,
            isGeneratingBriefing = extended.isGeneratingBriefing,
            studioState = studio.studioState,
            studioReferenceUri = studio.studioReferenceUri,
            coreStatus = status,
            streamingText = interaction.streamingText,
            isGenerating = interaction.isGenerating,
            isListening = voice.isListening,
            isHandsFreeMode = extended.isHandsFreeMode,
            partialTranscript = voice.partialTranscript,
            voiceAmplitude = voice.voiceAmplitude,
            isSpeaking = voice.isSpeaking,
            ttsReady = voice.ttsReady,
            attachedImageUri = interaction.attachedImageUri,
            errorMessage = interaction.errorMessage,
            statusBanner = interaction.statusBanner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = JarvisUiState(
            userEmail = userEmail,
            userDisplayName = userDisplayName
        )
    )

    init {
        viewModelScope.launch {
            prefsRepo.settings.collect { settings ->
                voiceManager.updateSettings(settings.languageCode, settings.speechRate)
            }
        }
        viewModelScope.launch {
            voiceManager.voiceError.collect { err ->
                if (!err.isNullOrBlank()) {
                    _statusBanner.value = err
                    voiceManager.clearVoiceError()
                    if (_isHandsFreeMode.value) {
                        delay(700L)
                        if (_isHandsFreeMode.value && !voiceManager.isListening.value && !_isGenerating.value) {
                            startVoiceListening()
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            voiceManager.utteranceCompleted.collect {
                if (_isHandsFreeMode.value && !_isGenerating.value) {
                    delay(350L)
                    if (_isHandsFreeMode.value && !voiceManager.isListening.value && !_isGenerating.value) {
                        startVoiceListening()
                    }
                }
            }
        }
    }

    fun refreshTelemetry() {
        _telemetry.value = DeviceActionExecutor.readTelemetry(appContext)
    }

    fun attachImage(uri: Uri?) {
        _attachedImageUri.value = uri
    }

    fun setStudioReferenceImage(uri: Uri?) {
        _studioReferenceUri.value = uri
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showNotice(message: String) {
        _statusBanner.value = message
    }

    fun setHandsFreeMode(enabled: Boolean) {
        _isHandsFreeMode.value = enabled
        if (enabled) {
            _statusBanner.value = "Mode Dialog Suara Langsung aktif."
            startVoiceListening()
        } else {
            voiceManager.stopListening()
            voiceManager.stopSpeaking()
            _statusBanner.value = "Mode Dialog Suara Langsung dihentikan."
        }
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        val imageUri = _attachedImageUri.value
        if (prompt.isEmpty() && imageUri == null) return

        activeGenerationJob?.cancel()
        _errorMessage.value = null
        _attachedImageUri.value = null
        voiceManager.stopSpeaking()

        activeGenerationJob = viewModelScope.launch {
            val currentSettings = uiState.value.settings
            val historySnapshot = uiState.value.messages
            val displayPrompt = prompt.ifBlank { "Analisis lampiran gambar ini." }

            cloudRepo.addMessage(
                userId = currentUserId,
                role = "user",
                text = displayPrompt
            ).onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Gagal menyimpan pesan ke cloud."
            }

            _isGenerating.value = true
            _streamingText.value = ""

            try {
                val bitmap = if (imageUri != null) {
                    ImageUtils.decodeScaledBitmap(appContext, imageUri)
                } else {
                    null
                }

                val actionDeferred = async {
                    if (prompt.isNotBlank()) aiRepo.detectActionCommand(prompt) else null
                }

                var finalReply = ""
                aiRepo.streamResponse(
                    prompt = displayPrompt,
                    history = historySnapshot,
                    attachedImage = bitmap,
                    persona = currentSettings.persona,
                    languageCode = currentSettings.languageCode,
                    locationInfo = _locationInfo.value
                ).collect { partial ->
                    finalReply = partial
                    _streamingText.value = partial
                }

                val detectedAction = actionDeferred.await()
                if (detectedAction != null && detectedAction.type == ActionType.ADD_DIRECTIVE) {
                    val title = runCatching {
                        JSONObject(detectedAction.payloadJson).optString("query")
                    }.getOrNull().orEmpty().ifBlank { displayPrompt }
                    cloudRepo.addDirective(currentUserId, title = title, priority = "HIGH")
                    _statusBanner.value = "Protokol baru disinkronkan: $title"
                }

                cloudRepo.addMessage(
                    userId = currentUserId,
                    role = "model",
                    text = finalReply,
                    actionType = detectedAction?.type?.name,
                    actionLabel = detectedAction?.label,
                    actionPayload = detectedAction?.payloadJson
                )

                _streamingText.value = ""
                _isGenerating.value = false

                if ((currentSettings.autoSpeakEnabled || _isHandsFreeMode.value) && finalReply.isNotBlank()) {
                    voiceManager.speak(finalReply)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini request timed out", e)
                val msg = "Waktu pemrosesan J.A.R.V.I.S. habis (60 detik). Silakan coba lagi."
                _errorMessage.value = msg
                cloudRepo.addMessage(currentUserId, role = "model", text = msg, errorFlag = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini request failed", e)
                val msg = e.localizedMessage ?: "Gagal terhubung ke inti AI J.A.R.V.I.S."
                _errorMessage.value = msg
                cloudRepo.addMessage(currentUserId, role = "model", text = msg, errorFlag = true)
            } finally {
                _isGenerating.value = false
                _streamingText.value = ""
            }
        }
    }

    fun startVoiceListening() {
        val lang = uiState.value.settings.languageCode
        voiceManager.startListening(lang) { recognizedText ->
            sendMessage(recognizedText)
        }
    }

    fun stopVoiceListening() {
        voiceManager.stopListening()
    }

    fun speakText(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun executeAction(actionType: String, payloadJson: String) {
        val result = DeviceActionExecutor.executeAction(
            appContext,
            actionType,
            payloadJson,
            _locationInfo.value
        )
        result.onSuccess { feedback ->
            _statusBanner.value = feedback
        }.onFailure { err ->
            _statusBanner.value = err.localizedMessage ?: "Gagal menjalankan perintah sistem."
        }
    }

    fun triggerQuickAlarm(hour: Int, minute: Int, label: String) {
        DeviceActionExecutor.launchAlarm(appContext, hour, minute, label)
            .onSuccess { _statusBanner.value = it }
            .onFailure { _statusBanner.value = it.localizedMessage }
    }

    fun triggerQuickTimer(seconds: Int, label: String) {
        DeviceActionExecutor.launchTimer(appContext, seconds, label)
            .onSuccess { _statusBanner.value = it }
            .onFailure { _statusBanner.value = it.localizedMessage }
    }

    fun triggerQuickWebSearch(query: String) {
        DeviceActionExecutor.launchWebSearch(appContext, query)
            .onSuccess { _statusBanner.value = it }
            .onFailure { _statusBanner.value = it.localizedMessage }
    }

    fun triggerQuickCalendar(title: String) {
        DeviceActionExecutor.launchCalendarInsert(appContext, title)
            .onSuccess { _statusBanner.value = it }
            .onFailure { _statusBanner.value = it.localizedMessage }
    }

    fun openMapsSearch(query: String) {
        DeviceActionExecutor.launchMapsSearch(appContext, query, _locationInfo.value)
            .onSuccess { _statusBanner.value = it }
            .onFailure { _statusBanner.value = it.localizedMessage }
    }

    fun refreshLocation() {
        if (_isFetchingLocation.value) return
        viewModelScope.launch {
            _isFetchingLocation.value = true
            LocationManager.fetchCurrentLocation(appContext)
                .onSuccess { info ->
                    _locationInfo.value = info
                    _statusBanner.value = "Lokasi terdeteksi: ${info.city}"
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Gagal mendeteksi lokasi."
                }
            _isFetchingLocation.value = false
        }
    }

    fun generateDailyBriefing(speakAloud: Boolean = true) {
        if (_isGeneratingBriefing.value) return
        viewModelScope.launch {
            _isGeneratingBriefing.value = true
            try {
                val settings = uiState.value.settings
                val briefing = aiRepo.generateDailyBriefing(
                    directives = uiState.value.directives,
                    locationInfo = _locationInfo.value,
                    telemetry = _telemetry.value,
                    persona = settings.persona,
                    languageCode = settings.languageCode
                )
                _dailyBriefingText.value = briefing
                if (speakAloud && settings.autoSpeakEnabled) {
                    voiceManager.speak(briefing)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Briefing generation failed", e)
                _statusBanner.value = e.localizedMessage ?: "Gagal membuat ringkasan harian."
            } finally {
                _isGeneratingBriefing.value = false
            }
        }
    }

    fun setDailyBriefingSchedule(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setDailyBriefing(enabled, hour, minute)
            if (enabled) {
                BriefingScheduler.scheduleDailyBriefing(appContext, hour, minute)
                _statusBanner.value = "Pengingat harian dijadwalkan pukul %02d:%02d.".format(hour, minute)
            } else {
                BriefingScheduler.cancelDailyBriefing(appContext)
                _statusBanner.value = "Pengingat harian terjadwal dimatikan."
            }
        }
    }

    fun sendTestBriefingNotification() {
        val summary = _dailyBriefingText.value
            ?: "J.A.R.V.I.S. siap dengan ${uiState.value.directives.count { !it.completed }} direktif aktif Anda."
        BriefingScheduler.sendTestBriefingNotification(appContext, summary)
        _statusBanner.value = "Notifikasi ringkasan dikirim."
    }

    fun generateOrEditStudioImage(prompt: String, aspectRatio: StudioAspectRatio) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return
        val refUri = _studioReferenceUri.value

        viewModelScope.launch {
            _studioState.value = ImageStudioState.Loading
            try {
                val resultBitmap = if (refUri != null) {
                    val sourceBitmap = ImageUtils.decodeScaledBitmap(appContext, refUri)
                        ?: throw IllegalStateException("Gagal membaca gambar referensi.")
                    imageRepo.editImage(sourceBitmap, cleanPrompt, aspectRatio)
                } else {
                    imageRepo.generateImage(cleanPrompt, aspectRatio)
                }
                _studioState.value = ImageStudioState.Success(resultBitmap, cleanPrompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Studio image error", e)
                _studioState.value = ImageStudioState.Error(
                    e.localizedMessage ?: "Gagal memproses gambar AI."
                )
            }
        }
    }

    fun saveGeneratedImageToGallery() {
        val current = _studioState.value as? ImageStudioState.Success ?: return
        viewModelScope.launch {
            LocationManager.saveBitmapToGallery(appContext, current.bitmap)
                .onSuccess {
                    _statusBanner.value = "Gambar berhasil disimpan ke Galeri (Pictures/JarvisAI)."
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Gagal menyimpan gambar."
                }
        }
    }

    fun addDirective(title: String, priority: String = "NORMAL") {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            cloudRepo.addDirective(currentUserId, clean, priority)
                .onSuccess { _statusBanner.value = "Direktif \"$clean\" disinkronkan ke cloud." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Gagal menyimpan direktif." }
        }
    }

    fun toggleDirective(directive: CloudDirective) {
        viewModelScope.launch {
            cloudRepo.toggleDirective(currentUserId, directive)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Gagal memperbarui direktif." }
        }
    }

    fun deleteDirective(id: String) {
        viewModelScope.launch {
            cloudRepo.deleteDirective(currentUserId, id)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Gagal menghapus direktif." }
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            voiceManager.stopSpeaking()
            cloudRepo.clearMessages(currentUserId)
                .onSuccess { _statusBanner.value = "Log percakapan cloud telah dibersihkan." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Gagal menghapus riwayat." }
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoSpeak(enabled)
            if (!enabled) voiceManager.stopSpeaking()
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            prefsRepo.setSpeechRate(rate)
        }
    }

    fun setLanguageCode(code: String) {
        viewModelScope.launch {
            prefsRepo.setLanguageCode(code)
        }
    }

    fun setPersona(persona: JarvisPersona) {
        viewModelScope.launch {
            prefsRepo.setPersona(persona)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _isHandsFreeMode.value = false
        voiceManager.release()
    }

    companion object {
        fun provideFactory(
            currentUserId: String,
            userEmail: String,
            userDisplayName: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val appCtx = app.applicationContext
                JarvisViewModel(
                    appContext = appCtx,
                    currentUserId = currentUserId,
                    userEmail = userEmail,
                    userDisplayName = userDisplayName,
                    cloudRepo = JarvisCloudRepository(db),
                    prefsRepo = JarvisPreferencesRepository(appCtx),
                    aiRepo = FirebaseJarvisAiRepository(),
                    imageRepo = JarvisImageRepository(),
                    voiceManager = VoiceManager(appCtx)
                )
            }
        }
    }

    private data class VoiceSnapshot(
        val isListening: Boolean,
        val partialTranscript: String,
        val voiceAmplitude: Float,
        val isSpeaking: Boolean,
        val ttsReady: Boolean
    )

    private data class InteractionSnapshot(
        val isGenerating: Boolean,
        val streamingText: String,
        val attachedImageUri: Uri?,
        val errorMessage: String?,
        val statusBanner: String?
    )

    private data class ExtendedSnapshot(
        val isHandsFreeMode: Boolean,
        val locationInfo: LocationInfo?,
        val isFetchingLocation: Boolean,
        val dailyBriefingText: String?,
        val isGeneratingBriefing: Boolean
    )

    private data class StudioSnapshot(
        val studioState: ImageStudioState,
        val studioReferenceUri: Uri?,
        val telemetry: DeviceTelemetry
    )
}
