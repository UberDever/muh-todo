package dev.uberdever.muhtodo.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.uberdever.muhtodo.document.TaskRef
import java.time.LocalDate

sealed interface EditorRequest {
    data object Create : EditorRequest
    data class CreateOnDate(val date: LocalDate) : EditorRequest
    data class CreateAfter(val uri: Uri, val ref: TaskRef) : EditorRequest
    data class Edit(val uri: Uri, val ref: TaskRef) : EditorRequest
}
object EditorIntents {
    const val CREATE = "dev.uberdever.muhtodo.CREATE"
    const val CREATE_AFTER = "dev.uberdever.muhtodo.CREATE_AFTER"
    const val EDIT = "dev.uberdever.muhtodo.EDIT"
    fun create(context: Context, date: LocalDate? = null) = Intent(context, EditorActivity::class.java).setAction(CREATE).apply {
        date?.let { putExtra("create.date", it.toString()) }
    }
    fun readCreation(intent: Intent): EditorRequest? {
        if (intent.action != CREATE) return null
        if (!intent.hasExtra("create.date")) return EditorRequest.Create
        return runCatching {
            val date = LocalDate.parse(intent.getStringExtra("create.date"))
            require(date.year in 2000..2099)
            EditorRequest.CreateOnDate(date)
        }.getOrNull()
    }
    fun createAfter(context: Context, uri: Uri, ref: TaskRef) = reference(Intent(context, EditorActivity::class.java).setAction(CREATE_AFTER), uri, ref)
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
        CREATE -> readCreation(intent)
        EDIT -> readReference(intent)
        CREATE_AFTER -> readReference(intent)?.let { EditorRequest.CreateAfter(it.uri, it.ref) }
        else -> null
    }
}
