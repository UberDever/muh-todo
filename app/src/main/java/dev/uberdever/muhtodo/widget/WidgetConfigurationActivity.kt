package dev.uberdever.muhtodo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
        outState.putBoolean("sort-dates", options.sortDates)
        outState.putBoolean("dates-older-first", options.datesOlderFirst)
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
                savedInstanceState.getBoolean("sort-dates"), savedInstanceState.getBoolean("dates-older-first"),
            )
        }
        setContent {
            TodoTheme {
                var ready by remember { mutableStateOf(false) }
                var message by remember { mutableStateOf("Opening document…") }
                LaunchedEffect(revision) {
                    try { AppServices.repository(this@WidgetConfigurationActivity).read(); ready = true; message = "Sorting changes only this widget’s view. Completion, then tags, then date; ties keep file order." }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { ready = false; message = e.message ?: "Select your Markdown file." }
                }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.widget_name), style = MaterialTheme.typography.headlineSmall)
                        Text(message)
                        SortControl(stringResource(R.string.sort_completion), options.sortCompletion, options.completionDescending,
                            labels = listOf(stringResource(R.string.no_sort), stringResource(R.string.incomplete_first), stringResource(R.string.complete_first)),
                            onChange = { enabled, descending -> options = options.copy(sortCompletion = enabled, completionDescending = descending) })
                        SortControl(stringResource(R.string.sort_tags), options.sortTags, options.tagsDescending,
                            labels = listOf(stringResource(R.string.no_sort), stringResource(R.string.tags_ascending), stringResource(R.string.tags_descending)),
                            onChange = { enabled, descending -> options = options.copy(sortTags = enabled, tagsDescending = descending) })
                        SortControl(stringResource(R.string.sort_dates), options.sortDates, options.datesOlderFirst,
                            labels = listOf(stringResource(R.string.no_sort), stringResource(R.string.newer_first), stringResource(R.string.older_first)),
                            onChange = { enabled, olderFirst -> options = options.copy(sortDates = enabled, datesOlderFirst = olderFirst) })
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
    labels: List<String>, onChange: (Boolean, Boolean) -> Unit) {
    val selected = if (!enabled) 0 else if (descending) 2 else 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            labels.forEachIndexed { index, text ->
                SegmentedButton(
                    selected = selected == index,
                    onClick = { onChange(index != 0, if (index == 0) descending else index == 2) },
                    shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp).fillMaxHeight(),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary),
                    icon = {},
                ) {
                    Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
