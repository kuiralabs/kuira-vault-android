package com.kuiralabs.vault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The Vault's "Steel" palette — cool gunmetal greys with a single steel-blue accent, tuned to
 * sit on the brushed-steel texture ([brushedSteel]). Dark scheme only: a treasury reads as a
 * heavy metal object, not paper.
 */
object Steel {
    val Ink = Color(0xFF0E1013)          // deepest recess / scrim
    val Dark = Color(0xFF181C21)         // panel base (cards sit here, recessed)
    val Mid = Color(0xFF2B3138)          // raised metal
    val Light = Color(0xFF4A525B)        // lit edge
    val Sheen = Color(0xFFC9D2DB)        // specular streak
    val Accent = Color(0xFF7FA6C4)       // steel-blue — actions, focus
    val AccentDim = Color(0xFF4C6B85)
    val OnSteel = Color(0xFFE7EBEF)      // primary text
    val OnSteelDim = Color(0xFF9AA3AC)   // secondary text
    val Line = Color(0xFF39414A)         // hairline dividers

    // Brushed-plate tones for the background texture — a genuinely metallic mid-grey range so
    // the grain reads as steel; darker cards ([Dark]) then sit recessed on top of it.
    val PlateHi = Color(0xFF767F89)      // lit brushed steel (top)
    val Plate = Color(0xFF525A63)        // plate body
    val PlateLo = Color(0xFF31373E)      // shadowed plate (bottom)
}

@Composable
fun VaultSteelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Steel.Accent,
            onPrimary = Steel.Ink,
            secondary = Steel.Light,
            background = Steel.Ink,
            onBackground = Steel.OnSteel,
            surface = Steel.Dark,
            onSurface = Steel.OnSteel,
            surfaceVariant = Steel.Mid,
            onSurfaceVariant = Steel.OnSteelDim,
            outline = Steel.Line,
        ),
        content = content,
    )
}
