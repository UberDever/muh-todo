package dev.uberdever.muhtodo.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.uberdever.muhtodo.AppServices
import dev.uberdever.muhtodo.document.DocumentChangedException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class EditorActivity : ComponentActivity() {
    private var state by mutableStateOf<EditorState?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                var loadError by remember { mutableStateOf<String?>(null) }
                val repository = remember { AppServices.repository(this) }
                LaunchedEffect(Unit) {
                    try {
                        val snapshot = repository.read()
                        state = when (val request = EditorIntents.decode(intent)) {
                            EditorRequest.Create -> EditorState.create(snapshot, LocalDate.now())
                            is EditorRequest.Edit -> {
                                if (snapshot.uri != request.uri) throw DocumentChangedException()
                                EditorState.edit(snapshot, request.ref)
                            }
                            null -> throw DocumentChangedException()
                        }.let { initial -> savedInstanceState?.let(initial::restoreDraft) ?: initial }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { loadError = e.message ?: "Could not open task." }
                }
                Surface {
                    val current = state
                    BackHandler(enabled = current?.saving == true) {}
                    if (current != null) EditorScreen(current, { state = it }, {
                        state = current.copy(saving = true, error = null)
                        scope.launch {
                            val result = current.save(repository)
                            state = result
                            if (result.saved) {
                                dev.uberdever.muhtodo.widget.WidgetActions.refreshAll(this@EditorActivity)
                                finish()
                            }
                        }
                    }, { finish() })
                    else Column(Modifier.padding(24.dp)) {
                        Text(loadError ?: "Opening document…")
                        TextButton(onClick = { finish() }) { Text("Close") }
                    }
                }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        state?.writeDraft(outState)
        super.onSaveInstanceState(outState)
    }
}
