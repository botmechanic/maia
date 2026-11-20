package com.example.feature.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.character.SystemPromptProvider
import com.example.core.contract.MaiaModule
import com.example.core.contract.MemoryRepository
import com.example.core.contract.ModelRuntime
import com.example.core.contract.ModuleRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val memoryRepository: MemoryRepository,
    private val modelRuntime: ModelRuntime,
    private val promptProvider: SystemPromptProvider,
    private val sharedPrefs: SharedPreferences,
    private val moduleRegistry: ModuleRegistry,
    private val onChatWiped: (String) -> Unit
) : AndroidViewModel(application) {

    private val _darkThemeEnabled = MutableStateFlow(sharedPrefs.getBoolean("dark_theme", false))
    val darkThemeEnabled: StateFlow<Boolean> = _darkThemeEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
        if (mode == "dark") {
            _darkThemeEnabled.value = true
            sharedPrefs.edit().putBoolean("dark_theme", true).apply()
        } else if (mode == "light") {
            _darkThemeEnabled.value = false
            sharedPrefs.edit().putBoolean("dark_theme", false).apply()
        }
    }

    private val _developerModeEnabled = MutableStateFlow(sharedPrefs.getBoolean("developer_mode", false))
    val developerModeEnabled: StateFlow<Boolean> = _developerModeEnabled.asStateFlow()

    fun unlockDeveloperMode() {
        _developerModeEnabled.value = true
        sharedPrefs.edit().putBoolean("developer_mode", true).apply()
    }

    private val _companionTone = MutableStateFlow(sharedPrefs.getString("companion_tone", "warm") ?: "warm")
    val companionTone: StateFlow<String> = _companionTone.asStateFlow()

    fun setCompanionTone(tone: String) {
        _companionTone.value = tone
        sharedPrefs.edit().putString("companion_tone", tone).apply()
    }

    private val _autoSpeakResponses = MutableStateFlow(sharedPrefs.getBoolean("auto_speak_responses", false))
    val autoSpeakResponses: StateFlow<Boolean> = _autoSpeakResponses.asStateFlow()

    fun toggleAutoSpeakResponses(enabled: Boolean) {
        _autoSpeakResponses.value = enabled
        sharedPrefs.edit().putBoolean("auto_speak_responses", enabled).apply()
    }

    private val _biometricLockEnabled = MutableStateFlow(sharedPrefs.getBoolean("biometric_lock_enabled", false))
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    fun toggleBiometricLock(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        sharedPrefs.edit().putBoolean("biometric_lock_enabled", enabled).apply()
    }

    private val _similarityThreshold = MutableStateFlow(sharedPrefs.getFloat("similarity_threshold", 0.35f))
    val similarityThreshold: StateFlow<Float> = _similarityThreshold.asStateFlow()

    private val _showInferenceTelemetry = MutableStateFlow(sharedPrefs.getBoolean("show_telemetry", true))
    val showInferenceTelemetry: StateFlow<Boolean> = _showInferenceTelemetry.asStateFlow()

    fun toggleInferenceTelemetry(enabled: Boolean) {
        _showInferenceTelemetry.value = enabled
        sharedPrefs.edit().putBoolean("show_telemetry", enabled).apply()
    }

    val selectedModelName = modelRuntime.selectedModelName
    val isModelMissing = modelRuntime.isModelMissing
    val isEmbeddingDegraded = memoryRepository.isEmbeddingDegraded

    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()

    val registeredModules: List<MaiaModule> get() = moduleRegistry.all()

    fun toggleDarkTheme(enabled: Boolean) {
        _darkThemeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun updateSimilarityThreshold(threshold: Float) {
        _similarityThreshold.value = threshold
        sharedPrefs.edit().putFloat("similarity_threshold", threshold).apply()
    }

    fun changeModel(modelName: String) {
        viewModelScope.launch {
            _isModelLoading.value = true
            sharedPrefs.edit().putString("selected_model", modelName).apply()
            modelRuntime.initializeModel(modelName)
            _isModelLoading.value = false
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepository.clearAllData()
            val greeting = promptProvider.wipeConfirmationMessage()
            onChatWiped(greeting)
            memoryRepository.addEpisodicMemory(
                "User wiped database and reset conversation history.",
                "OBSERVATION",
                moduleId = "settings"
            )
        }
    }

    fun rebuildMemoryIndex() {
        memoryRepository.scheduleImmediateIndexRebuild()
    }

    fun triggerImmediateDistillation() {
        memoryRepository.scheduleImmediateDistillation()
    }

    fun loadDemoScenario() {
        viewModelScope.launch {
            memoryRepository.loadDemoPersona()
            val intro = "Hello Alex. I've prepared your keynote research context and personal notes inside my encrypted vault. What shall we review first?"
            onChatWiped(intro)
        }
    }

    fun getEncryptedDatabaseSizeBytes(): Long {
        val dbFile = getApplication<Application>().getDatabasePath("maia_secure.db")
        return if (dbFile.exists()) dbFile.length() else 0L
    }

    fun getKeySecurityLevelDescription(): String {
        return memoryRepository.getKeySecurityLevelDescription()
    }

    fun importModelFromUri(uri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val contentResolver = app.contentResolver

                var displayName = "gemma3-1b-it-int4.task"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIndex)?.let { displayName = it }
                    }
                }

                val targetDir = app.getExternalFilesDir("models") ?: java.io.File(app.filesDir, "models")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val destFile = java.io.File(targetDir, displayName)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                _isModelLoading.value = true
                val currentModel = sharedPrefs.getString("selected_model", "Gemma 1B") ?: "Gemma 1B"
                modelRuntime.initializeModel(currentModel)
                _isModelLoading.value = false

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true, "Model $displayName imported successfully.")
                }
            } catch (e: Exception) {
                _isModelLoading.value = false
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Failed to import model: ${e.message}")
                }
            }
        }
    }
}
