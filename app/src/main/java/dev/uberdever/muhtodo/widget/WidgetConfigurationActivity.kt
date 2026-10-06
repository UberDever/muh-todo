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
        outState.putBoolean("incomplete-first", options.incompleteFirst)
        outState.putBoolean("sort-tags", options.sortTags)
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
            options = WidgetOptions(savedInstanceState.getBoolean("incomplete-first"), savedInstanceState.getBoolean("sort-tags"))
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
                        Text(stringResource(R.string.completion_order), style = MaterialTheme.typography.titleMedium)
                        Column(Modifier.selectableGroup()) {
                            CompletionOption(stringResource(R.string.order_as_is), !options.incompleteFirst) {
                                options = options.copy(incompleteFirst = false)
                            }
                            CompletionOption(stringResource(R.string.order_incomplete_first), options.incompleteFirst) {
                                options = options.copy(incompleteFirst = true)
                            }
                        }
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                            value = options.sortTags, role = Role.Checkbox,
                            onValueChange = { options = options.copy(sortTags = it) }), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = options.sortTags, onCheckedChange = null)
                            Text(stringResource(R.string.sort_tags), Modifier.padding(start = 12.dp))
                        }
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
private fun CompletionOption(label: String, selected: Boolean, choose: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = choose),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 12.dp))
    }
}
