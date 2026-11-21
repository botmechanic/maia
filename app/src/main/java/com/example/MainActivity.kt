package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.design.R as DesignR
import com.example.core.design.components.MaiaFloatingNavBar
import com.example.core.design.components.MaiaNavDestination
import com.example.core.design.theme.MaiaTheme
import com.example.core.design.theme.MyApplicationTheme
import com.example.di.MaiaAppContainer
import com.example.feature.chat.ChatScreen
import com.example.feature.chat.ChatViewModel
import com.example.feature.memoryview.MemoryScreen
import com.example.feature.memoryview.MemoryViewModel
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.VaultRecoveryScreen

class MainActivity : FragmentActivity() {

    private val shellViewModel: AppShellViewModel by viewModels()
    private val container by lazy { MaiaAppContainer.get(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by shellViewModel.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemDark
            }
            val onboardingCompleted by shellViewModel.onboardingCompleted.collectAsState()
            val vaultInitError by container.vaultInitError.collectAsState()
            val biometricLockEnabled by container.biometricLockEnabled.collectAsState()
            var isAppUnlocked by rememberSaveable { mutableStateOf(!biometricLockEnabled) }

            // Re-lock automatically whenever Maia goes to the background
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, biometricLockEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP && biometricLockEnabled) {
                        isAppUnlocked = false
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            MyApplicationTheme(darkTheme = isDark) {
                if (vaultInitError != null) {
                    VaultRecoveryScreen(
                        errorMessage = vaultInitError ?: "Vault key unavailable.",
                        onResetVault = {
                            container.resetCorruptedVault()
                            recreate()
                        }
                    )
                } else if (onboardingCompleted && biometricLockEnabled && !isAppUnlocked) {
                    AppLockScreen(
                        onUnlocked = { isAppUnlocked = true }
                    )
                } else if (!onboardingCompleted) {
                    OnboardingScreen(viewModel = shellViewModel)
                } else {
                    var currentTab by rememberSaveable { mutableStateOf("chat") }
                    val saveableStateHolder = rememberSaveableStateHolder()
                    val maiaColors = MaiaTheme.colors
                    val chatVm: ChatViewModel = viewModel(factory = container.chatFactory())
                    val memoryVm: MemoryViewModel = viewModel(factory = container.memoryFactory())
                    val settingsVm: SettingsViewModel = viewModel(factory = container.settingsFactory())

                    // Intercept hardware/gesture back press when in Settings to return to Chat
                    BackHandler(enabled = currentTab == "settings") {
                        currentTab = "chat"
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = maiaColors.canvas,
                        contentColor = maiaColors.ink,
                        bottomBar = {
                            if (currentTab != "settings") {
                                MaiaFloatingNavBar(
                                    destinations = listOf(
                                        MaiaNavDestination(
                                            id = "chat",
                                            label = stringResource(DesignR.string.nav_talk),
                                            icon = androidx.compose.material.icons.Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = stringResource(DesignR.string.nav_talk),
                                            testTag = "nav_tab_chat"
                                        ),
                                        MaiaNavDestination(
                                            id = "memory",
                                            label = stringResource(DesignR.string.nav_remembers),
                                            icon = androidx.compose.material.icons.Icons.Outlined.AutoAwesome,
                                            contentDescription = stringResource(DesignR.string.nav_remembers),
                                            testTag = "nav_tab_memory"
                                        )
                                    ),
                                    selectedId = currentTab,
                                    onSelect = { currentTab = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                                },
                                label = "TabTransitions"
                            ) { tab ->
                                saveableStateHolder.SaveableStateProvider(tab) {
                                    when (tab) {
                                        "chat" -> ChatScreen(
                                            viewModel = chatVm,
                                            onOpenSettings = { currentTab = "settings" }
                                        )
                                        "memory" -> MemoryScreen(
                                            viewModel = memoryVm,
                                            onOpenSettings = { currentTab = "settings" }
                                        )
                                        "settings" -> SettingsScreen(
                                            viewModel = settingsVm,
                                            onBack = { currentTab = "chat" }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
