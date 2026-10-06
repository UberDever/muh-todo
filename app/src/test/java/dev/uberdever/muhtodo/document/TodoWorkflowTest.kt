package dev.uberdever.muhtodo.document

import dev.uberdever.muhtodo.widget.WidgetProjection
import dev.uberdever.muhtodo.widget.WidgetRow
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TodoWorkflowTest {
    @Test fun createInheritToggleEditMoveAndSortPreserveDocumentSemantics() {
        val day = LocalDate.of(2026, 10, 2)
        var doc = TodoParser.parse("""
            notes
            ### 02.10.26
            trailing text
        """.trimIndent() + "\n")
        doc = TodoParser.parse(TodoMutation.insert(doc, TaskFields(day, false, listOf("#a"), "first"), false))
        doc = TodoParser.parse(TodoMutation.insert(doc, TaskFields(day, false, emptyList(), "second"), true))
        doc = TodoParser.parse(TodoMutation.insert(doc, TaskFields(day, false, emptyList(), "plain successor", hasCheckbox = false), true))
        assertEquals(listOf(TagForm.EXPLICIT, TagForm.INHERITED, TagForm.INHERITED), doc.tasks.map { it.form })
        doc = TodoParser.parse(TodoMutation.toggle(doc, doc.tasks.first()))
        doc = TodoParser.parse(TodoMutation.edit(doc, doc.tasks.first(), TaskFields(day, true, listOf("#b"), "first")))
        assertTrue(doc.tasks.all { it.tags == listOf("#b") })
        val moved = TodoMutation.edit(doc, doc.tasks.first(), TaskFields(day.plusDays(1), true, listOf("#b"), "first"))
        assertEquals("""
            notes
            ### 02.10.26
            - [ ] (#b) second
            - ^^^ plain successor
            trailing text
            ### 03.10.26
            - [x] (#b) first
        """.trimIndent() + "\n", moved)
        val finalDocument = TodoParser.parse(moved)
        val ascending = WidgetProjection.project(finalDocument, WidgetOptions(true, true)).filterIsInstance<WidgetRow.Task>()
        assertEquals(listOf("first", "second", "plain successor"), ascending.map { it.body })
        val descending = WidgetProjection.project(finalDocument, WidgetOptions(true, true, true, true)).filterIsInstance<WidgetRow.Task>()
        assertEquals(listOf("first", "plain successor", "second"), descending.map { it.body })
        assertFalse(descending.single { it.body == "plain successor" }.hasCheckbox)
        assertTrue(descending.first().completed)
        descending.forEach { row ->
            assertEquals(TaskRef.from(finalDocument, finalDocument.tasks.single { it.body == row.body }), row.ref)
        }
        assertEquals(moved, finalDocument.source)
    }
}
