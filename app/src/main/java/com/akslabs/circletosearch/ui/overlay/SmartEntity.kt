/*
 * Copyright (C) 2025 AKS-Labs (original author)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.akslabs.circletosearch.ui.overlay

import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.net.Uri
import android.util.Patterns
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.akslabs.circletosearch.R
import uniffi.translator_core.PositionedWord
import kotlin.math.max
import kotlin.math.min

sealed class SmartEntity(
    val text: String,
    val bounds: RectF,
    val typeName: String,
    val icon: ImageVector,
    val sourceColor: Color,
) {
    class Url(text: String, bounds: RectF, typeName: String) :
        SmartEntity(text, bounds, typeName, Icons.Default.Link, Color(0xFF1A73E8))

    class Email(text: String, bounds: RectF, typeName: String) :
        SmartEntity(text, bounds, typeName, Icons.Default.Email, Color(0xFF1A73E8))

    class Phone(text: String, bounds: RectF, typeName: String) :
        SmartEntity(text, bounds, typeName, Icons.Default.Phone, Color(0xFF43A047))

    class Upi(text: String, bounds: RectF, typeName: String) :
        SmartEntity(text, bounds, typeName, Icons.Default.Person, Color(0xFF8E24AA))
}

internal fun SmartEntity.toActionIntent(): Intent = when (this) {
    is SmartEntity.Url -> Intent(
        Intent.ACTION_VIEW,
        Uri.parse(if (!text.startsWith("http")) "http://$text" else text),
    )
    is SmartEntity.Email -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$text"))
    is SmartEntity.Phone -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(text)}"))
    is SmartEntity.Upi -> Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay?pa=${Uri.encode(text)}"))
}

private val urlRegex = Patterns.WEB_URL.toRegex()
private val emailRegex = Patterns.EMAIL_ADDRESS.toRegex()
private val phoneRegexLine = Regex("""(\+?[\d\s\-\(\).]{9,20})""")
private val upiRegex = Regex("""[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}""")

internal fun extractSmartEntities(
    sourceWords: List<PositionedWord>,
    context: Context,
): List<SmartEntity> {
    val entities = mutableListOf<SmartEntity>()

    sourceWords.groupBy { it.lineIndex }.forEach { (_, lineWords) ->
        val lineText = lineWords.joinToString(" ") { it.text }
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        lineWords.forEach { w ->
            left = min(left, w.bounds.cx - w.bounds.width / 2f)
            top = min(top, w.bounds.cy - w.bounds.height / 2f)
            right = max(right, w.bounds.cx + w.bounds.width / 2f)
            bottom = max(bottom, w.bounds.cy + w.bounds.height / 2f)
        }
        phoneRegexLine.findAll(lineText).forEach { match ->
            val phoneCandidate = match.value.trim()
            if (phoneCandidate.count { it.isDigit() } >= 10) {
                val startIdx = match.range.first
                val endIdx = match.range.last + 1
                val ratioStart = startIdx.toFloat() / lineText.length.coerceAtLeast(1)
                val ratioEnd = endIdx.toFloat() / lineText.length.coerceAtLeast(1)
                val entityBounds = RectF(
                    left + (ratioStart * (right - left)),
                    top,
                    left + (ratioEnd * (right - left)),
                    bottom,
                )
                entities.add(SmartEntity.Phone(phoneCandidate, entityBounds, context.getString(R.string.entity_type_phone)))
            }
        }
    }

    sourceWords.forEach { word ->
        val txt = word.text.trim()
        val wordBounds = RectF(
            word.bounds.cx - word.bounds.width / 2f,
            word.bounds.cy - word.bounds.height / 2f,
            word.bounds.cx + word.bounds.width / 2f,
            word.bounds.cy + word.bounds.height / 2f,
        )
        when {
            emailRegex.matches(txt) ->
                entities.add(SmartEntity.Email(txt, wordBounds, context.getString(R.string.entity_type_email)))
            upiRegex.matches(txt) ->
                entities.add(SmartEntity.Upi(txt, wordBounds, context.getString(R.string.entity_type_upi)))
            urlRegex.matches(txt) ->
                entities.add(SmartEntity.Url(txt, wordBounds, context.getString(R.string.entity_type_link)))
        }
    }

    return entities
}
