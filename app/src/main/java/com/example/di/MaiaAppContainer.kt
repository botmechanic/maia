package com.example.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.character.SystemPromptProvider
import com.example.core.contract.ModuleContext
import com.example.core.contract.ModuleRegistry
import com.example.feature.chat.ConversationSessionStore
import com.example.core.memory.MaiaServices
import com.example.core.memory.RoomMemoryRepository
import com.example.core.memory.StreamingModelAccess
import com.example.core.memory.worker.DistillationWorker
import com.example.core.runtime.MediaPipeEmbeddingService
import com.example.core.runtime.MediaPipeModelRuntime
import com.example.feature.chat.ChatModule
import com.example.feature.chat.ChatViewModel
import com.example.feature.memoryview.MemoryViewModel
import com.example.feature.memoryview.MemoryViewModule
import com.example.feature.settings.SettingsModule
import com.example.feature.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log

class MaiaAppContainer(private val app: Application) {

    val sharedPrefs = app.getSharedPreferences("maia_prefs", Context.MODE_PRIVATE)
    val sessionStore = ConversationSessionStore(sharedPrefs).also { it.startNewSession() }
    val promptProvider = SystemPromptProvider(app)
    val embeddingService = MediaPipeEmbeddingService(app)
    val modelRuntime = MediaPipeModelRuntime(app)
    val memoryRepository = RoomMemoryRepository(app, embeddingService)

    private val chatModule = ChatModule()
    private val memoryViewModule = MemoryViewModule()
    private val settingsModule = SettingsModule()

    val moduleRegistry = ModuleRegistry(
        listOf(chatModule, memoryViewModule, settingsModule)
    )

    private val _onboardingCompleted =
        MutableStateFlow(sharedPrefs.getBoolean("onboarding_completed", false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _userName =
        MutableStateFlow(sharedPrefs.getString("user_name", "Friend") ?: "Friend")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _onboardingStep = MutableStateFlow(1)
    val onboardingStep: StateFlow<Int> = _onboardingStep.asStateFlow()

    private val _darkThemeEnabled =
        MutableStateFlow(sharedPrefs.getBoolean("dark_theme", false))
    val darkThemeEnabled: StateFlow<Boolean> = _darkThemeEnabled.asStateFlow()

    private val _themeMode =
        MutableStateFlow(sharedPrefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private var chatViewModelRef: java.lang.ref.WeakReference<ChatViewModel>? = null
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        MaiaServices.modelRuntime = modelRuntime
        MaiaServices.embeddingService = embeddingService
        MaiaServices.memoryRepository = memoryRepository
        DistillationWorker.schedulePeriodic(app)
        appScope.launch {
            embeddingService.initializeEmbedder()
        }
    }

    fun moduleContextFor(moduleId: String): ModuleContext =
        ModuleContext(
            memory = memoryRepository.writerFor(moduleId),
            runtime = StreamingModelAccess(modelRuntime)
        )

    fun setOnboardingStep(step: Int) {
        _onboardingStep.value = step
    }

    fun updateUserName(name: String) {
        _userName.value = name
    }

    fun completeOnboarding() {
        sharedPrefs.edit()
            .putBoolean("onboarding_completed", true)
            .putString("user_name", _userName.value)
            .apply()
        _onboardingCompleted.value = true
    }

    fun toggleDarkTheme(enabled: Boolean) {
        _darkThemeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
        // Maintain backwards compatibility with legacy dark_theme boolean
        if (mode == "dark") {
            _darkThemeEnabled.value = true
            sharedPrefs.edit().putBoolean("dark_theme", true).apply()
        } else if (mode == "light") {
            _darkThemeEnabled.value = false
            sharedPrefs.edit().putBoolean("dark_theme", false).apply()
        }
    }

    fun chatFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val vm = ChatViewModel(
                application = app,
                memoryRepository = memoryRepository,
                modelRuntime = modelRuntime,
                embeddingService = embeddingService,
                promptProvider = promptProvider,
                sharedPrefs = sharedPrefs,
                onboardingCompleted = onboardingCompleted,
                sessionStore = sessionStore
            )
            chatViewModelRef = java.lang.ref.WeakReference(vm)
            return vm as T
        }
    }

    fun memoryFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MemoryViewModel(app, memoryRepository) as T
        }
    }

    fun settingsFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                application = app,
                memoryRepository = memoryRepository,
                modelRuntime = modelRuntime,
                promptProvider = promptProvider,
                sharedPrefs = sharedPrefs,
                moduleRegistry = moduleRegistry,
                onChatWiped = { greeting -> chatViewModelRef?.get()?.resetChatAfterWipe(greeting) }
            ) as T
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MaiaAppContainer? = null

        fun get(app: Application): MaiaAppContainer =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MaiaAppContainer(app).also { INSTANCE = it }
            }
    }
}
