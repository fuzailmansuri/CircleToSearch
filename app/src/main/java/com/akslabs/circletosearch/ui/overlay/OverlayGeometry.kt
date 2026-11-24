/*
 * Copyright (C) 2025 AKS-Labs (original author)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.akslabs.circletosearch.ui.overlay

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.RoundedCorner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.min

/**
 * Fit (contain) mapping for a bitmap centered in a view — the app-side mirror of the
 * module's internal fitTransform, which the word/scan overlays use. Unit-agnostic: pass
 * view/image sizes in px (crop path) or dp (chip path) consistently for both.
 */
internal class FitMapping(val scale: Float, val offsetX: Float, val offsetY: Float) {
    fun unmapX(x: Float) = if (scale == 0f) 0f else (x - offsetX) / scale
    fun unmapY(y: Float) = if (scale == 0f) 0f else (y - offsetY) / scale
}

internal fun fitMapping(viewW: Float, viewH: Float, imageW: Float, imageH: Float): FitMapping {
    if (viewW <= 0f || viewH <= 0f || imageW <= 0f || imageH <= 0f) {
        return FitMapping(1f, 0f, 0f)
    }
    val scale = min(viewW / imageW, viewH / imageH)
    return FitMapping(scale, (viewW - imageW * scale) / 2f, (viewH - imageH * scale) / 2f)
}

/**
 * Maps a screen-space rect into bitmap space for the ContentScale.Fit display, clamping to
 * the bitmap. For screen captures (bitmap size == view size) this is the identity.
 */
internal fun fitCrop(
    rect: Rect,
    t: FitMapping,
    imageW: Int,
    imageH: Int,
): Rect =
    Rect(
        t.unmapX(rect.left.toFloat()).toInt().coerceIn(0, imageW),
        t.unmapY(rect.top.toFloat()).toInt().coerceIn(0, imageH),
        t.unmapX(rect.right.toFloat()).toInt().coerceIn(0, imageW),
        t.unmapY(rect.bottom.toFloat()).toInt().coerceIn(0, imageH),
    )

/**
 * Smooths raw pointer samples into a flowing curve (Catmull-Rom -> cubic bezier),
 * so the drawn circle reads as a soft neon stroke instead of a jagged polyline.
 * With fewer than 3 points it falls back to straight segments.
 */
internal fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    val n = points.size
    if (n == 0) return path
    if (n < 3) {
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until n) path.lineTo(points[i].x, points[i].y)
        return path
    }
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until n - 1) {
        val p0 = points[if (i - 1 < 0) 0 else i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[if (i + 2 >= n) n - 1 else i + 2]
        val c1x = p1.x + (p2.x - p0.x) / 6f
        val c1y = p1.y + (p2.y - p0.y) / 6f
        val c2x = p2.x - (p3.x - p1.x) / 6f
        val c2y = p2.y - (p3.y - p1.y) / 6f
        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    return path
}

/**
 * The device's physical screen corner radii in px: upper-left, upper-right,
 * lower-right, lower-left.
 */
internal fun deviceCornerRadiiPx(context: Context): FloatArray {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val display = context.display
            ?: (context as? Activity)?.windowManager?.let {
                @Suppress("DEPRECATION")
                it.defaultDisplay
            }
        if (display != null) {
            fun radiusAt(position: Int): Float =
                display.getRoundedCorner(position)?.radius?.toFloat() ?: 0f
            return floatArrayOf(
                radiusAt(RoundedCorner.POSITION_TOP_LEFT),
                radiusAt(RoundedCorner.POSITION_TOP_RIGHT),
                radiusAt(RoundedCorner.POSITION_BOTTOM_RIGHT),
                radiusAt(RoundedCorner.POSITION_BOTTOM_LEFT),
            )
        }
    }
    val id = context.resources.getIdentifier("rounded_corner_radius", "dimen", "android")
    val r = if (id != 0) context.resources.getDimension(id) else 0f
    return floatArrayOf(r, r, r, r)
}
