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
            tasks.sortedWith { left, right ->
                val completion = if (options.incompleteFirst) left.completed.compareTo(right.completed) else 0
                val tags = if (completion == 0 && options.sortTags) compareTags(left.tags, right.tags) else 0
                when {
                    completion != 0 -> completion
                    tags != 0 -> tags
                    else -> left.lineIndex.compareTo(right.lineIndex)
                }
            }.forEach { task ->
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

    private fun compareTags(left: List<String>, right: List<String>): Int {
        for (index in 0 until minOf(left.size, right.size)) {
            val order = compareCodePoints(left[index], right[index])
            if (order != 0) return order
        }
        return left.size.compareTo(right.size)
    }

    private fun compareCodePoints(left: String, right: String): Int {
        var leftIndex = 0
        var rightIndex = 0
        while (leftIndex < left.length && rightIndex < right.length) {
            val leftPoint = left.codePointAt(leftIndex)
            val rightPoint = right.codePointAt(rightIndex)
            val order = leftPoint.compareTo(rightPoint)
            if (order != 0) return order
            leftIndex += Character.charCount(leftPoint)
            rightIndex += Character.charCount(rightPoint)
        }
        return left.length.compareTo(right.length)
    }
}
