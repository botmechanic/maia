package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppShellViewModel
import com.example.core.design.components.MaiaOnDevicePill
import com.example.core.design.components.MaiaOrb
import com.example.core.design.theme.MaiaTheme

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
            .background(MaiaTheme.colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Minimalist Top Row: OFFLINE CORE pill + Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MaiaOnDevicePill(label = "OFFLINE CORE")
            TextButton(
                onClick = { viewModel.completeOnboarding() },
                modifier = Modifier.testTag("skip_onboarding_button"),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaiaTheme.colors.inkMuted
                )
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Main Centered Step Content
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
                    fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(240))
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

        // Bottom Indicators + Single Primary Pill CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                repeat(3) { index ->
                    val isSelected = (index + 1) == step
                    val width by animateFloatAsState(if (isSelected) 22f else 7f, label = "indicator")
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(7.dp)
                            .width(width.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaiaTheme.colors.maiaPresence
                                else MaiaTheme.colors.hairline
                            )
                    )
                }
            }

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
                    .height(52.dp)
                    .testTag("onboarding_next_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaiaTheme.colors.maiaPresence,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (step) {
                            1 -> "Get started"
                            2 -> "Continue"
                            else -> "Let's begin"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (step == 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No cloud  ·  No account required",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaiaTheme.colors.inkMuted
            )
        }
    }
}

@Composable
fun StepOneIntro() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        MaiaOrb(
            size = 92.dp,
            isActive = true,
            pulseScale = 1.05f,
            glowAlpha = 0.22f
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Hi, I'm Maia",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp
            ),
            color = MaiaTheme.colors.ink,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "A companion that lives only on your phone.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                lineHeight = 25.sp
            ),
            color = MaiaTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun StepTwoPrivacy() {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Private by design",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp
            ),
            color = MaiaTheme.colors.ink
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Everything stays strictly inside your phone.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            color = MaiaTheme.colors.inkMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrivacyHighlightCard(
                icon = Icons.Outlined.Lock,
                tint = MaiaTheme.colors.maiaPresence,
                title = "No internet access",
                subtitle = "Zero network permissions in the app binary."
            )
            PrivacyHighlightCard(
                icon = Icons.Outlined.Memory,
                tint = MaiaTheme.colors.success,
                title = "Encrypted on this phone",
                subtitle = "AES-256 vault backed by secure hardware."
            )
            PrivacyHighlightCard(
                icon = Icons.Outlined.VisibilityOff,
                tint = MaiaTheme.colors.inkMuted,
                title = "Nothing leaves your device",
                subtitle = "Zero telemetry, zero cloud sync, no tracking."
            )
        }
    }
}

@Composable
private fun PrivacyHighlightCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaiaTheme.colors.surface,
        border = BorderStroke(1.dp, MaiaTheme.colors.hairline),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaiaTheme.colors.ink
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaiaTheme.colors.inkMuted
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
        MaiaOrb(
            size = 64.dp,
            isActive = false
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "What should I call you?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = MaiaTheme.colors.ink,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Stored only inside your encrypted vault.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaiaTheme.colors.inkMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = onNameChange,
            placeholder = {
                Text(
                    text = "Your first name (optional)",
                    color = MaiaTheme.colors.inkMuted.copy(alpha = 0.65f)
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaiaTheme.colors.surface,
                unfocusedContainerColor = MaiaTheme.colors.surface,
                focusedBorderColor = MaiaTheme.colors.maiaPresence,
                unfocusedBorderColor = MaiaTheme.colors.hairline,
                cursorColor = MaiaTheme.colors.maiaPresence,
                focusedTextColor = MaiaTheme.colors.ink,
                unfocusedTextColor = MaiaTheme.colors.ink
            ),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
