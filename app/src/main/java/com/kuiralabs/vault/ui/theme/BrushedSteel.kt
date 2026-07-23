package com.kuiralabs.vault.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.random.Random

/**
 * A detailed brushed-steel surface, five composited layers (cheapest first):
 *  1. base sheet — a diagonal gunmetal gradient, top-left lit, bottom-right shadowed;
 *  2. brushed grain — a 3px vertically-repeating gradient → fine horizontal mill lines, the
 *     defining "brushed" directionality (one tiled GPU shader, not thousands of draw calls);
 *  3. forge glow — a soft off-centre radial highlight, as if lit from above;
 *  4. specular streak — an angular sweep overlaid with Overlay blend, the glint on polished metal;
 *  5. grit — sparse deterministic speckle so the surface never reads as flat vector.
 *
 * Portable Canvas only (minSdk 30, no AGSL). Static — draws once and holds until resize.
 */
@Composable
fun VaultSteelBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().steelSurface(), content = content)
}

fun Modifier.steelSurface(): Modifier = drawWithCache {
    val w = size.width
    val h = size.height

    val baseSheet = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Steel.PlateHi,
            0.4f to Steel.Plate,
            1.0f to Steel.PlateLo,
        ),
        start = Offset(0f, 0f),
        end = Offset(w * 0.5f, h),
    )

    // Visible mill lines: strong alternating light/dark over a 3px period, tiled down the height.
    val grain = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color.White.copy(alpha = 0.11f),
            0.5f to Color.Black.copy(alpha = 0.16f),
            1.0f to Color.White.copy(alpha = 0.11f),
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, GRAIN_PERIOD_PX),
        tileMode = TileMode.Repeated,
    )

    val forgeGlow = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to Steel.Sheen.copy(alpha = 0.22f),
            0.55f to Steel.Sheen.copy(alpha = 0.06f),
            1.0f to Color.Transparent,
        ),
        center = Offset(w * 0.5f, h * 0.18f),
        radius = maxOf(w, h) * 0.8f,
    )

    val specular = Brush.sweepGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,
            0.10f to Color.White.copy(alpha = 0.16f),
            0.22f to Color.Transparent,
            0.50f to Color.White.copy(alpha = 0.09f),
            0.66f to Color.Transparent,
            1.00f to Color.Transparent,
        ),
        center = Offset(w * 0.5f, h * 0.26f),
    )

    val speckle = steelSpeckle(w, h)

    onDrawBehind {
        drawRect(baseSheet)
        drawRect(grain)
        drawRect(forgeGlow)
        drawRect(specular, blendMode = BlendMode.Overlay)
        speckle.forEach { (p, a) ->
            drawCircle(Color.White.copy(alpha = a), radius = 0.6f, center = p, style = Fill)
        }
    }
}

private fun steelSpeckle(w: Float, h: Float): List<Pair<Offset, Float>> {
    val rng = Random(SPECKLE_SEED)
    val count = ((w * h) / SPECKLE_DENSITY_PX).toInt().coerceIn(0, 4000)
    return List(count) {
        Offset(rng.nextFloat() * w, rng.nextFloat() * h) to (0.03f + rng.nextFloat() * 0.08f)
    }
}

private const val GRAIN_PERIOD_PX = 3f
private const val SPECKLE_SEED = 0x5EED_5EEDL
private const val SPECKLE_DENSITY_PX = 900f // 1 speck per ~900 px²
