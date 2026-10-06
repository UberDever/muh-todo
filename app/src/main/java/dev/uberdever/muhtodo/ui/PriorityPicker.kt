package dev.uberdever.muhtodo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.uberdever.muhtodo.document.PriorityTags
import kotlin.math.roundToInt

internal object PriorityAdjustment {
    fun drag(start: Int, totalPixels: Float, pixelsPerStep: Float): Int =
        (start + (totalPixels / pixelsPerStep).roundToInt()).coerceIn(0, 99)
}

@Composable
fun PriorityPicker(tags: List<String>, enabled: Boolean, onChange: (Int) -> Unit) {
    val value = PriorityTags.value(tags)
    val currentValue by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val pixelsPerStep = with(LocalDensity.current) { 8.dp.toPx() }
    var fine by rememberSaveable { mutableStateOf(false) }
    val coarseValues = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 99)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Priority", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (fine) TextButton(enabled = enabled && value > 0, onClick = { onChange(value - 1) }) {
                Text("−", modifier = Modifier.semantics { contentDescription = "Decrease priority" })
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color(TagColors.color(tags)).copy(alpha = .12f),
                border = BorderStroke(1.dp, Color(TagColors.color(tags)).copy(alpha = .5f)),
                modifier = Modifier
                    .combinedClickable(enabled = enabled, onClick = { fine = !fine }, onLongClick = { fine = true })
                    .pointerInput(enabled, pixelsPerStep) {
                        if (enabled) {
                            var start = 0
                            var travel = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { start = currentValue; travel = 0f; fine = true },
                                onHorizontalDrag = { event, distance ->
                                    event.consume()
                                    travel += distance
                                    change(PriorityAdjustment.drag(start, travel, pixelsPerStep))
                                },
                            )
                        }
                    }
                    .semantics {
                        contentDescription = "Priority $value. Tap for exact adjustment or drag horizontally."
                        progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..99f, 98)
                        if (enabled) {
                            setProgress { change(it.roundToInt().coerceIn(0, 99)); true }
                            customActions = listOf(
                                CustomAccessibilityAction("Increase priority") { change((currentValue + 1).coerceAtMost(99)); true },
                                CustomAccessibilityAction("Decrease priority") { change((currentValue - 1).coerceAtLeast(0)); true },
                            )
                        }
                    },
            ) {
                Box(Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 48.dp), contentAlignment = Alignment.Center) {
                    Text(value.toString(), color = Color(TagColors.color(tags)), style = MaterialTheme.typography.titleMedium)
                }
            }
            if (fine) TextButton(enabled = enabled && value < 99, onClick = { onChange(value + 1) }) {
                Text("+", modifier = Modifier.semantics { contentDescription = "Increase priority" })
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), maxItemsInEachRow = 6) {
            coarseValues.forEach { candidate ->
                val color = Color(TagColors.color(PriorityTags.withPriority(tags, candidate)))
                Surface(
                    onClick = { fine = false; onChange(candidate) }, enabled = enabled,
                    shape = MaterialTheme.shapes.small,
                    color = color.copy(alpha = if (candidate == value) .25f else .06f),
                    border = BorderStroke(if (candidate == value) 2.dp else 1.dp, color.copy(alpha = if (candidate == value) 1f else .35f)),
                    modifier = Modifier.semantics { selected = candidate == value; contentDescription = "Priority $candidate" },
                ) {
                    Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), contentAlignment = Alignment.Center) {
                        Text(candidate.toString(), color = color)
                    }
                }
            }
        }
    }
}
