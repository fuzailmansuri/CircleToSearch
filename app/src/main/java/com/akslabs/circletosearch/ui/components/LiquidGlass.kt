/*
 * Copyright (C) 2025 AKS-Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.akslabs.circletosearch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akslabs.circletosearch.ui.theme.m3ExpressivePress

/**
 * Liquid Glass material variants, modeled after iOS 26 Liquid Glass guidelines:
 * - [Regular]: Standard translucent glass for floating docks, sheets, and grouped controls.
 * - [Clear]: Lighter translucent glass with contrast-safe scrim for over-screenshot buttons.
 * - [Prominent]: Tinted glass reserved for primary/high-emphasis actions.
 */
sealed class LiquidGlassStyle {
    data object Regular : LiquidGlassStyle()
    data object Clear : LiquidGlassStyle()
    data class Prominent(val tint: Color) : LiquidGlassStyle()
}

/**
 * Groups sibling floating Liquid Glass controls with consistent spacing so they
 * read as a cohesive control cluster.
 */
@Composable
fun LiquidGlassContainer(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

/**
 * Translucent Liquid Glass surface with directional specular rim highlight.
 * Set [interactive] = true only for tappable controls; static status badges
 * should keep [interactive] = false.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    style: LiquidGlassStyle = LiquidGlassStyle.Regular,
    interactive: Boolean = false,
    shadowElevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme

    val containerColor = when (style) {
        LiquidGlassStyle.Regular -> if (isDark) {
            scheme.surfaceContainerHigh.copy(alpha = 0.78f)
        } else {
            scheme.surfaceContainerLowest.copy(alpha = 0.82f)
        }
        LiquidGlassStyle.Clear -> if (isDark) {
            Color.Black.copy(alpha = 0.48f)
        } else {
            Color(0xFF1A1C1E).copy(alpha = 0.44f)
        }
        is LiquidGlassStyle.Prominent -> {
            val base = if (isDark) scheme.surfaceContainerHigh.copy(alpha = 0.80f)
            else scheme.surfaceContainerLowest.copy(alpha = 0.84f)
            style.tint.copy(alpha = 0.20f).compositeOver(base)
        }
    }

    val rimBrush = when (style) {
        is LiquidGlassStyle.Prominent -> Brush.linearGradient(
            colors = listOf(
                style.tint.copy(alpha = 0.55f),
                Color.White.copy(alpha = if (isDark) 0.14f else 0.35f),
                style.tint.copy(alpha = 0.18f),
            ),
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
        LiquidGlassStyle.Clear -> Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.10f),
            ),
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
        LiquidGlassStyle.Regular -> Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.24f else 0.65f),
                scheme.outlineVariant.copy(alpha = if (isDark) 0.18f else 0.28f),
            ),
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }

    val pressModifier = if (interactive) {
        Modifier.m3ExpressivePress(pressedScale = 0.94f, onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(pressModifier),
        shape = shape,
        color = containerColor,
        shadowElevation = shadowElevation,
        border = BorderStroke(1.dp, rimBrush),
    ) {
        Box(content = content)
    }
}

/**
 * Circular Liquid Glass icon button supporting tap and optional long-press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    style: LiquidGlassStyle = LiquidGlassStyle.Clear,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidGlassSurface(
        modifier = modifier.size(size),
        shape = CircleShape,
        style = style,
        interactive = true,
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
