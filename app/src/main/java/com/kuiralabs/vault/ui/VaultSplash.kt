package com.kuiralabs.vault.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Launch splash — a Compose recreation of `kuira-vault.svg` (dark radial ground + the KUIRA /
 * VAULT wordmark + taglines). Recreated rather than rasterised so the type stays crisp at any
 * density and fades cleanly into the steel UI. Colours, weights, and letter-spacing match the SVG.
 *
 * @param visible drives the fade — flip to false to dissolve into the main content.
 */
@Composable
fun VaultSplash(visible: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "splashFade",
    )
    Box(
        modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF17171B), Color(0xFF0A0A0B)),
                    center = Offset.Unspecified, // ~ 50% / 34% is close enough to centre-high
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "KUIRA",
                style = TextStyle(
                    color = Color(0xFF8A8A92), fontSize = 18.sp, fontWeight = FontWeight.W400,
                    letterSpacing = 14.sp, textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "VAULT",
                style = TextStyle(
                    color = Color(0xFFECECEF), fontSize = 60.sp, fontWeight = FontWeight.W300,
                    letterSpacing = 12.sp, textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "M-of-N multisig treasury",
                style = TextStyle(
                    color = Color(0xFF8A8A92), fontSize = 17.sp, fontWeight = FontWeight.W300,
                    letterSpacing = 1.sp, textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(48.dp))
            Text(
                "deposit · propose · approve · execute",
                style = TextStyle(
                    color = Color(0xFF5F5F67), fontSize = 15.sp, fontWeight = FontWeight.W300,
                    letterSpacing = 3.sp, textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
