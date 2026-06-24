package com.pcodcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Diet → 🥗 light green, Exercise → 🧘‍♀️ lavender, Lifestyle → 🌿 peach.
// Single source of truth — use these everywhere a plan-item category appears.

/**
 * Always-dark text color for content sitting on a category-tinted card.
 * Category backgrounds (PastelGreenLight / PastelLavenderLight / PastelPeachLight)
 * stay LIGHT in both themes to preserve identity — so text on them must NOT use
 * MaterialTheme.colorScheme.onSurface (which flips to a near-white in dark mode
 * and disappears against the light bg). Use this constant instead.
 */
val OnCategoryPastel = Color(0xFF2D2235)

fun categoryEmoji(category: String): String = when (category) {
    "Diet" -> "🥗"
    "Exercise" -> "🧘‍♀️"
    "Lifestyle" -> "🌿"
    else -> "✨"
}

@Composable
@ReadOnlyComposable
fun categoryBackground(category: String): Color = when (category) {
    "Diet" -> PastelGreenLight
    "Exercise" -> PastelLavenderLight
    "Lifestyle" -> PastelPeachLight
    else -> MaterialTheme.colorScheme.surface
}

@Composable
@ReadOnlyComposable
fun categoryAccent(category: String): Color = when (category) {
    "Diet" -> PastelGreenDark
    "Exercise" -> PastelLavenderDark
    "Lifestyle" -> PastelPeachDark
    else -> MaterialTheme.colorScheme.primary
}
