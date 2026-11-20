package com.example.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * Earth-tone design tokens for Maia.
 *
 * This file defines the complete color palette using semantic roles.
 * All composables must use MaiaColors roles, never raw hex values.
 */

/**
 * Semantic color roles for the Maia design system.
 *
 * Usage rules:
 * - maiaPresence (terracotta) is reserved for Maia herself: orb, streaming indicator,
 *   memory chip dot, and single primary action button per screen. Never for decoration.
 * - maiaGlow (amber) only for orb gradient and glow/breathing states. Never for UI controls.
 * - success (sage) only for completed/confirmed states. Not for toggles or positive copy.
 * - Maia's chat responses are naked ink text on canvas; user messages use userBubble.
 */
data class MaiaColors(
    val canvas: Color,          // App background
    val surface: Color,         // Cards, sheets, input fields
    val ink: Color,             // Primary text
    val inkMuted: Color,        // Secondary text, timestamps, hints
    val hairline: Color,        // Borders, dividers
    val userBubble: Color,      // User chat bubble fill
    val maiaPresence: Color,    // Terracotta - Maia's brand color (orb, indicators, primary actions)
    val maiaGlow: Color,        // Amber - Orb gradient and glow effects only
    val success: Color,         // Sage - Completion/confirmation states only
    val danger: Color,          // Brick red - Destructive actions only
)

// Light theme — "morning hearth"
val LightMaiaColors = MaiaColors(
    canvas = Color(0xFFF8F5F0),       // Morning hearth background
    surface = Color(0xFFFFFFFF),      // Clean white cards
    ink = Color(0xFF241D15),          // Rich primary text
    inkMuted = Color(0xFF5F5550),     // Muted secondary text (darkened for WCAG AA)
    hairline = Color(0xFFE8E2D9),     // Subtle borders
    userBubble = Color(0xFFEFEAE2),   // User message bubble
    maiaPresence = Color(0xFFAF4F35), // Terracotta brand (darkened for button text contrast)
    maiaGlow = Color(0xFFE0A458),     // Amber glow
    success = Color(0xFF7D8C6F),      // Sage confirmation
    danger = Color(0xFFA8443A),       // Warm brick red
)

// Dark theme — "evening hearth"
val DarkMaiaColors = MaiaColors(
    canvas = Color(0xFF171310),       // Evening hearth background
    surface = Color(0xFF221C16),      // Warm dark cards
    ink = Color(0xFFF1EAE1),          // Soft cream text
    inkMuted = Color(0xFFA69A8C),     // Muted secondary text
    hairline = Color(0xFF322B24),     // Subtle dark borders
    userBubble = Color(0xFF2A231C),   // User message bubble
    maiaPresence = Color(0xFFB5582F), // Lifted terracotta (darkened for button text contrast)
    maiaGlow = Color(0xFFE8B36A),     // Lifted amber
    success = Color(0xFF93A383),      // Lifted sage
    danger = Color(0xFFC05548),       // Lifted brick red
)
