package dev.uberdever.muhtodo.editor

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
    val initialTags = (if (state.inheritTags) state.inheritedTags else fields.tags).joinToString(" ")
    var tagInput by rememberSaveable(state.ref, state.insertAfter, state.snapshot.uri.toString(), stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialTags, TextRange(initialTags.length)))
    }
    fun changeTags(value: TextFieldValue) {
        tagInput = value
        onChange(state.copy(fields = fields.copy(tags = if (value.text.isEmpty()) emptyList() else value.text.split(" ")), error = null))
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (state.ref == null) "New todo" else "Edit todo", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(enabled = enabled && state.insertAfter == null, onClick = {
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
        OutlinedTextField(
            value = tagInput,
            onValueChange = ::changeTags,
            enabled = enabled && !state.inheritTags, label = { Text("Tags, in order") },
            supportingText = { Text(stringResource(R.string.tag_format_example)) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (!state.inheritTags && state.snapshot.document.knownTags.isNotEmpty()) {
            Text("Insert a known tag", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TagInput.knownTags(state.snapshot.document.knownTags).forEach { tag ->
                    SuggestionChip(onClick = { changeTags(TagInput.insert(tagInput, tag)) },
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
