package dev.uberdever.muhtodo.widget

import dev.uberdever.muhtodo.document.*
import org.junit.Assert.*
import org.junit.Test

class WidgetProjectionTest {
    private val modeDocument = TodoParser.parse("""
        ### 02.10.26
        - [x] (#a) doneA
        - [ ] (#b) openB
        - [ ] (#a) openA
    """.trimIndent())
    private fun tasks(doc: ParsedDocument, options: WidgetOptions) =
        WidgetProjection.project(doc, options).filterIsInstance<WidgetRow.Task>()

    @Test fun asIsWithoutTagsPreservesFileOrder() {
        assertEquals(listOf("doneA", "openB", "openA"), tasks(modeDocument, WidgetOptions()).map { it.body })
    }
    @Test fun asIsWithTagsSortsTuplesBeforeOriginalOrder() {
        assertEquals(listOf("doneA", "openA", "openB"), tasks(modeDocument, WidgetOptions(false, true)).map { it.body })
    }
    @Test fun incompleteFirstWithoutTagsPreservesOrderWithinCompletionGroups() {
        assertEquals(listOf("openB", "openA", "doneA"), tasks(modeDocument, WidgetOptions(true, false)).map { it.body })
    }
    @Test fun completionOutranksTags() {
        assertEquals(listOf("openA", "openB", "doneA"), tasks(modeDocument, WidgetOptions(true, true)).map { it.body })
    }
    @Test fun tupleOrderKeepsEmptyPrefixesDuplicatesAndElementOrderDistinct() {
        val doc = TodoParser.parse("""
            ### 02.10.26
            - [ ] (#b #a) reversed
            - [ ] (#a #b) longer
            - [ ] (#a) shortFirst
            - [ ] untagged
            - [ ] (#a #a) duplicate
            - [ ] (#a) shortSecond
        """.trimIndent())
        assertEquals(listOf("untagged", "shortFirst", "shortSecond", "duplicate", "longer", "reversed"),
            tasks(doc, WidgetOptions(false, true)).map { it.body })
    }
    @Test fun identifiersSortByCaseSensitiveCodePointsRatherThanUtf16OrLocale() {
        val doc = TodoParser.parse("""
            ### 02.10.26
            - [ ] (#𐐀) supplementary
            - [ ] (#Ａ) fullwidth
            - [ ] (#a) lowercase
            - [ ] (#A) uppercase
            - [ ] (#123) numeric
            - [ ] (#a/part) slash
        """.trimIndent())
        assertEquals(6, doc.tasks.size)
        assertEquals(listOf("numeric", "uppercase", "lowercase", "slash", "fullwidth", "supplementary"),
            tasks(doc, WidgetOptions(false, true)).map { it.body })
    }
    @Test fun labelsResetAtTupleAndDateBoundariesWithoutEmptyShorthand() {
        val doc = TodoParser.parse("""
            ### 03.10.26
            - [ ] (#a) first
            - [ ] (#a) second
            - [ ] untagged1
            - [ ] untagged2
            - [ ] (#a) afterEmpty
            - [ ] (#b) different
            - [ ] (#a) afterDifferent
            ### 02.10.26
            - [ ] (#a) older
        """.trimIndent())
        assertEquals(listOf("#a", "^^^", null, null, "#a", "#b", "#a", "#a"),
            tasks(doc, WidgetOptions()).map { it.tagLabel })
    }
    @Test fun sortedRunsUseEffectiveTuplesInsteadOfFileTagRepresentation() {
        val doc = TodoParser.parse("""
            ### 02.10.26
            - [ ] (#b) firstB
            - [ ] ^^^ nextB
            - [ ] (#a) firstA
            - [ ] (#b) lastB
        """.trimIndent())
        val rows = tasks(doc, WidgetOptions(false, true))
        assertEquals(listOf("firstA", "firstB", "nextB", "lastB"), rows.map { it.body })
        assertEquals(listOf("#a", "#b", "^^^", "^^^"), rows.map { it.tagLabel })
    }
    @Test fun sortedReferencesStillAddressOriginalLinesAcrossRepeatedSections() {
        val source = """
            notes
            ### 02.10.26
            - [x] (#a) first
            unrelated
            ### 03.10.26
            - [ ] (#a) newer
            ### 02.10.26
            - [ ] (#b) second
            - [ ] (#a) third
        """.trimIndent()
        val doc = TodoParser.parse(source)
        val rows = tasks(doc, WidgetOptions(true, true))
        assertEquals(listOf("newer", "third", "second", "first"), rows.map { it.body })
        rows.forEach { row ->
            assertEquals(TaskRef.from(doc, doc.tasks.single { it.body == row.body }), row.ref)
        }
        assertEquals(source, doc.source)
    }
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
