/*
 * Copyright (C) 2025 AKS-Labs (original author)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.akslabs.circletosearch.ui.overlay

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.akslabs.circletosearch.ui.theme.OverlayGradientColors
import com.akslabs.circletosearch.ui.theme.rememberReducedMotion
import kotlin.math.abs

/**
 * Cyclic interpolation over the Google palette for a flowing border gradient:
 * t wraps at 1.0 and the palette starts/ends on blue, so the loop is seamless.
 */
internal fun flowingGoogleColor(t: Float): Color {
    val p = OverlayGradientColors
    val x = (((t % 1f) + 1f) % 1f) * (p.size - 1)
    val i = x.toInt().coerceAtMost(p.size - 2)
    val f = x - i
    val a = p[i]
    val b = p[i + 1]
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f,
    )
}

/**
 * Screen-edge border: the thin 2dp gradient line over a blurred neon glow whose
 * colors flow around the border and breathe in brightness. Respects system
 * reduced-motion (`ANIMATOR_DURATION_SCALE == 0f`) by rendering a static border.
 */
@Composable
internal fun AnimatedNeonBorder(
    visible: Boolean,
    shape: Shape,
) {
    val reducedMotion = rememberReducedMotion()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(if (reducedMotion) 0 else 200)),
    ) {
        if (reducedMotion) {
            val staticBrush = remember { Brush.sweepGradient(OverlayGradientColors) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 2.dp, brush = staticBrush, shape = shape),
            )
            return@AnimatedVisibility
        }

        val transition = rememberInfiniteTransition(label = "BorderGlow")
        val flowPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "BorderFlow",
        )
        val chasePhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "BorderChase",
        )
        val glowAlpha by transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "BorderPulse",
        )
        val borderBrush = Brush.sweepGradient(
            *Array(33) { i ->
                val t = i / 32f
                val base = flowingGoogleColor(t + flowPhase)
                val raw = abs(t - chasePhase)
                val d = minOf(raw, 1f - raw)
                val c = (1f - d / 0.09f).coerceIn(0f, 1f)
                val smooth = c * c
                t to (if (smooth <= 0.001f) base else Color(
                    red = base.red + (1f - base.red) * smooth,
                    green = base.green + (1f - base.green) * smooth,
                    blue = base.blue + (1f - base.blue) * smooth,
                    alpha = base.alpha + (1f - base.alpha) * smooth,
                ))
            },
        )
        Box(modifier = Modifier.fillMaxSize()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = glowAlpha
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(12f, 12f, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .border(width = 3.dp, brush = borderBrush, shape = shape),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 2.dp, brush = borderBrush, shape = shape),
            )
        }
    }
}
