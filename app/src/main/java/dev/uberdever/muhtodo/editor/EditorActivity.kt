package dev.uberdever.muhtodo.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.uberdever.muhtodo.AppServices
import dev.uberdever.muhtodo.widget.WidgetActions
import dev.uberdever.muhtodo.ui.TodoTheme

class EditorActivity : ComponentActivity() {
    private val editor by viewModels<EditorViewModel> {
        val app = applicationContext
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return EditorViewModel(AppServices.repository(app)) { WidgetActions.refreshAll(app) } as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editor.open(EditorIntents.decode(intent), savedInstanceState)
        setContent {
            TodoTheme {
                val current = editor.state
                LaunchedEffect(current?.saved) { if (current?.saved == true) finish() }
                BackHandler(enabled = current?.saving == true) {}
                Surface {
                    if (current != null) EditorScreen(current, editor::update, editor::save, { finish() })
                    else Column(Modifier.padding(24.dp)) {
                        Text(editor.loadError ?: "Opening document…")
                        TextButton(onClick = { finish() }) { Text("Close") }
                    }
                }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        editor.state?.writeDraft(outState)
        super.onSaveInstanceState(outState)
    }
}
