package dev.uberdever.muhtodo.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.uberdever.muhtodo.AppServices
import dev.uberdever.muhtodo.MainActivity
import dev.uberdever.muhtodo.document.WidgetOptions
import kotlinx.coroutines.CancellationException

class WidgetConfigurationActivity : ComponentActivity() {
    private var revision by mutableIntStateOf(0)
    override fun onResume() { super.onResume(); revision++ }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val manager = AppWidgetManager.getInstance(this)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || manager.getAppWidgetInfo(id)?.provider?.className != WidgetProvider::class.java.name) { finish(); return }
        setContent {
            MaterialTheme {
                var ready by remember { mutableStateOf(false) }
                var message by remember { mutableStateOf("Opening document…") }
                LaunchedEffect(revision) {
                    try { AppServices.repository(this@WidgetConfigurationActivity).read(); ready = true; message = "Tasks appear newest date first, in file order." }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { ready = false; message = e.message ?: "Select your Markdown file." }
                }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp).windowInsetsPadding(WindowInsets.safeDrawing), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Markdown Todo widget", style = MaterialTheme.typography.headlineSmall)
                        Text(message)
                        OutlinedButton(onClick = { startActivity(Intent(this@WidgetConfigurationActivity, MainActivity::class.java)) }) { Text("Select or change document") }
                        Button(enabled = ready, onClick = {
                            AppServices.repository(this@WidgetConfigurationActivity).preferences.setWidgetOptions(id, WidgetOptions())
                            WidgetProvider.render(this@WidgetConfigurationActivity, manager, intArrayOf(id))
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)); finish()
                        }) { Text("Add widget") }
                        TextButton(onClick = { finish() }) { Text("Cancel") }
                    }
                }
            }
        }
    }
}
