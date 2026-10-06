package dev.uberdever.muhtodo.editor

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import dev.uberdever.muhtodo.R

@Composable
fun EditorScreen(state: EditorState, onChange: (EditorState) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val fields = state.fields
    val enabled = !state.saving
    val validation = state.validationError()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (state.ref == null) "New todo" else "Edit todo", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(enabled = enabled, onClick = {
            DatePickerDialog(context, { _, year, month, day -> onChange(state.withDate(LocalDate.of(year, month + 1, day))) },
                fields.date.year, fields.date.monthValue - 1, fields.date.dayOfMonth).apply {
                datePicker.minDate = Calendar.getInstance().apply { set(2000, 0, 1, 0, 0, 0) }.timeInMillis
                datePicker.maxDate = Calendar.getInstance().apply { set(2099, 11, 31, 23, 59, 59) }.timeInMillis
            }.show()
        }) { Text(fields.date.format(DateTimeFormatter.ofPattern("dd.MM.yy"))) }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(fields.hasCheckbox, { onChange(state.copy(fields = fields.copy(hasCheckbox = it, completed = fields.completed && it), error = null)) }, enabled = enabled)
            Text(stringResource(R.string.use_checkbox))
        }
        if (fields.hasCheckbox) Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(fields.completed, { onChange(state.copy(fields = fields.copy(completed = it), error = null)) }, enabled = enabled)
            Text("Completed")
        }
        if (state.ref == null) Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(state.inheritTags, { onChange(state.copy(inheritTags = it, error = null)) }, enabled = enabled && state.canInherit)
            Text("Inherit tags")
        }
        OutlinedTextField(
            value = (if (state.inheritTags) state.inheritedTags else fields.tags).joinToString(" "),
            onValueChange = { value -> onChange(state.copy(fields = fields.copy(tags = if (value.isEmpty()) emptyList() else value.split(" ")), error = null)) },
            enabled = enabled && !state.inheritTags, label = { Text("Tags, in order") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (!state.inheritTags && state.snapshot.document.knownTags.isNotEmpty()) {
            Text("Append a known tag", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.snapshot.document.knownTags.forEach { tag ->
                    SuggestionChip(onClick = { onChange(state.copy(fields = fields.copy(tags = fields.tags + tag), error = null)) },
                        label = { Text(tag) }, enabled = enabled)
                }
            }
        }
        OutlinedTextField(fields.body, { onChange(state.copy(fields = fields.copy(body = it), error = null)) },
            enabled = enabled, label = { Text("Todo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        (state.error ?: validation?.takeIf { fields.body.isNotEmpty() })?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel, enabled = enabled) { Text("Cancel") }
            Button(onClick = onSave, enabled = enabled && validation == null) { Text(if (state.saving) "Saving…" else "Save") }
        }
    }
}
