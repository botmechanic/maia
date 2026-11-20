package com.example.feature.memoryview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.theme.MaiaTheme
import com.example.core.model.ProfileFact
import kotlin.math.cos
import kotlin.math.sin

data class VisualNode(
    val fact: ProfileFact,
    val center: Offset,
    val radius: Float,
    val color: Color,
    val label: String,
    val category: String
)

@Composable
fun MemoryMindMapVisualizer(
    facts: List<ProfileFact>,
    onWhyClick: (ProfileFact) -> Unit,
    onDeleteClick: (ProfileFact) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedFact by remember { mutableStateOf<ProfileFact?>(null) }

    // Breathing glow animation for central root node
    val infiniteTransition = rememberInfiniteTransition(label = "mindmap_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val userName = facts.firstOrNull {
        it.factKey.equals("name", ignoreCase = true) || it.factKey.equals("user name", ignoreCase = true)
    }?.factValue?.trim() ?: "You"

    val canvasBackground = MaiaTheme.colors.canvas
    val rootColor = MaiaTheme.colors.maiaPresence
    val glowColor = MaiaTheme.colors.maiaGlow
    val hairlineColor = MaiaTheme.colors.hairline
    val inkColor = MaiaTheme.colors.ink
    val inkMutedColor = MaiaTheme.colors.inkMuted
    val surfaceColor = MaiaTheme.colors.surface
    val successColor = MaiaTheme.colors.success
    val dangerColor = MaiaTheme.colors.danger

    var currentNodes by remember { mutableStateOf<List<VisualNode>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBackground)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mind_map_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.6f, 2.2f)
                        panOffset += pan
                    }
                }
                .pointerInput(facts, currentNodes, panOffset, scale) {
                    detectTapGestures { tapOffset ->
                        // Reverse transform tapOffset to find tapped node in graph space
                        val hit = currentNodes.firstOrNull { node ->
                            val dx = tapOffset.x - node.center.x
                            val dy = tapOffset.y - node.center.y
                            (dx * dx + dy * dy) <= (node.radius * 1.5f * node.radius * 1.5f)
                        }
                        selectedFact = hit?.fact
                    }
                }
        ) {
            val canvasCenter = Offset(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)

            // Draw faint orbital background rings
            val baseRadius = with(density) { 140.dp.toPx() } * scale
            val outerRadius = with(density) { 210.dp.toPx() } * scale
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f), 0f)

            drawCircle(
                color = hairlineColor.copy(alpha = 0.45f),
                radius = baseRadius,
                center = canvasCenter,
                style = Stroke(width = 1f, pathEffect = dashEffect)
            )
            drawCircle(
                color = hairlineColor.copy(alpha = 0.25f),
                radius = outerRadius,
                center = canvasCenter,
                style = Stroke(width = 1f, pathEffect = dashEffect)
            )

            // Compute positions for satellite nodes
            val nodeRadius = with(density) { 26.dp.toPx() } * scale
            val nodes = mutableListOf<VisualNode>()

            val factCount = facts.size
            facts.forEachIndexed { index, fact ->
                val useOuterOrbit = factCount > 6 && (index % 2 == 1)
                val orbitDist = if (useOuterOrbit) outerRadius else baseRadius
                val angle = (2.0 * Math.PI * index / factCount) - (Math.PI / 2.0)
                val nodeCenter = Offset(
                    x = canvasCenter.x + (orbitDist * cos(angle)).toFloat(),
                    y = canvasCenter.y + (orbitDist * sin(angle)).toFloat()
                )

                val (catColor, category) = when {
                    fact.factKey.contains("partner", ignoreCase = true) ||
                        fact.factKey.contains("friend", ignoreCase = true) ||
                        fact.factKey.contains("relationship", ignoreCase = true) ->
                        Pair(Color(0xFFD47A65), "Relationship")

                    fact.factKey.contains("favorite", ignoreCase = true) ||
                        fact.factKey.contains("coffee", ignoreCase = true) ||
                        fact.factKey.contains("like", ignoreCase = true) ->
                        Pair(glowColor, "Preference")

                    fact.factKey.contains("habit", ignoreCase = true) ||
                        fact.factKey.contains("routine", ignoreCase = true) ||
                        fact.factKey.contains("run", ignoreCase = true) ->
                        Pair(successColor, "Habit")

                    fact.factKey.contains("mood", ignoreCase = true) ||
                        fact.factKey.contains("feeling", ignoreCase = true) ||
                        fact.factKey.contains("tone", ignoreCase = true) ->
                        Pair(Color(0xFF8E7D9E), "Tone")

                    else -> Pair(rootColor, "Profile")
                }

                val label = fact.factKey.take(12)
                nodes.add(VisualNode(fact, nodeCenter, nodeRadius, catColor, label, category))
            }
            currentNodes = nodes

            // 1. Draw connecting lines from root center to each node
            nodes.forEach { node ->
                val isSelected = selectedFact?.id == node.fact.id
                val lineColor = if (isSelected) node.color else hairlineColor.copy(alpha = 0.7f)
                val strokeWidth = if (isSelected) 2.5f * scale else 1.2f * scale

                drawLine(
                    color = lineColor,
                    start = canvasCenter,
                    end = node.center,
                    strokeWidth = strokeWidth
                )

                // Midpoint glow particle
                val midX = (canvasCenter.x + node.center.x) / 2f
                val midY = (canvasCenter.y + node.center.y) / 2f
                drawCircle(
                    color = if (isSelected) node.color else hairlineColor,
                    radius = 3.5f * scale,
                    center = Offset(midX, midY)
                )
            }

            // 2. Draw satellite nodes
            nodes.forEach { node ->
                val isSelected = selectedFact?.id == node.fact.id

                // Selection halo
                if (isSelected) {
                    drawCircle(
                        color = node.color.copy(alpha = 0.25f),
                        radius = node.radius * 1.55f,
                        center = node.center
                    )
                    drawCircle(
                        color = node.color,
                        radius = node.radius * 1.25f,
                        center = node.center,
                        style = Stroke(width = 2f)
                    )
                }

                // Node circle
                drawCircle(
                    color = surfaceColor,
                    radius = node.radius,
                    center = node.center
                )
                drawCircle(
                    color = node.color.copy(alpha = 0.9f),
                    radius = node.radius - (2f * scale),
                    center = node.center
                )
                drawCircle(
                    color = surfaceColor.copy(alpha = 0.9f),
                    radius = node.radius,
                    center = node.center,
                    style = Stroke(width = 1.5f * scale)
                )

                // Node label text
                val textLayout = textMeasurer.measure(
                    text = node.label,
                    style = TextStyle(
                        color = inkColor,
                        fontSize = (10f * scale).sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = node.label,
                    topLeft = Offset(
                        x = node.center.x - (textLayout.size.width / 2f),
                        y = node.center.y + node.radius + (4f * scale)
                    ),
                    style = TextStyle(
                        color = inkColor,
                        fontSize = (10f * scale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // 3. Draw central root node (User / Mind of Maia)
            val rootRadius = with(density) { 38.dp.toPx() } * scale

            // Pulsing background glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.35f),
                        rootColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = canvasCenter,
                    radius = rootRadius * pulseGlow
                ),
                radius = rootRadius * pulseGlow,
                center = canvasCenter
            )

            // Center Orb
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(rootColor, glowColor),
                    start = Offset(canvasCenter.x - rootRadius, canvasCenter.y - rootRadius),
                    end = Offset(canvasCenter.x + rootRadius, canvasCenter.y + rootRadius)
                ),
                radius = rootRadius,
                center = canvasCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = rootRadius,
                center = canvasCenter,
                style = Stroke(width = 2f * scale)
            )

            // Center Name text
            val rootLayout = textMeasurer.measure(
                text = userName,
                style = TextStyle(
                    color = Color.White,
                    fontSize = (13f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            drawText(
                textMeasurer = textMeasurer,
                text = userName,
                topLeft = Offset(
                    x = canvasCenter.x - (rootLayout.size.width / 2f),
                    y = canvasCenter.y - (rootLayout.size.height / 2f)
                ),
                style = TextStyle(
                    color = Color.White,
                    fontSize = (13f * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Floating Controls: Zoom Reset
        Surface(
            onClick = {
                scale = 1f
                panOffset = Offset.Zero
            },
            shape = CircleShape,
            color = surfaceColor,
            border = BorderStroke(1.dp, hairlineColor),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ZoomOutMap,
                    contentDescription = "Reset view",
                    tint = inkMutedColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Empty state overlay if no facts yet
        if (facts.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = surfaceColor.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, hairlineColor),
                    shadowElevation = 3.dp
                ) {
                    Text(
                        text = "Maia's mental map is ready. Chat with her or load the demo persona in Settings to see her memory graph form in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = inkMutedColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Floating Node Detail Sheet / Card when a node is selected
        AnimatedVisibility(
            visible = selectedFact != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (selectedFact != null) {
                val fact = selectedFact!!
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, hairlineColor),
                    modifier = Modifier.fillMaxWidth().testTag("node_inspector_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = rootColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = fact.source.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = rootColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { selectedFact = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close inspector",
                                    tint = inkMutedColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = fact.factKey,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = inkColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fact.factValue,
                            style = MaterialTheme.typography.bodyLarge,
                            color = inkColor,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onWhyClick(fact)
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = rootColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Why I Know This", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Surface(
                                onClick = {
                                    onDeleteClick(fact)
                                    selectedFact = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = dangerColor.copy(alpha = 0.12f),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Forget fact",
                                        tint = dangerColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Forget", color = dangerColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

