package com.example.feature.chat

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.character.SystemPromptProvider
import com.example.core.contract.EmbeddingService
import com.example.core.contract.MemoryRepository
import com.example.core.contract.ModelRuntime
import com.example.core.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application,
    private val memoryRepository: MemoryRepository,
    private val modelRuntime: ModelRuntime,
    private val embeddingService: EmbeddingService,
    private val promptProvider: SystemPromptProvider,
    private val sharedPrefs: android.content.SharedPreferences,
    private val onboardingCompleted: StateFlow<Boolean>,
    private val sessionStore: ConversationSessionStore
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    val isModelReady = modelRuntime.isReady
    val isModelMissing = modelRuntime.isModelMissing
    val selectedModelName = modelRuntime.selectedModelName

    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()

    val isEmbeddingDegraded: StateFlow<Boolean> = memoryRepository.isEmbeddingDegraded

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isMaiaThinking = MutableStateFlow(false)
    val isMaiaThinking: StateFlow<Boolean> = _isMaiaThinking.asStateFlow()

    private val _showInferenceTelemetry = MutableStateFlow(sharedPrefs.getBoolean("show_telemetry", true))
    val showInferenceTelemetry: StateFlow<Boolean> = _showInferenceTelemetry.asStateFlow()

    private val sessionMetricsByContent = java.util.concurrent.ConcurrentHashMap<String, com.example.core.model.InferenceMetrics>()

    private val _lastInferenceMetrics = MutableStateFlow<com.example.core.model.InferenceMetrics?>(null)
    val lastInferenceMetrics: StateFlow<com.example.core.model.InferenceMetrics?> = _lastInferenceMetrics.asStateFlow()

    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "show_telemetry") {
            _showInferenceTelemetry.value = prefs.getBoolean("show_telemetry", true)
        }
    }

    fun toggleTelemetry(show: Boolean) {
        _showInferenceTelemetry.value = show
        sharedPrefs.edit().putBoolean("show_telemetry", show).apply()
    }

    val voiceManager = MaiaVoiceManager(application.applicationContext)
    val isSpeaking = voiceManager.isSpeaking

    private val _autoSpeak = MutableStateFlow(sharedPrefs.getBoolean("auto_speak_responses", false))
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    fun toggleAutoSpeak(enabled: Boolean) {
        _autoSpeak.value = enabled
        sharedPrefs.edit().putBoolean("auto_speak_responses", enabled).apply()
        if (!enabled) {
            voiceManager.stop()
        }
    }

    fun speakMessage(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stop()
    }

    private val _showNewConversationConfirm = MutableStateFlow(false)
    val showNewConversationConfirm: StateFlow<Boolean> = _showNewConversationConfirm.asStateFlow()

    private val activeProfileMemories = memoryRepository.allActiveProfileMemories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val episodicMemories = memoryRepository.allEpisodicMemories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
        viewModelScope.launch {
            _isModelLoading.value = true
            embeddingService.initializeEmbedder()
            val currentModel = sharedPrefs.getString("selected_model", "Gemma 1B") ?: "Gemma 1B"
            modelRuntime.initializeModel(currentModel)
            _isModelLoading.value = false
            if (onboardingCompleted.value) loadChatHistory()
        }
        viewModelScope.launch {
            isModelMissing.collectLatest {
                if (onboardingCompleted.value) loadChatHistory()
            }
        }
    }

    fun retryModelInitialization() {
        viewModelScope.launch {
            _isModelLoading.value = true
            embeddingService.initializeEmbedder()
            val currentModel = sharedPrefs.getString("selected_model", "Gemma 1B") ?: "Gemma 1B"
            modelRuntime.initializeModel(currentModel)
            _isModelLoading.value = false
            loadChatHistory()
        }
    }

    fun requestNewConversation() {
        _showNewConversationConfirm.value = true
    }

    fun dismissNewConversationConfirm() {
        _showNewConversationConfirm.value = false
    }

    fun confirmNewConversation() {
        _showNewConversationConfirm.value = false
        sessionStore.startNewSession()
        _chatMessages.value = listOf(
            ChatMessage(
                id = -1,
                sender = "MAIA",
                text = "Fresh page. I still remember you — this is just a new thread between us.",
                sessionId = sessionStore.currentSessionId
            )
        )
    }

    private suspend fun loadChatHistory() {
        episodicMemories.collectLatest { list ->
            val sessionId = sessionStore.currentSessionId
            val filtered = list.filter {
                (it.type == "USER" || it.type == "MAIA") && it.sessionId == sessionId
            }
            if (filtered.isEmpty()) {
                _chatMessages.value = listOf(
                    ChatMessage(
                        id = -1,
                        sender = "MAIA",
                        text = promptProvider.greeting(isModelMissing.value, isEmbeddingDegraded.value),
                        sessionId = sessionId
                    )
                )
            } else {
                val mapped = filtered.map { episodic ->
                    val associatedFact = activeProfileMemories.value.firstOrNull { fact ->
                        episodic.content.contains(fact.factValue, ignoreCase = true) ||
                            episodic.content.contains(fact.factKey, ignoreCase = true)
                    }
                    ChatMessage(
                        id = episodic.id,
                        sender = episodic.type,
                        text = episodic.content,
                        timestamp = episodic.timestamp,
                        memoryUsed = associatedFact?.let { "Recall: ${it.factKey} = ${it.factValue}" },
                        sessionId = episodic.sessionId,
                        metrics = sessionMetricsByContent[episodic.content]
                    )
                }
                _chatMessages.value = if (isModelMissing.value) {
                    mapped + ChatMessage(
                        id = System.currentTimeMillis(),
                        sender = "MAIA",
                        text = promptProvider.greeting(isModelMissing = true, isEmbeddingDegraded = false),
                        sessionId = sessionId
                    )
                } else mapped
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val sessionId = sessionStore.currentSessionId
            _isMaiaThinking.value = true
            val request = memoryRepository.buildChatGenerationRequest(
                userQuery = text,
                systemPromptTemplate = promptProvider.load(),
                sessionId = sessionId,
                maxHistoryTurns = 8
            )
            memoryRepository.addEpisodicMemory(text, "USER", moduleId = "chat", sessionId = sessionId)
            val matched = memoryRepository.querySemanticMemories(text, limit = 1).firstOrNull()
            val matchedFact = activeProfileMemories.value.firstOrNull { fact ->
                text.contains(fact.factKey, ignoreCase = true) || text.contains(fact.factValue, ignoreCase = true)
            }
            val memoryLabel = when {
                matchedFact != null -> "Core Profile: ${matchedFact.factKey} = ${matchedFact.factValue}"
                matched != null -> "Episodic: \"${matched.episodic.content.take(50)}\" (${(matched.score * 100).toInt()}% match)"
                else -> null
            }
            val maiaMessageId = System.currentTimeMillis()
            var currentResponseText = ""
            _chatMessages.value = _chatMessages.value + ChatMessage(
                id = maiaMessageId,
                sender = "MAIA",
                text = "",
                memoryUsed = memoryLabel,
                sessionId = sessionId
            )
            val startGenTime = System.currentTimeMillis()
            var firstTokenTime: Long? = null
            var chunkCount = 0

            if (CrisisIntervention.isCrisisDisclosure(text)) {
                _isMaiaThinking.value = false
                currentResponseText = CrisisIntervention.CRISIS_RESPONSE
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == maiaMessageId) msg.copy(text = currentResponseText) else msg
                }
            } else {
                try {
                    modelRuntime.generateChatStreaming(request).collect { token ->
                        if (firstTokenTime == null) {
                            firstTokenTime = System.currentTimeMillis()
                        }
                        chunkCount++
                        _isMaiaThinking.value = false
                        currentResponseText += token
                        _chatMessages.value = _chatMessages.value.map { msg ->
                            if (msg.id == maiaMessageId) msg.copy(text = currentResponseText) else msg
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating chat response: ${e.message}", e)
                } finally {
                    _isMaiaThinking.value = false
                }
            }

            if (currentResponseText.isBlank()) {
                val fallbackText = "I'm here, but I lost my train of thought. Could you say that again?"
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == maiaMessageId) msg.copy(text = fallbackText) else msg
                }
                currentResponseText = fallbackText
            }

            val totalDurationMs = (System.currentTimeMillis() - startGenTime).coerceAtLeast(1)
            val ttftMs = (firstTokenTime ?: System.currentTimeMillis()) - startGenTime
            val wordCount = currentResponseText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val estimatedTokens = maxOf(chunkCount, (wordCount * 1.3).toInt()).coerceAtLeast(1)
            val tokPerSec = (estimatedTokens.toFloat() / (totalDurationMs.toFloat() / 1000f))

            val inferenceMetrics = com.example.core.model.InferenceMetrics(
                timeToFirstTokenMs = ttftMs,
                totalGenerationTimeMs = totalDurationMs,
                tokenCount = estimatedTokens,
                tokensPerSecond = tokPerSec
            )
            sessionMetricsByContent[currentResponseText] = inferenceMetrics
            _lastInferenceMetrics.value = inferenceMetrics

            _chatMessages.value = _chatMessages.value.map { msg ->
                if (msg.id == maiaMessageId) msg.copy(metrics = inferenceMetrics) else msg
            }

            memoryRepository.addEpisodicMemory(
                currentResponseText,
                "MAIA",
                moduleId = "chat",
                sessionId = sessionId
            )
            performHapticTick()

            if (_autoSpeak.value && currentResponseText.isNotBlank()) {
                voiceManager.speak(currentResponseText)
            }

            val newEventsCount = sharedPrefs.getInt("new_events_count", 0) + 1
            if (newEventsCount >= 5) {
                memoryRepository.scheduleDistillation()
                sharedPrefs.edit().putInt("new_events_count", 0).apply()
            } else {
                sharedPrefs.edit().putInt("new_events_count", newEventsCount).apply()
            }
        }
    }

    fun resetChatAfterWipe(greeting: String) {
        sessionStore.startNewSession()
        _chatMessages.value = listOf(
            ChatMessage(
                id = -1,
                sender = "MAIA",
                text = greeting,
                sessionId = sessionStore.currentSessionId
            )
        )
    }

    private fun performHapticTick() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (_: Exception) {
            Log.w(TAG, "Haptic feedback unavailable.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        voiceManager.shutdown()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
