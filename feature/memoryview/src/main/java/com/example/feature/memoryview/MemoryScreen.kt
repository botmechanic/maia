package com.example.feature.memoryview

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.res.stringResource
import com.example.core.design.R as DesignR
import com.example.core.design.theme.MaiaTheme
import com.example.core.model.EpisodicEvent
import com.example.core.model.ProfileFact
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("maia_prefs", Context.MODE_PRIVATE) }
    val isBiometricEnabled = remember { sharedPrefs.getBoolean("biometric_lock_enabled", false) }
    var isVaultUnlocked by rememberSaveable { mutableStateOf(true) }

    // If Biometric Vault Lock is enabled and vault is locked, render Lock Screen
    if (isBiometricEnabled && !isVaultUnlocked) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaiaTheme.colors.canvas)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Minimalist Lock Screen
            com.example.core.design.components.MaiaOrb(size = 64.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Vault Locked",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                ),
                color = MaiaTheme.colors.ink
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock with biometrics to view your memories.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaiaTheme.colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        val executor = ContextCompat.getMainExecutor(context)
                        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                isVaultUnlocked = true
                            }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                Toast.makeText(context, "Unlock failed: $errString", Toast.LENGTH_SHORT).show()
                            }
                        })
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Unlock Maia's Memory Vault")
                            .setSubtitle("Confirm your biometric credential or screen lock")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            .build()
                        prompt.authenticate(promptInfo)
                    } else {
                        isVaultUnlocked = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaiaTheme.colors.maiaPresence,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
                    .testTag("unlock_vault_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock Vault", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    val activeFacts by viewModel.activeProfileMemories.collectAsState()
    val episodicMemories by viewModel.episodicMemories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    // 3-way segmented mode switch: "list", "timeline", "mindmap"
    var memoryViewMode by rememberSaveable { mutableStateOf("list") }

    // Memory Review Inbox Confirmed Tracking State (Keep / Curated)
    val confirmedFactIds = remember { mutableStateMapOf<Long, Boolean>() }

    val filteredFacts = remember(activeFacts, searchQuery, selectedCategory) {
        activeFacts.filter { fact ->
            val matchesCategory = when (selectedCategory) {
                "All", "Profile" -> true
                "Preferences" -> fact.factKey.contains("preference", ignoreCase = true) ||
                        fact.factKey.contains("favorite", ignoreCase = true) ||
                        fact.factKey.contains("like", ignoreCase = true) ||
                        fact.factKey.contains("coffee", ignoreCase = true)
                "Habits" -> fact.factKey.contains("habit", ignoreCase = true) ||
                        fact.factKey.contains("routine", ignoreCase = true)
                "Relationships" -> fact.factKey.contains("partner", ignoreCase = true) ||
                        fact.factKey.contains("friend", ignoreCase = true) ||
                        fact.factKey.contains("family", ignoreCase = true) ||
                        fact.factKey.contains("relationship", ignoreCase = true)
                "Episodic" -> false
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    fact.factKey.contains(searchQuery, ignoreCase = true) ||
                    fact.factValue.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val filteredEpisodics = remember(episodicMemories, searchQuery, selectedCategory) {
        if (selectedCategory in listOf("Preferences", "Habits", "Relationships")) {
            emptyList()
        } else {
            episodicMemories
                .filter { it.type != "OBSERVATION" }
                .reversed()
                .filter { event ->
                    searchQuery.isBlank() || event.content.contains(searchQuery, ignoreCase = true)
                }
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var selectedFactForSources by remember { mutableStateOf<ProfileFact?>(null) }
    var factToEdit by remember { mutableStateOf<ProfileFact?>(null) }
    var sourceEventsForSelected by remember { mutableStateOf<List<EpisodicEvent>>(emptyList()) }
    var showAddFactDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showPassphraseDialog by remember { mutableStateOf(false) }

    // Encrypted Vault File Exporter (SAF CreateDocument)
    var pendingEncryptedBytes by remember { mutableStateOf<ByteArray?>(null) }
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && pendingEncryptedBytes != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(pendingEncryptedBytes)
                }
                Toast.makeText(context, "Encrypted vault file exported (.maiavault)!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                pendingEncryptedBytes = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaiaTheme.colors.canvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Unified Minimalist Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenSettings() }
                ) {
                    com.example.core.design.components.MaiaOrb(size = 28.dp)
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
                    com.example.core.design.components.MaiaOnDevicePill(label = stringResource(DesignR.string.status_on_device))
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("memory_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Privacy & Settings",
                        tint = MaiaTheme.colors.inkMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Editorial Title + Quiet Count
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 14.dp)
            ) {
                Text(
                    text = stringResource(DesignR.string.remembers_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp
                    ),
                    color = MaiaTheme.colors.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(DesignR.string.remembers_subtitle, activeFacts.size),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaiaTheme.colors.inkMuted
                )
            }

            // Apple-Style Pill Segmented Control (List | Timeline | Explore)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(38.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaiaTheme.colors.userBubble,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Triple("list", stringResource(DesignR.string.remembers_tab_list), Icons.AutoMirrored.Filled.List),
                        Triple("timeline", stringResource(DesignR.string.remembers_tab_timeline), Icons.Default.History),
                        Triple("mindmap", stringResource(DesignR.string.remembers_tab_explore), Icons.Default.AccountTree)
                    ).forEach { (mode, label, icon) ->
                        val isSelected = memoryViewMode == mode
                        Surface(
                            onClick = { memoryViewMode = mode },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) MaiaTheme.colors.surface else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaiaTheme.colors.hairline) else null,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag(if (mode == "mindmap") "toggle_mind_map_button" else "mode_tab_$mode")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaiaTheme.colors.ink else MaiaTheme.colors.inkMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaiaTheme.colors.ink else MaiaTheme.colors.inkMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body depending on selected mode
            when (memoryViewMode) {
                "mindmap" -> {
                    // Mind Map Visualizer
                    MemoryMindMapVisualizer(
                        facts = activeFacts,
                        onWhyClick = { fact ->
                            scope.launch {
                                val events = viewModel.memoryRepository.getSourceEvents(fact)
                                sourceEventsForSelected = events
                                selectedFactForSources = fact
                            }
                        },
                        onDeleteClick = { fact ->
                            viewModel.deleteProfileFact(fact.id)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                "timeline" -> {
                    // Chronological Timeline Mode
                    val dayFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
                    val groupedTimeline = remember(filteredEpisodics) {
                        filteredEpisodics.groupBy { event ->
                            dayFormat.format(Date(event.timestamp))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            // Search bar in timeline
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("timeline_search_input"),
                                placeholder = {
                                    Text(
                                        text = "Search chronological turns...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaiaTheme.colors.inkMuted
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaiaTheme.colors.inkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = MaiaTheme.colors.inkMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaiaTheme.colors.maiaPresence,
                                    unfocusedBorderColor = MaiaTheme.colors.hairline,
                                    focusedContainerColor = MaiaTheme.colors.surface,
                                    unfocusedContainerColor = MaiaTheme.colors.surface
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (groupedTimeline.isEmpty()) {
                            item {
                                EmptyMemoryStateCard(
                                    text = if (searchQuery.isNotBlank()) "No conversation history matched \"$searchQuery\"."
                                    else "No conversations recorded yet. When you chat with Maia, your message turns will be grouped chronologically by day right here."
                                )
                            }
                        } else {
                            groupedTimeline.forEach { (dateHeader, events) ->
                                item {
                                    Text(
                                        text = dateHeader.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaiaTheme.colors.maiaPresence,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }

                                items(events, key = { "timeline_${it.id}" }) { memory ->
                                    EpisodicHistoryItem(
                                        memory = memory,
                                        onDeleteClick = {
                                            viewModel.deleteEpisodicEvent(memory.id)
                                        }
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                }

                else -> {
                    // Default List Mode with Memory Review Inbox ("Keep, Edit, or Forget")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "I hold this mental map of your life close. It is stored inside an encrypted SQLCipher database on your phone, protected by your Keystore. It never leaves this device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaiaTheme.colors.inkMuted,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Bar: Add Fact, Distill Now, Export
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = { showAddFactDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaiaTheme.colors.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaiaTheme.colors.hairline),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaiaTheme.colors.maiaPresence,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Add Fact",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaiaTheme.colors.ink
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        viewModel.triggerImmediateDistillation()
                                        Toast.makeText(context, "Distillation queued in background", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaiaTheme.colors.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaiaTheme.colors.hairline),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = null,
                                            tint = MaiaTheme.colors.maiaPresence,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Distill Now",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaiaTheme.colors.ink
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { showExportOptionsDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaiaTheme.colors.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaiaTheme.colors.hairline),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null,
                                            tint = MaiaTheme.colors.maiaPresence,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Export",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaiaTheme.colors.ink
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Instant Search Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("memory_search_input"),
                                placeholder = {
                                    Text(
                                        text = "Search memories & profile facts...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaiaTheme.colors.inkMuted
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaiaTheme.colors.inkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = MaiaTheme.colors.inkMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaiaTheme.colors.maiaPresence,
                                    unfocusedBorderColor = MaiaTheme.colors.hairline,
                                    focusedContainerColor = MaiaTheme.colors.surface,
                                    unfocusedContainerColor = MaiaTheme.colors.surface
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Category Filter Pills
                            val categories = listOf("All", "Preferences", "Habits", "Relationships")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categories) { category ->
                                    val isSelected = selectedCategory == category
                                    Surface(
                                        onClick = { viewModel.setSelectedCategory(category) },
                                        shape = CircleShape,
                                        color = if (isSelected) MaiaTheme.colors.maiaPresence else MaiaTheme.colors.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) MaiaTheme.colors.maiaPresence else MaiaTheme.colors.hairline
                                        ),
                                        modifier = Modifier.testTag("filter_$category")
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) Color.White else MaiaTheme.colors.inkMuted,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Memory Review Inbox Title with Curator Guidance
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "MEMORY REVIEW INBOX (${filteredFacts.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaiaTheme.colors.maiaPresence,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Keep • Edit • Forget",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaiaTheme.colors.inkMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Distilled Profile Facts List with Curator Actions
                        if (filteredFacts.isEmpty()) {
                            item {
                                EmptyMemoryStateCard(
                                    text = if (searchQuery.isNotBlank()) "No profile facts matched \"$searchQuery\"."
                                    else "I don't know much about you yet. That changes from here. Chat with me, and my background distillation worker will weave key facts about your life in here."
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        } else {
                            items(filteredFacts, key = { "fact_${it.id}" }) { fact ->
                                ProfileFactItem(
                                    fact = fact,
                                    isConfirmed = confirmedFactIds[fact.id] == true,
                                    onKeepClick = {
                                        confirmedFactIds[fact.id] = true
                                        Toast.makeText(context, "Fact confirmed in memory!", Toast.LENGTH_SHORT).show()
                                    },
                                    onEditClick = {
                                        factToEdit = fact
                                    },
                                    onWhyClick = {
                                        scope.launch {
                                            val events = viewModel.memoryRepository.getSourceEvents(fact)
                                            sourceEventsForSelected = events
                                            selectedFactForSources = fact
                                        }
                                    },
                                    onDeleteClick = {
                                        viewModel.deleteProfileFact(fact.id)
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Interactive Dialog: Source Events ("Why do you think that?")
    if (selectedFactForSources != null) {
        val fact = selectedFactForSources!!
        AlertDialog(
            onDismissRequest = { selectedFactForSources = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = MaiaTheme.colors.maiaPresence
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Why do I think this?")
                }
            },
            text = {
                Column {
                    Text(
                        text = "I distilled the profile fact \"${fact.factKey} = ${fact.factValue}\" from these secure local conversation logs:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaiaTheme.colors.inkMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (sourceEventsForSelected.isEmpty()) {
                        Text(
                            text = "Source events were cleared or modified. This fact remains as an on-device distillation.",
                            fontStyle = FontStyle.Italic,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sourceEventsForSelected) { event ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "${event.type}: ${event.content}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(event.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaiaTheme.colors.inkMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFactForSources = null }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }

    // Dialog: Edit Fact (Memory Review Inbox)
    if (factToEdit != null) {
        val target = factToEdit!!
        var editKey by remember { mutableStateOf(target.factKey) }
        var editValue by remember { mutableStateOf(target.factValue) }

        AlertDialog(
            onDismissRequest = { factToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaiaTheme.colors.maiaPresence)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Edit Memory Fact")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Refine this fact to keep Maia's mental model exact:", style = MaterialTheme.typography.bodyMedium, color = MaiaTheme.colors.inkMuted)
                    OutlinedTextField(
                        value = editKey,
                        onValueChange = { editKey = it },
                        label = { Text("Fact Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text("Fact Value") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editKey.isNotBlank() && editValue.isNotBlank()) {
                            viewModel.updateProfileFact(target.id, editKey, editValue)
                            factToEdit = null
                            Toast.makeText(context, "Fact updated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { factToEdit = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }

    // Dialog: Add Personal Fact
    if (showAddFactDialog) {
        var keyInput by remember { mutableStateOf("") }
        var valueInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFactDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaiaTheme.colors.maiaPresence
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Add Personal Fact")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Teach Maia a direct fact about you. It will be stored securely in her local encrypted database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaiaTheme.colors.inkMuted
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Fact Key (e.g. Favorite Coffee)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = valueInput,
                        onValueChange = { valueInput = it },
                        label = { Text("Fact Value (e.g. Oat Milk Cortado)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (keyInput.isNotBlank() && valueInput.isNotBlank()) {
                            viewModel.addProfileFact(keyInput, valueInput)
                            showAddFactDialog = false
                            Toast.makeText(context, "Fact saved to local vault", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Fact")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFactDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }

    // Dialog: Export Vault Options with Privacy Warning
    if (showExportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaiaTheme.colors.danger)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Export Memory Vault")
                }
            },
            text = {
                Column {
                    Text(
                        text = "⚠️ Privacy Notice: Exporting writes your private conversation logs and distilled profile facts outside of Maia's hardware-encrypted sandbox.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaiaTheme.colors.ink
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Choose your preferred export format:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaiaTheme.colors.inkMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showExportOptionsDialog = false
                            showPassphraseDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaiaTheme.colors.maiaPresence,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Encrypted Backup (.maiavault)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            showExportOptionsDialog = false
                            val markdown = viewModel.exportVaultMarkdown()
                            clipboardManager.setText(AnnotatedString(markdown))
                            Toast.makeText(context, "Unencrypted vault copied to clipboard. Keep it safe!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaiaTheme.colors.surface,
                            contentColor = MaiaTheme.colors.ink
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy Plain Markdown (Unencrypted)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportOptionsDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }

    // Dialog: Passphrase for Encrypted Vault Export
    if (showPassphraseDialog) {
        var passphraseInput by remember { mutableStateOf("") }
        var confirmPassphraseInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPassphraseDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaiaTheme.colors.maiaPresence)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Set Vault Password")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a password to encrypt your vault file with AES-256-GCM:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaiaTheme.colors.inkMuted
                    )
                    OutlinedTextField(
                        value = passphraseInput,
                        onValueChange = { passphraseInput = it; errorMessage = null },
                        label = { Text("Password (min 6 chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassphraseInput,
                        onValueChange = { confirmPassphraseInput = it; errorMessage = null },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(text = errorMessage!!, color = MaiaTheme.colors.danger, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (passphraseInput.length < 6) {
                            errorMessage = "Password must be at least 6 characters."
                        } else if (passphraseInput != confirmPassphraseInput) {
                            errorMessage = "Passwords do not match."
                        } else {
                            try {
                                val markdown = viewModel.exportVaultMarkdown()
                                val encryptedBytes = VaultCrypto.encryptVault(markdown, passphraseInput)
                                pendingEncryptedBytes = encryptedBytes
                                showPassphraseDialog = false
                                createDocLauncher.launch("maia_vault_backup.maiavault")
                            } catch (e: Exception) {
                                errorMessage = "Encryption failed: ${e.message}"
                            }
                        }
                    }
                ) {
                    Text("Export File", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPassphraseDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaiaTheme.colors.surface
        )
    }
}

@Composable
fun ProfileFactItem(
    fact: ProfileFact,
    isConfirmed: Boolean = false,
    onKeepClick: () -> Unit,
    onEditClick: () -> Unit,
    onWhyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("profile_fact_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fact.factKey.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaiaTheme.colors.maiaPresence,
                        fontWeight = FontWeight.Bold
                    )
                    if (isConfirmed) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaiaTheme.colors.maiaPresence.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaiaTheme.colors.maiaPresence,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Curated",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaiaTheme.colors.maiaPresence,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Curator Actions: Keep • Edit • Forget
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onKeepClick,
                        modifier = Modifier.size(32.dp).testTag("keep_fact_button_${fact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Keep in Memory",
                            tint = if (isConfirmed) MaiaTheme.colors.maiaPresence else MaiaTheme.colors.inkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp).testTag("edit_fact_button_${fact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Fact",
                            tint = MaiaTheme.colors.inkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp).testTag("delete_fact_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Forget Fact",
                            tint = MaiaTheme.colors.danger,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fact.factValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaiaTheme.colors.ink
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Why do you think that?",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaiaTheme.colors.maiaPresence,
                modifier = Modifier
                    .clickable { onWhyClick() }
                    .padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
fun EpisodicHistoryItem(
    memory: EpisodicEvent,
    onDeleteClick: () -> Unit
) {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("episodic_history_item"),
        colors = CardDefaults.cardColors(
            containerColor = MaiaTheme.colors.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaiaTheme.colors.inkMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memory.type,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaiaTheme.colors.maiaPresence
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sdf.format(Date(memory.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaiaTheme.colors.inkMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaiaTheme.colors.ink
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_episodic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Memory",
                    tint = MaiaTheme.colors.danger,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyMemoryStateCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaiaTheme.colors.maiaPresence.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaiaTheme.colors.inkMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
