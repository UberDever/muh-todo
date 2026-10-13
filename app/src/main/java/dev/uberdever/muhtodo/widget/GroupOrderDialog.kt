package dev.uberdever.muhtodo.widget

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.uberdever.muhtodo.R
import dev.uberdever.muhtodo.document.EntryGroup
import dev.uberdever.muhtodo.document.EntryGroupOrder

@Composable
internal fun groupLabel(group: EntryGroup): String = stringResource(when (group) {
    EntryGroup.INCOMPLETE -> R.string.group_incomplete
    EntryGroup.COMPLETE -> R.string.group_complete
    EntryGroup.BULLETS -> R.string.group_bullets
})

@Composable
internal fun GroupOrderDialog(order: List<EntryGroup>, onConfirm: (List<EntryGroup>) -> Unit, onDismiss: () -> Unit) {
    var draft by rememberSaveable(stateSaver = listSaver(
        save = { groups: List<EntryGroup> -> groups.map { it.name } },
        restore = { EntryGroupOrder.parse(it) },
    )) { mutableStateOf(EntryGroupOrder.validated(order)) }
    val step = with(LocalDensity.current) { 72.dp.toPx() }
    var dragging by remember { mutableStateOf<EntryGroup?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_completion)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.drag_group_hint))
                draft.forEachIndexed { index, group -> key(group) {
                    val label = groupLabel(group)
                    Surface(color = if (dragging == group) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.small) {
                        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(40.dp).fillMaxHeight().pointerInput(group) {
                                var remainder = 0f
                                detectDragGestures(
                                    onDragStart = { remainder = 0f; dragging = group },
                                    onDragEnd = { dragging = null },
                                    onDragCancel = { dragging = null },
                                ) { change, amount ->
                                    change.consume()
                                    remainder += amount.y
                                    while (remainder > step / 2 && draft.indexOf(group) < 2) {
                                        draft = EntryGroupOrder.move(draft, group, draft.indexOf(group) + 1)
                                        remainder -= step
                                    }
                                    while (remainder < -step / 2 && draft.indexOf(group) > 0) {
                                        draft = EntryGroupOrder.move(draft, group, draft.indexOf(group) - 1)
                                        remainder += step
                                    }
                                    remainder = remainder.coerceIn(-step / 2, step / 2)
                                }
                            }, contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_drag_handle), stringResource(R.string.drag_group, label), Modifier.size(20.dp))
                            }
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { draft = EntryGroupOrder.move(draft, group, index - 1) }, enabled = index > 0) {
                                Icon(painterResource(R.drawable.ic_move_up), stringResource(R.string.move_group_up, label))
                            }
                            IconButton(onClick = { draft = EntryGroupOrder.move(draft, group, index + 1) }, enabled = index < 2) {
                                Icon(painterResource(R.drawable.ic_move_down), stringResource(R.string.move_group_down, label))
                            }
                        }
                    }
                } }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(draft) }) { Text(stringResource(R.string.group_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_widget_settings)) } },
    )
}
