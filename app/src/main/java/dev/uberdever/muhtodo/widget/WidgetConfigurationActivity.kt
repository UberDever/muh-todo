package dev.uberdever.muhtodo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.uberdever.muhtodo.AppServices
import dev.uberdever.muhtodo.MainActivity
import dev.uberdever.muhtodo.document.WidgetOptions
import dev.uberdever.muhtodo.document.DocumentPreferences
import dev.uberdever.muhtodo.R
import dev.uberdever.muhtodo.ui.TodoTheme
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException

class WidgetConfigurationActivity : ComponentActivity() {
    internal var options by mutableStateOf(WidgetOptions())
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val preferences by lazy { DocumentPreferences(this) }
    private var revision by mutableIntStateOf(0)

    private fun ownsWidget() = widgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
        AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider == ComponentName(this, WidgetProvider::class.java)

    internal fun saveConfiguration() {
        if (isFinishing || !ownsWidget()) return
        preferences.setWidgetOptions(widgetId, options)
        WidgetProvider.render(this, AppWidgetManager.getInstance(this), intArrayOf(widgetId))
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("widget-id", widgetId)
        outState.putBoolean("incomplete-first", options.sortCompletion)
        outState.putBoolean("sort-tags", options.sortTags)
        outState.putBoolean("completion-descending", options.completionDescending)
        outState.putBoolean("tags-descending", options.tagsDescending)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() { super.onResume(); revision++ }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (!ownsWidget()) { finish(); return }
        options = preferences.widgetOptions(widgetId)
        if (savedInstanceState?.getInt("widget-id", AppWidgetManager.INVALID_APPWIDGET_ID) == widgetId) {
            options = WidgetOptions(
                savedInstanceState.getBoolean("incomplete-first"), savedInstanceState.getBoolean("sort-tags"),
                savedInstanceState.getBoolean("completion-descending"), savedInstanceState.getBoolean("tags-descending"),
            )
        }
        setContent {
            TodoTheme {
                var ready by remember { mutableStateOf(false) }
                var message by remember { mutableStateOf("Opening document…") }
                LaunchedEffect(revision) {
                    try { AppServices.repository(this@WidgetConfigurationActivity).read(); ready = true; message = "Dates appear newest first. Sorting changes only this widget's view." }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { ready = false; message = e.message ?: "Select your Markdown file." }
                }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.widget_name), style = MaterialTheme.typography.headlineSmall)
                        Text(message)
                        SortControl(stringResource(R.string.sort_completion), options.sortCompletion, options.completionDescending,
                            onEnabledChange = { options = options.copy(sortCompletion = it) },
                            onDescendingChange = { options = options.copy(completionDescending = it) },
                            hint = stringResource(R.string.completion_sort_hint))
                        SortControl(stringResource(R.string.sort_tags), options.sortTags, options.tagsDescending,
                            onEnabledChange = { options = options.copy(sortTags = it) },
                            onDescendingChange = { options = options.copy(tagsDescending = it) })
                        OutlinedButton(onClick = { startActivity(Intent(this@WidgetConfigurationActivity, MainActivity::class.java)) }) { Text("Select or change document") }
                        Button(enabled = ready, onClick = { saveConfiguration() }) { Text(stringResource(R.string.save_widget_settings)) }
                        TextButton(onClick = { finish() }) { Text(stringResource(R.string.cancel_widget_settings)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortControl(label: String, enabled: Boolean, descending: Boolean,
    onEnabledChange: (Boolean) -> Unit, onDescendingChange: (Boolean) -> Unit, hint: String? = null) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
            value = enabled, role = Role.Checkbox, onValueChange = onEnabledChange), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = enabled, onCheckedChange = null)
            Text(label, Modifier.padding(start = 12.dp))
        }
        if (enabled) {
            Column(Modifier.padding(start = 24.dp).selectableGroup()) {
                SortDirectionOption(stringResource(R.string.sort_ascending), !descending) { onDescendingChange(false) }
                SortDirectionOption(stringResource(R.string.sort_descending), descending) { onDescendingChange(true) }
            }
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun SortDirectionOption(label: String, selected: Boolean, choose: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = choose),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 12.dp))
    }
}
