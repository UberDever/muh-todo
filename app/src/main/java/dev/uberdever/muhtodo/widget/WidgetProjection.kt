package dev.uberdever.muhtodo.widget

import dev.uberdever.muhtodo.document.*
import java.time.LocalDate

sealed interface WidgetRow {
    data class DateHeader(val date: LocalDate) : WidgetRow
    data class Task(val ref: TaskRef, val completed: Boolean, val body: String, val tagLabel: String?) : WidgetRow
}
object WidgetProjection {
    fun project(document: ParsedDocument, options: WidgetOptions): List<WidgetRow> = buildList {
        document.tasks.groupBy { it.date }.toSortedMap(compareByDescending { it }).forEach { (date, tasks) ->
            add(WidgetRow.DateHeader(date))
            var previousTags: List<String>? = null
            tasks.forEach { task ->
                val label = when {
                    task.tags.isEmpty() -> null
                    task.tags == previousTags -> "^^^"
                    else -> task.tags.joinToString(" ")
                }
                add(WidgetRow.Task(TaskRef.from(document, task), task.completed, task.body, label))
                previousTags = task.tags
            }
        }
    }
}
