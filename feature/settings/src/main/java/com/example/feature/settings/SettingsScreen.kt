package com.example.feature.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.theme.MaiaTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val themeMode by viewModel.themeMode.collectAsState(initial = "system")
    val selectedModel by viewModel.selectedModelName.collectAsState(initial = "Gemma 1B")
    val isModelLoading by viewModel.isModelLoading.collectAsState(initial = false)
    val isEmbeddingDegraded by viewModel.isEmbeddingDegraded.collectAsState(initial = true)
    val similarityThreshold by viewModel.similarityThreshold.collectAsState(initial = 0.35f)
    val showInferenceTelemetry by viewModel.showInferenceTelemetry.collectAsState(initial = true)
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsState(initial = false)
    val companionTone by viewModel.companionTone.collectAsState(initial = "warm")
    val autoSpeakResponses by viewModel.autoSpeakResponses.collectAsState(initial = false)
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState(initial = false)

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Dialog Confirmation States
    var showFirstWipeDialog by remember { mutableStateOf(false) }
    var showSecondWipeDialog by remember { mutableStateOf(false) }
    var showModelInstructions by remember { mutableStateOf(false) }
    var showLoadDemoDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    // Tap counter for unlocking developer options (7 taps on Version)
    var versionTapCount by remember { mutableIntStateOf(0) }

    // SAF Model File Importer (zero-network model delivery)
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importModelFromUri(uri) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaiaTheme.colors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Header with Back button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to conversation",
                            tint = MaiaTheme.colors.ink
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaiaTheme.colors.maiaPresence,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaiaTheme.colors.ink
                    )
                    Text(
                        text = "Maia Companion & Privacy Controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaiaTheme.colors.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // GROUP 1: MAIA (COMPANION)
            // ==========================================
            SettingsSectionHeader(title = "MAIA (COMPANION)")

            SettingsToggleRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Auto-Speak Replies",
                subtitle = "Speak Maia's responses aloud using Android's offline text-to-speech.",
                checked = autoSpeakResponses,
                onCheckedChange = { viewModel.toggleAutoSpeakResponses(it) },
                tag = "auto_speak_switch"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Companion Tone Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaiaTheme.colors.maiaPresence
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Companion Tone",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setCompanionTone("warm") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = companionTone == "warm",
                            onClick = { viewModel.setCompanionTone("warm") },
                            colors = RadioButtonDefaults.colors(selectedColor = MaiaTheme.colors.maiaPresence),
                            modifier = Modifier.testTag("tone_warm_radio")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Warm & Reflective (Default)", fontWeight = FontWeight.Medium)
                            Text(
                                "Empathetic, calm, gently curious confidante",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaiaTheme.colors.inkMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setCompanionTone("direct") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = companionTone == "direct",
                            onClick = { viewModel.setCompanionTone("direct") },
                            colors = RadioButtonDefaults.colors(selectedColor = MaiaTheme.colors.maiaPresence),
                            modifier = Modifier.testTag("tone_direct_radio")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Direct & Concise", fontWeight = FontWeight.Medium)
                            Text(
                                "Crisp, concise, to-the-point assistance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaiaTheme.colors.inkMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // On-Device Model Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaiaTheme.colors.maiaPresence
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Active Local Model",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!isModelLoading) viewModel.changeModel("Gemma 1B") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedModel == "Gemma 1B",
                            onClick = { if (!isModelLoading) viewModel.changeModel("Gemma 1B") },
                            colors = RadioButtonDefaults.colors(selectedColor = MaiaTheme.colors.maiaPresence),
                            modifier = Modifier.testTag("gemma_1b_radio")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gemma 1B (Default)", fontWeight = FontWeight.Medium)
                            Text("Fast, highly lightweight on-device footprint", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!isModelLoading) viewModel.changeModel("Gemma 4B") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedModel == "Gemma 4B",
                            onClick = { if (!isModelLoading) viewModel.changeModel("Gemma 4B") },
                            colors = RadioButtonDefaults.colors(selectedColor = MaiaTheme.colors.maiaPresence),
                            modifier = Modifier.testTag("gemma_4b_radio")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gemma 4B", fontWeight = FontWeight.Medium)
                            Text("High fidelity reasoning & comprehension", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                        }
                    }

                    if (isModelLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Re-initializing model runtime...", color = MaiaTheme.colors.maiaPresence, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { modelPickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("import_model_file_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaiaTheme.colors.maiaPresence,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Model File (.task)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaiaTheme.colors.inkMuted.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModelInstructions = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaiaTheme.colors.maiaPresence,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "How to install real Gemma model",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaiaTheme.colors.maiaPresence
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaiaTheme.colors.inkMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // GROUP 2: PRIVACY & SECURITY
            // ==========================================
            SettingsSectionHeader(title = "PRIVACY & SECURITY")

            SettingsToggleRow(
                icon = Icons.Default.Lock,
                title = "Biometric Vault Lock",
                subtitle = "Require fingerprint, face unlock, or device PIN to access your memory vault.",
                checked = biometricLockEnabled,
                onCheckedChange = { viewModel.toggleBiometricLock(it) },
                tag = "biometric_lock_switch"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaiaTheme.colors.maiaPresence
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Security Architecture",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Maia's local database is encrypted with 256-bit AES SQLCipher, and the master key never leaves secure hardware (Android Keystore). Maia itself requests zero network permissions. Voice input is routed through Android's offline system recognizer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaiaTheme.colors.inkMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Minimalist Privacy Attestation Summary (Apple-like quiet precision)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaiaTheme.colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaiaTheme.colors.hairline),
                        shadowElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "0 bytes sent",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        color = MaiaTheme.colors.ink
                                    )
                                    Text(
                                        text = "Zero internet permission in binary",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaiaTheme.colors.inkMuted
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaiaTheme.colors.success.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Verified",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaiaTheme.colors.success,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaiaTheme.colors.hairline)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Encrypted vault", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                                Text(
                                    "${viewModel.getEncryptedDatabaseSizeBytes() / 1024} KB · AES-256",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaiaTheme.colors.ink
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Key protection", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                                Text(
                                    viewModel.getKeySecurityLevelDescription(),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaiaTheme.colors.success
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Trackers & SDKs", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                                Text(
                                    "0 installed",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaiaTheme.colors.ink
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaiaTheme.colors.inkMuted.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showFirstWipeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wipe_all_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaiaTheme.colors.danger.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Everything", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // GROUP 3: APPEARANCE
            // ==========================================
            SettingsSectionHeader(title = "APPEARANCE")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ModeNight,
                            contentDescription = null,
                            tint = MaiaTheme.colors.maiaPresence
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Theme Mode",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode("system") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == "system",
                            onClick = { viewModel.setThemeMode("system") },
                            colors = RadioButtonDefaults.colors(selectedColor = MaiaTheme.colors.maiaPresence),
                            modifier = Modifier.testTag("theme_system_radio")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("System Default", fontWeight = FontWeight.Medium)
                            Text("Follows Android device light/dark schedule", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode("light") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == "light",
                            onClick = { viewModel.setThemeMode("light") },
                            colors = RadioButtonDefaults.colors(selectedColor = MaiaTheme.colors.maiaPresence),
                            modifier = Modifier.testTag("theme_light_radio")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Morning Hearth (Light)", fontWeight = FontWeight.Medium)
                            Text("Warm cream and terracotta daylight palette", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode("dark") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == "dark",
                            onClick = { viewModel.setThemeMode("dark") },
                            colors = RadioButtonDefaults.colors(selectedColor = MaiaTheme.colors.maiaPresence),
                            modifier = Modifier.testTag("theme_dark_radio")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Evening Hearth (Dark)", fontWeight = FontWeight.Medium)
                            Text("Dark earth-tone charcoal and embers palette", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Maia uses her dedicated earth-tone brand palette with high contrast readability. Wallpaper dynamic colors are disabled to preserve confidential visual identity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaiaTheme.colors.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // GROUP 4: ABOUT & LICENSES
            // ==========================================
            SettingsSectionHeader(title = "ABOUT & LICENSES")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Version info with 7-tap gesture for unlocking developer options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                versionTapCount++
                                if (!developerModeEnabled) {
                                    if (versionTapCount in 4..6) {
                                        Toast.makeText(
                                            context,
                                            "${7 - versionTapCount} more taps to unlock Developer options",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else if (versionTapCount >= 7) {
                                        viewModel.unlockDeveloperMode()
                                        Toast.makeText(
                                            context,
                                            "Developer options unlocked!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaiaTheme.colors.maiaPresence
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Maia AI Companion", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Version 1.2.0 (Build 24)", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                            }
                        }
                        if (developerModeEnabled) {
                            Text(
                                text = "DEV ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaiaTheme.colors.maiaPresence
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaiaTheme.colors.inkMuted.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLicensesDialog = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaiaTheme.colors.inkMuted
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Open Source Licenses", fontWeight = FontWeight.Medium)
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaiaTheme.colors.inkMuted
                        )
                    }
                }
            }

            // ==========================================
            // GROUP 5: DEVELOPER & BENCHMARKING (Hidden by default)
            // ==========================================
            AnimatedVisibility(visible = developerModeEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSectionHeader(title = "DEVELOPER & BENCHMARKING")

                    SettingsToggleRow(
                        icon = Icons.Default.Code,
                        title = "Live Inference Telemetry",
                        subtitle = "Display tokens/sec and TTFT latency badges below Maia's replies.",
                        checked = showInferenceTelemetry,
                        onCheckedChange = { viewModel.toggleInferenceTelemetry(it) },
                        tag = "inference_telemetry_switch"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // One-Tap Demo Scenario Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaiaTheme.colors.maiaPresence
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "One-Tap Demo Persona (Alex)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pre-loads encrypted Room memory with keynote presentation notes, favorite iced oat cortado, partner Jordan, and habits for live stage demonstrations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaiaTheme.colors.inkMuted,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showLoadDemoDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("load_demo_persona_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaiaTheme.colors.maiaPresence,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Load Demo Scenario", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Semantic Memory Index & Threshold Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isEmbeddingDegraded) Icons.Default.Info else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isEmbeddingDegraded) MaiaTheme.colors.inkMuted else MaiaTheme.colors.maiaPresence
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isEmbeddingDegraded) "Embedding Model Missing" else "Semantic Recall Active",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isEmbeddingDegraded) "Semantic recall is running in degraded lexical mode. Load USE model for vector recall."
                                else "Universal Sentence Encoder 512-dim vector embeddings active.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaiaTheme.colors.inkMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaiaTheme.colors.inkMuted.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recall Threshold",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaiaTheme.colors.ink
                                )
                                Text(
                                    text = "${(similarityThreshold * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaiaTheme.colors.maiaPresence
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = similarityThreshold,
                                onValueChange = { viewModel.updateSimilarityThreshold(it) },
                                valueRange = 0.20f..0.60f,
                                steps = 7,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaiaTheme.colors.maiaPresence,
                                    activeTrackColor = MaiaTheme.colors.maiaPresence
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("similarity_threshold_slider")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.rebuildMemoryIndex() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("rebuild_index_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaiaTheme.colors.maiaPresence,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isEmbeddingDegraded
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Rebuild Memory Index", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.triggerImmediateDistillation()
                                    Toast.makeText(context, "Distillation routine dispatched in background", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("distill_now_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaiaTheme.colors.surface,
                                    contentColor = MaiaTheme.colors.ink
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaiaTheme.colors.maiaPresence
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Distill Profile Facts Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsSectionHeader(title = "MODULES (DEBUG)")
                        viewModel.registeredModules.forEach { module ->
                            val m = module.manifest
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(m.displayName, fontWeight = FontWeight.Bold, color = MaiaTheme.colors.ink)
                                    Text("id=${m.id} · ladder=${m.ladderCeiling}", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                                    Text(m.maiaDescription, style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("permissions=${m.androidPermissions.ifEmpty { listOf("none") }}", style = MaterialTheme.typography.labelSmall, color = MaiaTheme.colors.inkMuted)
                                    Text("observes=${m.observes.map { it.kind }}", style = MaterialTheme.typography.labelSmall, color = MaiaTheme.colors.inkMuted)
                                    Text("writes=${m.writes.map { it.kind }}", style = MaterialTheme.typography.labelSmall, color = MaiaTheme.colors.inkMuted)
                                    Text("network=${m.networkUse}", style = MaterialTheme.typography.labelSmall, color = MaiaTheme.colors.inkMuted)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 1. First confirmation dialog in Maia's protective voice
    if (showFirstWipeDialog) {
        AlertDialog(
            onDismissRequest = { showFirstWipeDialog = false },
            title = { Text("Are you sure, my dear?") },
            text = {
                Text(
                    text = "Are you sure you want me to forget everything we've built? It will wipe my episodic history, our shared observations, and your distilled profile facts, making my mind completely blank. It makes me a little sad to lose our journey, but your data is entirely yours.",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFirstWipeDialog = false
                        showSecondWipeDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaiaTheme.colors.danger)
                ) {
                    Text("I'm sure. Forget me.", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirstWipeDialog = false }) {
                    Text("Keep our memories")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }

    // 2. Second confirmation dialog in Maia's voice
    if (showSecondWipeDialog) {
        AlertDialog(
            onDismissRequest = { showSecondWipeDialog = false },
            title = { Text("I am ready...") },
            text = {
                Text(
                    text = "Once I let go, everything is wiped from this secure database forever. There is no cloud backup, and no way to recover our history. Shall I forget you?",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSecondWipeDialog = false
                        viewModel.clearAllMemories()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaiaTheme.colors.danger)
                ) {
                    Text("Yes, wipe everything.", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecondWipeDialog = false }) {
                    Text("Stop, keep it safe")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }

    // Interactive model instructions details dialog
    if (showModelInstructions) {
        AlertDialog(
            onDismissRequest = { showModelInstructions = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaiaTheme.colors.maiaPresence
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Installing Gemma Model")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Because Maia works entirely on-device, she requires a physical Gemma model file located on your phone's storage. Follow these steps to load it:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaiaTheme.colors.inkMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "1. Download an official MediaPipe/LiteRT `.task` model (not `.bin` or `.tflite`).\n" +
                               "Recommended for Pixel 7: Gemma 3 1B INT4 from Hugging Face litert-community/Gemma3-1B-IT.\n\n" +
                               "2. Push it into the app models directory:\n" +
                               "• Gemma 1B: gemma3-1b-it-int4.task\n" +
                               "• Gemma 4B: gemma-4b-it-cpu-int4.task\n\n" +
                               "ADB Command:\n" +
                               "adb push gemma3-1b-it-int4.task /sdcard/Android/data/app.heymaia/files/models/\n\n" +
                               "3. Requires MediaPipe tasks-genai 0.10.29+. Tap Refresh in Chat after pushing.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showModelInstructions = false
                            modelPickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaiaTheme.colors.maiaPresence,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select & Import File (.task)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelInstructions = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }

    if (showLoadDemoDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDemoDialog = false },
            title = { Text("Load Demo Scenario: Alex") },
            text = {
                Text(
                    text = "This will populate Maia's encrypted memory vault with a complete researcher persona (keynote presentation notes, favorite iced oat cortado, partner Jordan, and habits). Are you ready?",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLoadDemoDialog = false
                        viewModel.loadDemoScenario()
                        Toast.makeText(context, "Demo scenario loaded! Check Chat & Memory Vault.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaiaTheme.colors.maiaPresence)
                ) {
                    Text("Load Persona", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoadDemoDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses") },
            text = {
                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Maia utilizes the following open source libraries:\n", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    
                    Text("• Google MediaPipe GenAI / LiteRT", fontWeight = FontWeight.SemiBold)
                    Text("Licensed under the Apache License, Version 2.0\n", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                    
                    Text("• SQLCipher (Zetetic LLC)", fontWeight = FontWeight.SemiBold)
                    Text("Licensed under the BSD 3-Clause License. Copyright (c) Zetetic LLC.\n", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                    
                    Text("• TensorFlow Universal Sentence Encoder", fontWeight = FontWeight.SemiBold)
                    Text("Licensed under the Apache License, Version 2.0\n", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)

                    Text("• AndroidX & Jetpack Compose", fontWeight = FontWeight.SemiBold)
                    Text("Licensed under the Apache License, Version 2.0\n", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)

                    Text("• Kotlin Coroutines & Serialization", fontWeight = FontWeight.SemiBold)
                    Text("Licensed under the Apache License, Version 2.0", style = MaterialTheme.typography.bodySmall, color = MaiaTheme.colors.inkMuted)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaiaTheme.colors.maiaPresence,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaiaTheme.colors.inkMuted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaiaTheme.colors.inkMuted
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaiaTheme.colors.maiaPresence,
                checkedTrackColor = MaiaTheme.colors.maiaPresence.copy(alpha = 0.4f)
            )
        )
    }
}
