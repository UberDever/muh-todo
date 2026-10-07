package dev.uberdever.muhtodo.widget

import dev.uberdever.muhtodo.document.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface WidgetRow {
    data object Create : WidgetRow
    data class Task(val ref: TaskRef, val completed: Boolean, val body: String, val tagLabel: String?, val hasCheckbox: Boolean = true, val tags: List<String> = emptyList(), val ageDays: Long = 0) : WidgetRow
}
object WidgetProjection {
    fun project(document: ParsedDocument, options: WidgetOptions, today: LocalDate = LocalDate.now()): List<WidgetRow> = buildList {
        add(WidgetRow.Create)
        var previousTags: List<String>? = null
        document.tasks.sortedWith { left, right ->
            val completion = if (options.sortCompletion) {
                val order = (left.completed || !left.hasCheckbox).compareTo(right.completed || !right.hasCheckbox)
                if (options.completionDescending) -order else order
            } else 0
            val tags = if (options.sortTags) {
                if (options.tagsDescending) compareTags(right.tags, left.tags) else compareTags(left.tags, right.tags)
            } else 0
            val date = if (options.sortDates) {
                if (options.datesOlderFirst) left.date.compareTo(right.date) else right.date.compareTo(left.date)
            } else 0
            when {
                completion != 0 -> completion
                tags != 0 -> tags
                date != 0 -> date
                else -> left.lineIndex.compareTo(right.lineIndex)
            }
        }.forEach { task ->
            val label = when {
                task.tags.isEmpty() -> null
                task.tags == previousTags -> "^^^"
                else -> task.tags.joinToString(" ")
            }
            add(WidgetRow.Task(TaskRef.from(document, task), task.completed, task.body, label, task.hasCheckbox, task.tags,
                ChronoUnit.DAYS.between(task.date, today)))
            previousTags = task.tags
        }
    }

    private fun compareTags(left: List<String>, right: List<String>): Int {
        for (index in 0 until minOf(left.size, right.size)) {
            val order = TagSyntax.compareTokens(left[index], right[index])
            if (order != 0) return order
        }
        return left.size.compareTo(right.size)
    }
}
