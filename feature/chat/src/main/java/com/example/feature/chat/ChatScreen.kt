package com.example.feature.chat

import com.example.core.design.theme.MaiaTheme

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val messages by viewModel.chatMessages.collectAsState(initial = emptyList())
    val isThinking by viewModel.isMaiaThinking.collectAsState(initial = false)
    val isModelLoading by viewModel.isModelLoading.collectAsState(initial = false)
    val isModelMissing by viewModel.isModelMissing.collectAsState(initial = false)
    val selectedModel by viewModel.selectedModelName.collectAsState(initial = "Gemma 1B")
    val showNewConversationConfirm by viewModel.showNewConversationConfirm.collectAsState(initial = false)
    val autoSpeak by viewModel.autoSpeak.collectAsState(initial = false)
    val isSpeaking by viewModel.isSpeaking.collectAsState(initial = false)
    val lastMetrics by viewModel.lastInferenceMetrics.collectAsState(initial = null)
    val showInferenceTelemetry by viewModel.showInferenceTelemetry.collectAsState(initial = true)
    
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Offline speech-to-text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = spokenList?.firstOrNull()?.trim()
            if (!spoken.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
            }
        }
    }

    fun launchVoiceRecognition() {
        if (isModelMissing || isModelLoading) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Maia...")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voice input not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // 1. Set up shared infinite transition for organic pulsing breathing orb
    val infiniteTransition = rememberInfiniteTransition(label = "OrbBreathing")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isThinking) 1.25f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 1200 else 2500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isThinking) 0.5f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 1200 else 2500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbAlpha"
    )

    val effectivePulseScale = if (isModelMissing) 1.0f else pulseScale
    val effectiveGlowAlpha = if (isModelMissing) 0.05f else glowAlpha

    // Auto-scroll to bottom of messages
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaiaTheme.colors.canvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Top Header & Breathing Presence Orb
            MaiaHeaderPresence(
                isThinking = isThinking, 
                isModelLoading = isModelLoading,
                isModelMissing = isModelMissing,
                selectedModel = selectedModel,
                pulseScale = effectivePulseScale,
                glowAlpha = effectiveGlowAlpha,
                lastMetrics = lastMetrics,
                showTelemetry = showInferenceTelemetry,
                autoSpeak = autoSpeak,
                isSpeaking = isSpeaking,
                onToggleAutoSpeak = { viewModel.toggleAutoSpeak(!autoSpeak) },
                onStopSpeaking = { viewModel.stopSpeaking() },
                onRefresh = { viewModel.retryModelInitialization() },
                onNewConversation = { viewModel.requestNewConversation() },
                onOpenSettings = onOpenSettings
            )

            if (showNewConversationConfirm) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissNewConversationConfirm() },
                    title = { Text("New conversation") },
                    text = {
                        Text(
                            "I'll still remember you. This just starts a fresh page between us."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.confirmNewConversation() }) {
                            Text("Start fresh")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissNewConversationConfirm() }) {
                            Text("Stay here")
                        }
                    }
                )
            }

            // Warning panel if model is missing (graceful fallback)
            if (isModelMissing) {
                ModelMissingWarningHeader(selectedModel, onRefresh = { viewModel.retryModelInitialization() })
            }

            // 2. Chat messages log or Central Breathing Orb Empty State
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Capture colors for use in drawBehind
                        val orbTerracotta = MaiaTheme.colors.maiaPresence
                        val orbAmber = MaiaTheme.colors.maiaGlow

                        // Central Core Breathing Orb representation
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .drawBehind {
                                    // Background soft glow pulsing (Pulse Ring 2)
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                orbTerracotta.copy(alpha = glowAlpha * 0.4f),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = (size.width / 2f) * pulseScale * 1.2f
                                    )
                                    // Secondary pulsing ring (Pulse Ring 1)
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                orbTerracotta.copy(alpha = glowAlpha * 0.25f),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = (size.width / 2f) * pulseScale * 0.8f
                                    )
                                    // Inner Core Orb bg
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                orbTerracotta,
                                                orbAmber
                                            )
                                        ),
                                        radius = size.width / 4.5f
                                    )
                                    // Ambient White Inner Ring
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.25f),
                                        radius = size.width / 5.2f,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                    )
                                    // Core Center Dot
                                    drawCircle(
                                        color = Color.White,
                                        radius = size.width / 40f
                                    )
                                }
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Maia's Voice tagline and privacy pledge
                        Text(
                            text = "She remembers. She protects. She's yours.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaiaTheme.colors.ink,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "I keep everything here on your phone.\nNothing leaves unless you say so.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp
                            ),
                            color = MaiaTheme.colors.inkMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    
                    // Grouping by day dividers phrased naturally
                    val groupedMessages = messages.groupBy { getFriendlyDayTime(it.timestamp) }
                    
                    groupedMessages.forEach { (dayTimeLabel, msgs) ->
                        item(key = "divider_$dayTimeLabel") {
                            DayDivider(dayTimeLabel)
                        }
                        items(msgs, key = { it.id }) { message ->
                            ChatMessageItem(
                                message = message,
                                showTelemetry = showInferenceTelemetry,
                                onSpeakClick = { viewModel.speakMessage(it) }
                            )
                        }
                    }
                    
                    if (isThinking) {
                        item {
                            MaiaThinkingIndicator()
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // 3. Minimalist Floating Pill Chat Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaiaTheme.colors.surface,
                border = BorderStroke(1.dp, MaiaTheme.colors.hairline),
                shadowElevation = 4.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Offline Speech-to-Text Mic Button
                    IconButton(
                        onClick = { launchVoiceRecognition() },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("voice_input_button"),
                        enabled = !isModelMissing && !isModelLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (!isModelMissing && !isModelLoading) MaiaTheme.colors.inkMuted
                                   else MaiaTheme.colors.inkMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = if (isModelMissing) "Model required to chat…" else "Talk to Maia…",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                color = MaiaTheme.colors.inkMuted.copy(alpha = 0.65f)
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            enabled = !isModelMissing && !isModelLoading,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                color = MaiaTheme.colors.ink
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaiaTheme.colors.maiaPresence),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() && !isModelMissing && !isModelLoading) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val canSend = inputText.isNotBlank() && !isModelLoading && !isModelMissing
                    IconButton(
                        onClick = {
                            if (canSend) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (canSend) MaiaTheme.colors.maiaPresence
                                else MaiaTheme.colors.maiaPresence.copy(alpha = 0.35f),
                                CircleShape
                            )
                            .testTag("chat_send_button"),
                        enabled = canSend
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MaiaHeaderPresence(
    isThinking: Boolean,
    isModelLoading: Boolean,
    isModelMissing: Boolean,
    selectedModel: String,
    pulseScale: Float,
    glowAlpha: Float,
    lastMetrics: com.example.core.model.InferenceMetrics? = null,
    showTelemetry: Boolean = true,
    autoSpeak: Boolean = false,
    isSpeaking: Boolean = false,
    onToggleAutoSpeak: () -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    onRefresh: () -> Unit,
    onNewConversation: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaiaTheme.colors.canvas,
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Radiant Orb + "Maia" + Quiet ON DEVICE pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenSettings() }
                ) {
                    com.example.core.design.components.MaiaOrb(
                        size = 28.dp,
                        isActive = isThinking || isModelLoading,
                        isDisabled = isModelMissing,
                        pulseScale = pulseScale,
                        glowAlpha = glowAlpha
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Maia",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaiaTheme.colors.ink
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    val pillLabel = when {
                        isModelLoading -> "WAKING UP"
                        isThinking -> "THINKING"
                        isModelMissing -> "OFFLINE"
                        showTelemetry && lastMetrics != null && lastMetrics.tokensPerSecond > 0f ->
                            String.format(Locale.US, "%.1f TOK/S", lastMetrics.tokensPerSecond)
                        else -> "ON DEVICE"
                    }
                    com.example.core.design.components.MaiaOnDevicePill(
                        label = pillLabel,
                        isHighlighted = isThinking || isModelLoading || (showTelemetry && lastMetrics != null)
                    )
                }

                // Right side: Minimalist voice, new chat, and privacy shield icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isSpeaking) onStopSpeaking() else onToggleAutoSpeak()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("header_voice_button")
                    ) {
                        Icon(
                            imageVector = when {
                                isSpeaking -> Icons.Default.Stop
                                autoSpeak -> Icons.AutoMirrored.Filled.VolumeUp
                                else -> Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = if (isSpeaking) "Stop voice" else "Toggle voice response",
                            tint = if (isSpeaking || autoSpeak) MaiaTheme.colors.maiaPresence else MaiaTheme.colors.inkMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onNewConversation,
                        modifier = Modifier.size(36.dp).testTag("header_new_conversation")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "New conversation",
                            tint = MaiaTheme.colors.inkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(36.dp).testTag("header_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy & Settings",
                            tint = MaiaTheme.colors.inkMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    if (isModelMissing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(36.dp).testTag("header_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry loading model",
                                tint = MaiaTheme.colors.maiaPresence,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider(
                color = MaiaTheme.colors.hairline,
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun ModelMissingWarningHeader(selectedModel: String, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaiaTheme.colors.maiaPresence.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Gemma Model Missing",
                tint = MaiaTheme.colors.maiaPresence,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$selectedModel Model File Not Loaded",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaiaTheme.colors.maiaPresence
                )
                Text(
                    text = "Place gemma3-1b-it-int4.task in /sdcard/Android/data/app.heymaia/files/models/ via 'adb push' and tap refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(36.dp).background(MaiaTheme.colors.maiaPresence.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaiaTheme.colors.maiaPresence,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun DayDivider(labelText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaiaTheme.colors.inkMuted.copy(alpha = 0.8f),
            modifier = Modifier
                .background(MaiaTheme.colors.canvas)
                .padding(horizontal = 12.dp)
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    showTelemetry: Boolean = true,
    onSpeakClick: (String) -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    var showTimestamp by remember { mutableStateOf(false) }
    
    // Memory Details Dialog State
    var showMemoryDetail by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val alignment = if (message.sender == "USER") Alignment.End else Alignment.Start
    val containerColor = if (message.sender == "USER") {
        MaiaTheme.colors.userBubble
    } else {
        Color.Transparent
    }

    val contentColor = if (message.sender == "USER") {
        MaiaTheme.colors.ink
    } else {
        MaiaTheme.colors.ink
    }

    val shape = if (message.sender == "USER") {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(16.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        // Tappable message bubble with long-press timestamp support
        Surface(
            modifier = Modifier
                .combinedClickable(
                    onClick = { /* Tap dismisses or does standard action */ },
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showTimestamp = !showTimestamp
                    }
                )
                .testTag(if (message.sender == "USER") "user_message" else "maia_message"),
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = if (message.sender == "USER") 16.dp else 4.dp, vertical = 12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )

                // Render visual RAG "remembering..." chip inside Maia's message bubble
                val memoryUsed = message.memoryUsed
                if (message.sender == "MAIA" && memoryUsed != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Surface(
                        onClick = { showMemoryDetail = true },
                        modifier = Modifier.testTag("remembering_chip"),
                        color = MaiaTheme.colors.surface,
                        border = BorderStroke(1.dp, MaiaTheme.colors.hairline),
                        shape = CircleShape,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaiaTheme.colors.maiaPresence, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val chipText = when {
                                memoryUsed.startsWith("Core Profile:") || memoryUsed.startsWith("Recall:") -> "CORE PROFILE RECALLED"
                                else -> "MEMORY RECALLED"
                            }
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaiaTheme.colors.inkMuted
                            )
                        }
                    }
                }

                // Live On-Device Inference Telemetry Pill
                val metrics = message.metrics
                if (showTelemetry && message.sender == "MAIA" && metrics != null && metrics.tokenCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaiaTheme.colors.surface.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, MaiaTheme.colors.hairline),
                        shadowElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaiaTheme.colors.maiaPresence,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f tok/s • %dms TTFT • 100%% on-device", metrics.tokensPerSecond, metrics.timeToFirstTokenMs),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaiaTheme.colors.inkMuted
                            )
                        }
                    }
                }

                // Actions: Voice Read Aloud and Play Policy AI Output Reporting
                if (message.sender == "MAIA" && message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { onSpeakClick(message.text) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaiaTheme.colors.surface.copy(alpha = 0.7f),
                            border = BorderStroke(0.5.dp, MaiaTheme.colors.hairline),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.testTag("speak_message_button_${message.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read aloud",
                                    tint = MaiaTheme.colors.maiaPresence,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Listen",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaiaTheme.colors.inkMuted
                                )
                            }
                        }

                        Surface(
                            onClick = { showReportDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaiaTheme.colors.surface.copy(alpha = 0.7f),
                            border = BorderStroke(0.5.dp, MaiaTheme.colors.hairline),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.testTag("report_message_button_${message.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Report AI response",
                                    tint = MaiaTheme.colors.inkMuted,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Report",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaiaTheme.colors.inkMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Long press toggleable timestamp
        AnimatedVisibility(
            visible = showTimestamp,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(message.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaiaTheme.colors.inkMuted,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
        }
    }

    // Interactive dialog to reveal the detailed memory source on tap
    val memoryDetail = message.memoryUsed
    if (showMemoryDetail && memoryDetail != null) {
        val isCoreProfile = memoryDetail.startsWith("Core Profile:") || memoryDetail.startsWith("Recall:")
        AlertDialog(
            onDismissRequest = { showMemoryDetail = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaiaTheme.colors.maiaPresence
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Memory Retrospective")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Maia formulated part of her response based on this secure on-device record:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaiaTheme.colors.inkMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = if (isCoreProfile) MaiaTheme.colors.surface else MaiaTheme.colors.userBubble,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaiaTheme.colors.hairline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaiaTheme.colors.maiaPresence, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCoreProfile) "CORE PROFILE FACT" else "EPISODIC MEMORY RECALL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaiaTheme.colors.maiaPresence
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = memoryDetail,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaiaTheme.colors.ink
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🔒 Stored locally in encrypted tri-store (Room + SQLCipher). Never shared over any network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaiaTheme.colors.inkMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemoryDetail = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaiaTheme.colors.maiaPresence
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Report Response")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Because Maia is completely offline with zero network access, reporting this message creates an email draft in your device's email client to submit feedback securely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaiaTheme.colors.inkMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaiaTheme.colors.userBubble.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "\"${message.text.take(160)}${if (message.text.length > 160) "..." else ""}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReportDialog = false
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@maia-companion.org"))
                            putExtra(Intent.EXTRA_SUBJECT, "[Maia AI Report] User Feedback on Model Output")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "I would like to report the following AI response from Maia:\n\n" +
                                "\"${message.text}\"\n\n" +
                                "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))}\n\n" +
                                "Issue description (e.g. offensive, inaccurate, inappropriate):\n"
                            )
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send Report via Email"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found to send report.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Draft Email Report", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }
}

@Composable
fun MaiaThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingDots")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaiaTheme.colors.userBubble,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Maia is breathing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaiaTheme.colors.inkMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaiaTheme.colors.maiaPresence.copy(alpha = alpha1)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaiaTheme.colors.maiaPresence.copy(alpha = alpha2)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaiaTheme.colors.maiaPresence.copy(alpha = alpha3)))
                }
            }
        }
    }
}

// Convert long UTC millisecond timestamps to natural friendly labels
private fun getFriendlyDayTime(timestamp: Long): String {
    val date = Date(timestamp)
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault()).format(date) // e.g. Tuesday
    val hour = SimpleDateFormat("H", Locale.getDefault()).format(date).toInt()
    val timeOfDay = when (hour) {
        in 0..4 -> "night"
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..21 -> "evening"
        else -> "night"
    }
    return "$dayFormat $timeOfDay"
}


