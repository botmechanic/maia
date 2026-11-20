package com.example.ui.screens

import com.example.core.design.theme.MaiaTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppShellViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: AppShellViewModel,
    modifier: Modifier = Modifier
) {
    val step by viewModel.onboardingStep.collectAsState()
    val userName by viewModel.userName.collectAsState()
    var nameInput by remember { mutableStateOf(if (userName == "Friend") "" else userName) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaiaTheme.colors.canvas,
                        MaiaTheme.colors.userBubble.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        // Top Skip Button
        TextButton(
            onClick = { viewModel.completeOnboarding() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("skip_onboarding_button"),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaiaTheme.colors.inkMuted
            )
        ) {
            Text("Skip onboarding")
        }

        // Main content container with fade-in animations for each step
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "OnboardingSteps"
            ) { targetStep ->
                when (targetStep) {
                    1 -> StepOneIntro()
                    2 -> StepTwoPrivacy()
                    3 -> StepThreeName(
                        nameInput = nameInput,
                        onNameChange = {
                            nameInput = it
                            viewModel.updateUserName(it)
                        }
                    )
                }
            }
        }

        // Bottom Navigation/Action controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(3) { index ->
                    val isSelected = (index + 1) == step
                    val width by animateFloatAsState(if (isSelected) 24f else 8f, label = "indicator")
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaiaTheme.colors.maiaPresence 
                                else MaiaTheme.colors.inkMuted.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            // Navigation Button
            Button(
                onClick = {
                    if (step < 3) {
                        viewModel.setOnboardingStep(step + 1)
                    } else {
                        keyboardController?.hide()
                        viewModel.completeOnboarding()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_next_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaiaTheme.colors.maiaPresence,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (step == 3) "Let's begin" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next"
                    )
                }
            }
        }
    }
}

@Composable
fun StepOneIntro() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Decorative glowing halo/shield
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaiaTheme.colors.maiaPresence.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaiaTheme.colors.maiaPresence,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "I am Maia",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp),
            fontWeight = FontWeight.Bold,
            color = MaiaTheme.colors.ink,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "\"I remember. I protect. I'm yours.\"",
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = MaiaTheme.colors.maiaPresence,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "I am a warm, offline AI companion built to stay with you, entirely on your phone. I Chat with you, hold your life's memories close, and make sure your data never leaves your device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaiaTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun StepTwoPrivacy() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Lock and security representation
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaiaTheme.colors.maiaGlow.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaiaTheme.colors.maiaPresence,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "The Privacy Promise",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaiaTheme.colors.ink,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaiaTheme.colors.userBubble.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaiaTheme.colors.maiaPresence,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Zero Internet Permissions",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "I have no network permission in my manifest file. It is literally impossible for me to send your messages, facts, or observations to any server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaiaTheme.colors.inkMuted,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaiaTheme.colors.maiaPresence,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SQLCipher Encryption",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "My local memories are heavily encrypted with SQLCipher. The encryption key is generated and stored securely inside your Android Keystore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaiaTheme.colors.inkMuted,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun StepThreeName(
    nameInput: String,
    onNameChange: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Greeting visual
        Text(
            text = "👋",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Let's exchange names",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaiaTheme.colors.ink,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "I am Maia, and I will keep your secrets. How should I address you, my dear?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaiaTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = onNameChange,
            placeholder = { Text("Enter your name...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("user_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaiaTheme.colors.maiaPresence,
                unfocusedBorderColor = MaiaTheme.colors.inkMuted.copy(alpha = 0.5f),
                cursorColor = MaiaTheme.colors.maiaPresence
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
