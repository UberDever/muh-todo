package dev.uberdever.muhtodo.editor

import android.os.Bundle
import dev.uberdever.muhtodo.document.*
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

data class EditorState(val snapshot: DocumentSnapshot, val ref: TaskRef?, val fields: TaskFields, val inheritTags: Boolean = false, val canInherit: Boolean = false, val inheritedTags: List<String> = emptyList(), val saving: Boolean = false, val saved: Boolean = false, val error: String? = null, val insertAfter: TaskRef? = null, val deleting: Boolean = false) {
    fun withDate(date: LocalDate): EditorState {
        if (insertAfter != null) return copy(fields = fields.copy(date = insertAfter.date), error = null)
        if (ref != null) return copy(fields = fields.copy(date = date), error = null)
        val previous = predecessor(snapshot, date)
        return copy(fields = fields.copy(date = date), canInherit = previous != null,
            inheritedTags = previous?.tags.orEmpty(), inheritTags = inheritTags && previous != null, error = null)
    }
    fun validationError(): String? = try {
        if (insertAfter != null) TodoMutation.insertAfter(snapshot.document, selected(snapshot, insertAfter), fields)
        else if (ref == null) TodoMutation.insert(snapshot.document, fields, inheritTags)
        else TodoMutation.edit(snapshot.document, selected(snapshot, ref), fields)
        null
    } catch (e: Exception) { e.message ?: "Check the task fields." }
    fun writeDraft(bundle: Bundle) {
        bundle.putString("draft.uri", snapshot.uri.toString())
        bundle.putString("draft.date", fields.date.toString())
        bundle.putString("draft.body", fields.body)
        bundle.putStringArrayList("draft.tags", ArrayList(fields.tags))
        bundle.putBoolean("draft.completed", fields.completed)
        bundle.putBoolean("draft.has-checkbox", fields.hasCheckbox)
        bundle.putBoolean("draft.inherit", inheritTags)
    }
    fun restoreDraft(bundle: Bundle): EditorState {
        if (!bundle.containsKey("draft.uri")) return this
        if (bundle.getString("draft.uri") != snapshot.uri.toString()) throw DocumentChangedException()
        val dated = withDate(LocalDate.parse(bundle.getString("draft.date")))
        return dated.copy(fields = dated.fields.copy(body = bundle.getString("draft.body").orEmpty(),
            tags = bundle.getStringArrayList("draft.tags").orEmpty(), completed = bundle.getBoolean("draft.completed"),
            hasCheckbox = bundle.getBoolean("draft.has-checkbox", fields.hasCheckbox)),
            inheritTags = insertAfter == null && dated.canInherit && bundle.getBoolean("draft.inherit"))
    }
    suspend fun save(repository: DocumentRepository): EditorState = try {
        if (insertAfter != null) repository.createAfter(snapshot.uri, insertAfter, fields)
        else if (ref == null) repository.create(snapshot.uri, fields, inheritTags)
        else repository.edit(snapshot.uri, ref, fields)
        copy(saved = true, saving = false, error = null)
    } catch (e: CancellationException) { throw e }
      catch (e: Exception) { copy(saved = false, saving = false, error = e.message ?: "Could not save task.") }
    suspend fun delete(repository: DocumentRepository): EditorState = try {
        val reference = ref ?: throw IllegalStateException("Only existing entries can be deleted.")
        repository.delete(snapshot.uri, reference)
        copy(saved = true, saving = false, deleting = false, error = null)
    } catch (e: CancellationException) { throw e }
      catch (e: Exception) { copy(saved = false, saving = false, deleting = false, error = e.message ?: "Could not delete task.") }
    companion object {
        fun create(snapshot: DocumentSnapshot, today: LocalDate) = EditorState(snapshot, null,
            TaskFields(today, false, emptyList(), "")).withDate(today)
        fun createAfter(snapshot: DocumentSnapshot, ref: TaskRef): EditorState {
            val task = selected(snapshot, ref)
            return EditorState(snapshot, null, TaskFields(task.date, false, task.tags, ""),
                inheritedTags = task.tags, insertAfter = ref)
        }
        fun edit(snapshot: DocumentSnapshot, ref: TaskRef): EditorState {
            val task = selected(snapshot, ref)
            return EditorState(snapshot, ref, TaskFields(task.date, task.completed, task.tags, task.body, task.hasCheckbox))
        }
        private fun predecessor(snapshot: DocumentSnapshot, date: LocalDate): Todo? {
            val section = snapshot.document.sections.lastOrNull { it.date == date } ?: return null
            return snapshot.document.tasks.lastOrNull { it.sectionHeaderLine == section.headerLineIndex }
        }
        private fun selected(snapshot: DocumentSnapshot, ref: TaskRef): Todo {
            val task = snapshot.document.tasks.firstOrNull { it.lineIndex == ref.lineIndex && it.date == ref.date }
                ?: throw DocumentChangedException()
            if (TaskRef.from(snapshot.document, task) != ref) throw DocumentChangedException()
            return task
        }
    }
}
