package dev.uberdever.muhtodo.document

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DocumentRepository(private val store: DocumentStore, val preferences: DocumentPreferences, private val resolver: ContentResolver) {
    private val mutex = Mutex()

    fun select(uri: Uri, grantFlags: Int) {
        val required = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val flags = grantFlags and required
        if (uri.scheme != "content" || flags != required) throw DocumentAccessException("Select a document with read and write access.")
        try {
            resolver.takePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
            throw DocumentAccessException("Could not retain document permission; select the file again.", e)
        }
        preferences.setDocumentUri(uri)
    }

    suspend fun read(): DocumentSnapshot = withContext(Dispatchers.IO) {
        mutex.withLock {
            val uri = preferences.documentUri() ?: throw DocumentAccessException("Select a Markdown document.")
            DocumentSnapshot(uri, TodoParser.parse(store.read(uri)))
        }
    }

    suspend fun create(expectedUri: Uri, fields: TaskFields, inheritTags: Boolean) = mutate(expectedUri, null) { doc, _ -> TodoMutation.insert(doc, fields, inheritTags) }
    suspend fun createAfter(expectedUri: Uri, ref: TaskRef, fields: TaskFields) = mutate(expectedUri, ref) { doc, task -> TodoMutation.insertAfter(doc, task!!, fields) }
    suspend fun edit(expectedUri: Uri, ref: TaskRef, fields: TaskFields) = mutate(expectedUri, ref) { doc, task -> TodoMutation.edit(doc, task!!, fields) }
    suspend fun delete(expectedUri: Uri, ref: TaskRef) = mutate(expectedUri, ref) { doc, task -> TodoMutation.delete(doc, task!!) }
    suspend fun toggle(expectedUri: Uri, ref: TaskRef) = mutate(expectedUri, ref) { doc, task -> TodoMutation.toggle(doc, task!!) }

    private suspend fun mutate(expectedUri: Uri, ref: TaskRef?, operation: (ParsedDocument, Todo?) -> String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (preferences.documentUri() != expectedUri) throw DocumentChangedException()
            val doc = TodoParser.parse(store.read(expectedUri))
            val task = ref?.let { reference ->
                val current = doc.tasks.firstOrNull { it.lineIndex == reference.lineIndex && it.date == reference.date }
                if (current == null || sourceLines(doc.source).getOrNull(reference.lineIndex)?.content != reference.expectedLine) throw DocumentChangedException()
                current
            }
            val changed = operation(doc, task)
            if (preferences.documentUri() != expectedUri) throw DocumentChangedException()
            store.write(expectedUri, changed)
        }
    }
}
