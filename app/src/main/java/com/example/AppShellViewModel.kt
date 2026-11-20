package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.MaiaAppContainer
import kotlinx.coroutines.launch

/** App-shell ViewModel for onboarding and shared theme state. */
class AppShellViewModel(application: Application) : AndroidViewModel(application) {
    private val container = MaiaAppContainer.get(application)

    val onboardingCompleted = container.onboardingCompleted
    val onboardingStep = container.onboardingStep
    val userName = container.userName
    val darkThemeEnabled = container.darkThemeEnabled
    val themeMode = container.themeMode

    fun setOnboardingStep(step: Int) = container.setOnboardingStep(step)
    fun updateUserName(name: String) = container.updateUserName(name)
    fun setThemeMode(mode: String) = container.setThemeMode(mode)

    fun completeOnboarding() {
        viewModelScope.launch {
            container.completeOnboarding()
            val rawName = container.userName.value.trim()
            if (rawName.isNotBlank() && !rawName.equals("Friend", ignoreCase = true)) {
                val episodicId = container.memoryRepository.addEpisodicMemory(
                    "User exchanged names and prefers to be addressed as $rawName",
                    "OBSERVATION",
                    moduleId = "chat",
                    sessionId = container.sessionStore.currentSessionId
                )
                container.memoryRepository.addProfileMemory(
                    "User Name",
                    rawName,
                    listOf(episodicId),
                    moduleId = "chat"
                )
            }
        }
    }

    fun toggleDarkTheme(enabled: Boolean) = container.toggleDarkTheme(enabled)
}
