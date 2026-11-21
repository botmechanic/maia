package com.example.core.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.theme.MaiaTheme

/**
 * Radiant amber-to-terracotta companion orb.
 */
@Composable
fun MaiaOrb(
    size: Dp = 28.dp,
    isActive: Boolean = false,
    isDisabled: Boolean = false,
    pulseScale: Float = 1f,
    glowAlpha: Float = 0.25f,
    modifier: Modifier = Modifier
) {
    val terracotta = MaiaTheme.colors.maiaPresence
    val amber = MaiaTheme.colors.maiaGlow
    val muted = MaiaTheme.colors.inkMuted

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                if (isActive && !isDisabled) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                terracotta.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = (this.size.width / 2f) * pulseScale * 1.45f
                    )
                }
                if (isDisabled) {
                    drawCircle(color = muted.copy(alpha = 0.35f))
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(amber, terracotta),
                            center = androidx.compose.ui.geometry.Offset(
                                x = this.size.width * 0.32f,
                                y = this.size.height * 0.28f
                            ),
                            radius = this.size.width * 0.85f
                        )
                    )
                }
            }
    )
}

/**
 * Quiet status pill ("ON DEVICE" or live telemetry when enabled in Developer Settings).
 */
@Composable
fun MaiaOnDevicePill(
    label: String = "ON DEVICE",
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(24.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaiaTheme.colors.userBubble
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (isHighlighted) MaiaTheme.colors.maiaPresence else MaiaTheme.colors.inkMuted,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                ),
                color = if (isHighlighted) MaiaTheme.colors.maiaPresence else MaiaTheme.colors.inkMuted
            )
        }
    }
}

data class MaiaNavDestination(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val testTag: String
)

/**
 * Minimalist Apple-like floating pill bottom navigation bar.
 * Suspended above the system gesture inset with 1dp hairline and soft micro-elevation.
 */
@Composable
fun MaiaFloatingNavBar(
    destinations: List<MaiaNavDestination>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(56.dp)
                .testTag("main_bottom_nav"),
            shape = RoundedCornerShape(999.dp),
            color = MaiaTheme.colors.surface,
            border = BorderStroke(1.dp, MaiaTheme.colors.hairline),
            shadowElevation = 6.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                destinations.forEach { dest ->
                    val selected = dest.id == selectedId
                    val pillBg = if (selected) {
                        MaiaTheme.colors.maiaPresence.copy(alpha = 0.10f)
                    } else {
                        Color.Transparent
                    }
                    val contentColor = if (selected) {
                        MaiaTheme.colors.maiaPresence
                    } else {
                        MaiaTheme.colors.inkMuted
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(pillBg)
                            .clickable { onSelect(dest.id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag(dest.testTag),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.contentDescription,
                            tint = contentColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Level 1 architectural card: 22dp radius, 1dp hairline border, zero drop shadow.
 */
@Composable
fun MaiaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(22.dp),
            color = MaiaTheme.colors.surface,
            border = BorderStroke(1.dp, MaiaTheme.colors.hairline),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(22.dp),
            color = MaiaTheme.colors.surface,
            border = BorderStroke(1.dp, MaiaTheme.colors.hairline),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

