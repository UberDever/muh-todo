package dev.uberdever.muhtodo.widget

import dev.uberdever.muhtodo.document.*
import org.junit.Assert.*
import org.junit.Test

class WidgetProjectionTest {
    @Test fun repeatedDatesCombineWithoutReorderingTheirTasks() {
        val doc = TodoParser.parse("""
            ### 01.10.26
            - [ ] (#a) old1
            ### 02.10.26
            - [x] new
            ### 01.10.26
            - [ ] ^^^ malformed
            - [ ] old2
        """.trimIndent())
        val rows = WidgetProjection.project(doc, WidgetOptions())
        assertEquals(listOf("new", "old1", "old2"), rows.filterIsInstance<WidgetRow.Task>().map { it.body })
        assertEquals(listOf("2026-10-02", "2026-10-01"), rows.filterIsInstance<WidgetRow.DateHeader>().map { it.date.toString() })
        val first = rows.filterIsInstance<WidgetRow.Task>().first()
        assertTrue(first.completed)
        assertEquals(TaskRef.from(doc, doc.tasks[1]), first.ref)
    }
    @Test fun labelsUseEffectiveTagsAndPreserveOrder() {
        val doc = TodoParser.parse("""
            ### 06.10.26
            - [ ] (#b #a #b) first
            unrelated
            - [ ] ^^^ next
        """.trimIndent())
        val rows = WidgetProjection.project(doc, WidgetOptions()).filterIsInstance<WidgetRow.Task>()
        assertEquals(listOf("#b #a #b", "^^^"), rows.map { it.tagLabel })
    }
    @Test fun emptyDocumentHasNoSampleTasks() {
        assertTrue(WidgetProjection.project(TodoParser.parse("notes"), WidgetOptions()).isEmpty())
    }
}
