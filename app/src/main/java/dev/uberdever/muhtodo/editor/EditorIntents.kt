package dev.uberdever.muhtodo.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.uberdever.muhtodo.document.TaskRef
import java.time.LocalDate

sealed interface EditorRequest {
    data object Create : EditorRequest
    data class Edit(val uri: Uri, val ref: TaskRef) : EditorRequest
}
object EditorIntents {
    const val CREATE = "dev.uberdever.muhtodo.CREATE"
    const val EDIT = "dev.uberdever.muhtodo.EDIT"
    fun create(context: Context) = Intent(context, EditorActivity::class.java).setAction(CREATE)
    fun edit(context: Context, uri: Uri, ref: TaskRef) = reference(Intent(context, EditorActivity::class.java).setAction(EDIT), uri, ref)
    fun reference(intent: Intent, uri: Uri, ref: TaskRef): Intent = intent.setData(uri)
        .putExtra("task.line", ref.lineIndex).putExtra("task.original", ref.expectedLine).putExtra("task.date", ref.date.toString())
    fun readReference(intent: Intent): EditorRequest.Edit? { return try {
        val uri = intent.data ?: return null
        val line = intent.getIntExtra("task.line", -1)
        val original = intent.getStringExtra("task.original") ?: return null
        val date = LocalDate.parse(intent.getStringExtra("task.date") ?: return null)
        if (uri.scheme != "content" || line < 0 || date.year !in 2000..2099) null
        else EditorRequest.Edit(uri, TaskRef(line, original, date))
    } catch (_: Exception) { null } }
    fun decode(intent: Intent): EditorRequest? = when (intent.action) {
        CREATE -> EditorRequest.Create
        EDIT -> readReference(intent)
        else -> null
    }
}
