/*
 * Copyright (C) 2025 AKS-Labs
 */

package com.akslabs.circletosearch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akslabs.circletosearch.R
import com.akslabs.circletosearch.data.ActionType
import com.akslabs.circletosearch.data.OverlaySegment

@Composable
fun SegmentEditorItem(
    index: Int,
    segment: OverlaySegment,
    overlayPalette: List<Color>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onUpdate: (OverlaySegment) -> Unit,
    onDelete: () -> Unit,
) {
    var showGestureDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val maxWidth = maxOf(10f, metrics.widthPixels.toFloat())
    val maxHeight = maxOf(10f, metrics.heightPixels.toFloat())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onExpandToggle() },
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = overlayPalette[index % overlayPalette.size],
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.label_overlay_number, index + 1), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.label_x_position, segment.xOffset), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = segment.xOffset.toFloat().coerceIn(0f, maxWidth),
                    onValueChange = { onUpdate(segment.copy(xOffset = it.toInt())) },
                    valueRange = 0f..maxWidth,
                )

                Text(stringResource(R.string.label_y_position, segment.yOffset), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = segment.yOffset.toFloat().coerceIn(0f, maxHeight),
                    onValueChange = { onUpdate(segment.copy(yOffset = it.toInt())) },
                    valueRange = 0f..maxHeight,
                )

                Text(stringResource(R.string.label_width, segment.width), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = segment.width.toFloat().coerceIn(10f, maxWidth),
                    onValueChange = { onUpdate(segment.copy(width = it.toInt())) },
                    valueRange = 10f..maxWidth,
                )

                Text(stringResource(R.string.label_height, segment.height), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = segment.height.toFloat().coerceIn(10f, maxHeight),
                    onValueChange = { onUpdate(segment.copy(height = it.toInt())) },
                    valueRange = 10f..maxHeight,
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGestureDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.section_gestures), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.gestures_subtitle), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    segment.gestures.entries.filter { it.value != ActionType.NONE }.take(3).forEach {
                        Text(
                            "${it.key.getFriendlyName(context)}: ${it.value.getFriendlyName(context)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }

    if (showGestureDialog) {
        GestureConfigDialog(
            segment = segment,
            onDismiss = { showGestureDialog = false },
            onUpdate = onUpdate,
        )
    }
}
