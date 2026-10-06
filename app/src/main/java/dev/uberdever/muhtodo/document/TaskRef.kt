package dev.uberdever.muhtodo.document

import android.net.Uri
import java.time.LocalDate

data class TaskRef(val lineIndex: Int, val expectedLine: String, val date: LocalDate) {
    companion object {
        fun from(document: ParsedDocument, task: Todo) = TaskRef(task.lineIndex, sourceLines(document.source)[task.lineIndex].content, task.date)
    }
}
data class DocumentSnapshot(val uri: Uri, val document: ParsedDocument)
data class WidgetOptions(
    val sortCompletion: Boolean = false,
    val sortTags: Boolean = false,
    val completionDescending: Boolean = false,
    val tagsDescending: Boolean = false,
)
class DocumentChangedException : Exception("Document changed; reopen task.")
class DocumentAccessException(message: String, cause: Throwable? = null) : Exception(message, cause)
