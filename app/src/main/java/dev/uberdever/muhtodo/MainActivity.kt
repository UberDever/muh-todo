package dev.uberdever.muhtodo

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.uberdever.muhtodo.editor.EditorIntents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var error by mutableStateOf<String?>(null)
    private var documentName by mutableStateOf<String?>(null)
    private var revision by mutableIntStateOf(0)
    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val uri = data?.data
        if (result.resultCode == RESULT_OK && uri != null) {
            try { AppServices.repository(this).select(uri, data.flags); error = null; revision++ }
            catch (e: Exception) { error = e.message ?: "Could not select document." }
        }
    }
    override fun onResume() { super.onResume(); revision++ }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LaunchedEffect(revision) {
                    try {
                        val snapshot = AppServices.repository(this@MainActivity).read()
                        documentName = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            contentResolver.query(snapshot.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                                if (it.moveToFirst()) it.getString(0) else null
                            } ?: "Markdown document"
                        }
                        error = null
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { documentName = null; error = e.message ?: "Could not read document." }
                }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(24.dp).windowInsetsPadding(WindowInsets.safeDrawing), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Markdown Todo", style = MaterialTheme.typography.headlineMedium)
                        Text(documentName ?: "Choose your Markdown todo file, then add the widget to your home screen.")
                        Button(onClick = {
                            picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION))
                        }) { Text(if (documentName == null) "Select document" else "Change document") }
                        if (documentName != null) {
                            OutlinedButton(onClick = { startActivity(EditorIntents.create(this@MainActivity)) }) { Text("New todo") }
                            OutlinedButton(onClick = {
                                val uri = AppServices.repository(this@MainActivity).preferences.documentUri() ?: return@OutlinedButton
                                try {
                                    startActivity(Intent(Intent.ACTION_EDIT).setDataAndType(uri, "text/plain")
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                        .apply { clipData = ClipData.newRawUri("Markdown document", uri) })
                                } catch (_: android.content.ActivityNotFoundException) { error = "Install a text editor that can edit documents." }
                                  catch (_: SecurityException) { error = "Could not grant editor access; select the file again." }
                            }) { Text("Open in text editor") }
                        }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
