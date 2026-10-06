package dev.uberdever.muhtodo.widget

import dev.uberdever.muhtodo.document.*
import org.junit.Assert.*
import org.junit.Test

class WidgetProjectionTest {
    @Test fun completionDescendingIncludesPlainEntriesAndKeepsTiesInFileOrder() {
        val doc = TodoParser.parse("""
            ### 02.10.26
            - [ ] (#b) openB
            - (#a) plain
            - [x] (#b) done
            - [ ] (#a) openA
        """.trimIndent())
        assertEquals(listOf("plain", "done", "openB", "openA"),
            tasks(doc, WidgetOptions(sortCompletion = true, completionDescending = true)).map { it.body })
        assertEquals(listOf("openB", "plain", "done", "openA"),
            tasks(doc, WidgetOptions(completionDescending = true, tagsDescending = true)).map { it.body })
    }

    @Test fun tagDescendingReversesTupleComparisonButNeverFileOrderTiesOrDates() {
        val source = """
            ### 01.10.26
            - (#z) older
            ### 02.10.26
            - (#a) firstA
            - (#a #a) duplicate
            - untagged
            - (#a #b) longer
            - (#a) secondA
            - (#𐐀) supplementary
            - (#Ａ) fullwidth
        """.trimIndent()
        val doc = TodoParser.parse(source)
        val rows = tasks(doc, WidgetOptions(sortTags = true, tagsDescending = true))
        assertEquals(listOf("supplementary", "fullwidth", "longer", "duplicate", "firstA", "secondA", "untagged", "older"), rows.map { it.body })
        assertEquals(listOf("#𐐀", "#Ａ", "#a #b", "#a #a", "#a", "^^^", null, "#z"), rows.map { it.tagLabel })
        rows.forEach { assertEquals(TaskRef.from(doc, doc.tasks.single { task -> task.body == it.body }), it.ref) }
        assertEquals(source, doc.source)
    }

    @Test fun bothSortDirectionsAreIndependentAndCompletionAlwaysOutranksTags() {
        val doc = TodoParser.parse("""
            ### 02.10.26
            - [x] (#b) doneB
            - [ ] (#a) openA
            - (#a) plainA
            - [ ] (#b) openB
            - [x] (#a) doneA
        """.trimIndent())
        val cases = listOf(
            WidgetOptions(true, true, false, false) to listOf("openA", "openB", "plainA", "doneA", "doneB"),
            WidgetOptions(true, true, false, true) to listOf("openB", "openA", "doneB", "plainA", "doneA"),
            WidgetOptions(true, true, true, false) to listOf("plainA", "doneA", "doneB", "openA", "openB"),
            WidgetOptions(true, true, true, true) to listOf("doneB", "plainA", "doneA", "openB", "openA"),
        )
        for ((options, expected) in cases) assertEquals(options.toString(), expected, tasks(doc, options).map { it.body })
    }

    @Test fun checkboxFreeEntriesSortAsCompletedAndKeepTheirPhysicalReferences() {
        val doc = TodoParser.parse("""
            ### 02.10.26
            - (#a) plain
            - [x] (#b) done
            - [ ] (#b) openB
            - [ ] (#a) openA
        """.trimIndent())
        assertEquals(4, doc.tasks.size)
        assertEquals(listOf("plain", "done", "openB", "openA"), tasks(doc, WidgetOptions()).map { it.body })
        assertEquals(listOf("openB", "openA", "plain", "done"), tasks(doc, WidgetOptions(true, false)).map { it.body })
        val sorted = tasks(doc, WidgetOptions(true, true))
        assertEquals(listOf("openA", "openB", "plain", "done"), sorted.map { it.body })
        assertEquals(TaskRef.from(doc, doc.tasks.first()), sorted.single { it.body == "plain" }.ref)
        assertFalse(sorted.single { it.body == "plain" }.hasCheckbox)
    }
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
    @Test fun sortCompletionWithoutTagsPreservesOrderWithinCompletionGroups() {
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
