package dev.uberdever.muhtodo.editor

import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.uberdever.muhtodo.document.*
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Owns the draft and save across configuration changes; never retains an Activity. */
class EditorViewModel(private val repository: DocumentRepository, private val refresh: () -> Unit) : ViewModel() {
    var state by mutableStateOf<EditorState?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    private var opened = false

    fun open(request: EditorRequest?, saved: Bundle?) {
        if (opened || state != null) return
        opened = true
        viewModelScope.launch {
            try {
                val snapshot = repository.read()
                val expectedUri = when (request) {
                    is EditorRequest.Edit -> request.uri
                    is EditorRequest.CreateAfter -> request.uri
                    else -> null
                }
                if (expectedUri != null && snapshot.uri != expectedUri) throw DocumentChangedException()
                state = if (saved?.containsKey("draft.uri") == true) restore(snapshot, request, saved)
                else when (request) {
                    EditorRequest.Create -> EditorState.create(snapshot, LocalDate.now())
                    is EditorRequest.CreateOnDate -> EditorState.create(snapshot, request.date)
                    is EditorRequest.Edit -> EditorState.edit(snapshot, request.ref)
                    is EditorRequest.CreateAfter -> EditorState.createAfter(snapshot, request.ref)
                    null -> throw DocumentChangedException()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // Entered fields survive even when permission or the original source is stale.
                val uri = saved?.getString("draft.uri")?.let(Uri::parse)
                state = if (uri?.scheme == "content" && request != null) runCatching {
                    restore(DocumentSnapshot(uri, TodoParser.parse("")), request, saved!!)
                        .copy(error = e.message ?: "Could not open document.")
                }.getOrNull() else null
                loadError = e.message ?: "Could not open task."
            }
        }
    }
    private fun restore(snapshot: DocumentSnapshot, request: EditorRequest?, bundle: Bundle): EditorState {
        val base = when (request) {
            EditorRequest.Create -> EditorState.create(snapshot, LocalDate.now())
            is EditorRequest.CreateOnDate -> EditorState.create(snapshot, request.date)
            is EditorRequest.CreateAfter -> {
                if (snapshot.uri != request.uri) throw DocumentChangedException()
                val tags = bundle.getStringArrayList("draft.tags").orEmpty()
                EditorState(snapshot, null, TaskFields(request.ref.date, false, tags, ""),
                    inheritedTags = tags, insertAfter = request.ref)
            }
            is EditorRequest.Edit -> {
                if (snapshot.uri != request.uri) throw DocumentChangedException()
                EditorState(snapshot, request.ref, TaskFields(request.ref.date, false, emptyList(), ""))
            }
            null -> throw DocumentChangedException()
        }
        return base.restoreDraft(bundle)
    }
    fun update(value: EditorState) { if (state?.saving != true) state = value }
    fun delete() {
        val current = state ?: return
        if (current.ref == null || current.saving || current.saved) return
        state = current.copy(saving = true, deleting = true, error = null)
        viewModelScope.launch {
            val result = current.delete(repository)
            state = result
            if (result.saved) refresh()
        }
    }
    fun save() {
        val current = state ?: return
        if (current.saving || current.saved || current.validationError() != null) return
        state = current.copy(saving = true, error = null)
        viewModelScope.launch {
            val result = current.save(repository)
            state = result
            if (result.saved) refresh()
        }
    }
}
