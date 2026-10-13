package dev.uberdever.muhtodo.widget

import dev.uberdever.muhtodo.document.*
import org.junit.Assert.*
import org.junit.Test

class WidgetProjectionTest {
    @Test fun everyGroupPermutationControlsOrderBeforeTagsOrDate() {
        val doc = TodoParser.parse("""
            ### 06.10.26
            - (#a) bulletA
            - [x] (#a) doneA
            - [ ] (#z) openZ
            - (#z) bulletZ
            - [ ] (#a) openA
            - [x] (#z) doneZ
        """.trimIndent())
        val groups = EntryGroup.entries
        val names = mapOf(EntryGroup.INCOMPLETE to listOf("openA", "openZ"),
            EntryGroup.COMPLETE to listOf("doneA", "doneZ"), EntryGroup.BULLETS to listOf("bulletA", "bulletZ"))
        for (first in groups) for (second in groups.filter { it != first }) {
            val order = listOf(first, second, groups.single { it != first && it != second })
            assertEquals(order.flatMap { names.getValue(it) },
                tasks(doc, WidgetOptions(sortTags = true, sortDates = true, groupOrder = order)).map { it.body })
        }
    }
    @Test fun creationCounterIgnoresBulletsAndReflectsCompletionChanges() {
        val doc = TodoParser.parse("""
            ### 06.10.26
            - [ ] open
            - [x] done
            - bullet
        """.trimIndent())
        assertEquals(WidgetRow.Create(1, 2), WidgetProjection.project(doc, WidgetOptions()).first())
        val toggled = TodoParser.parse(TodoMutation.toggle(doc, doc.tasks.first()))
        assertEquals(WidgetRow.Create(0, 2), WidgetProjection.project(toggled, WidgetOptions()).first())
    }

    @Test fun defaultGroupingSeparatesPlainBulletsFromCompletedCheckboxes() {
        val doc = TodoParser.parse("""
            ### 06.10.26
            - plain
            - [x] done
            - [ ] open
        """.trimIndent())
        assertEquals(listOf("open", "plain", "done"), tasks(doc, WidgetOptions()).map { it.body })
    }

    @Test fun dateSortIsOptionalWithinCompletionAndTagGroupsAndAgeUsesSectionDate() {
        val source = """
            ### 01.10.26
            - [x] (#b) oldDone
            - [ ] (#a) oldOpen
            ### 06.10.26
            - [ ] (#a) newOpen
            - [ ] (#b) newB
            ### 08.10.26
            - [ ] (#a) futureOpen
        """.trimIndent()
        val doc = TodoParser.parse(source)
        val today = java.time.LocalDate.of(2026, 10, 7)
        val rows = WidgetProjection.project(doc, WidgetOptions(sortTags = true, sortDates = true), today)
        assertTrue(rows.first() is WidgetRow.Create)
        val entries = rows.filterIsInstance<WidgetRow.Task>()
        assertEquals(listOf("futureOpen", "newOpen", "oldOpen", "newB", "oldDone"), entries.map { it.body })
        assertEquals(listOf(-1L, 1L, 6L, 1L, 6L), entries.map { it.ageDays })
        assertEquals(listOf("#a", "^^^", "^^^", "#b", "^^^"), entries.map { it.tagLabel })
        assertEquals(listOf("oldOpen", "newOpen", "futureOpen", "newB", "oldDone"),
            tasks(doc, WidgetOptions(sortTags = true, sortDates = true, datesOlderFirst = true)).map { it.body })
        assertEquals(listOf("oldOpen", "newOpen", "newB", "futureOpen", "oldDone"), tasks(doc, WidgetOptions()).map { it.body })
        assertEquals(source, doc.source)
    }

    @Test fun entriesFromDifferentDatesFormOneTagRunInFileOrder() {
        val doc = TodoParser.parse("""
            ### 01.10.26
            - (#a) older
            ### 06.10.26
            - (#a) newer
        """.trimIndent())
        val rows = WidgetProjection.project(doc, WidgetOptions())
        assertEquals(listOf("older", "newer"), rows.filterIsInstance<WidgetRow.Task>().map { it.body })
        assertEquals(listOf("#a", "^^^"), rows.filterIsInstance<WidgetRow.Task>().map { it.tagLabel })
        assertEquals(3, rows.size) // one scrollable creation row plus two entries
    }

    @Test fun explicitGroupOrderSeparatesBulletsAndKeepsTiesInFileOrder() {
        val doc = TodoParser.parse("""
            ### 02.10.26
            - [ ] (#b) openB
            - (#a) plain
            - [x] (#b) done
            - [ ] (#a) openA
        """.trimIndent())
        assertEquals(listOf("done", "plain", "openB", "openA"),
            tasks(doc, WidgetOptions(groupOrder = listOf(EntryGroup.COMPLETE, EntryGroup.BULLETS, EntryGroup.INCOMPLETE))).map { it.body })
        assertEquals(listOf("openB", "openA", "plain", "done"),
            tasks(doc, WidgetOptions(tagsDescending = true)).map { it.body })
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
        assertEquals(listOf("supplementary", "fullwidth", "older", "longer", "duplicate", "firstA", "secondA", "untagged"), rows.map { it.body })
        assertEquals(listOf("#𐐀", "#Ａ", "#z", "#a #b", "#a #a", "#a", "^^^", null), rows.map { it.tagLabel })
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
            WidgetOptions(sortTags = true) to listOf("openA", "openB", "plainA", "doneA", "doneB"),
            WidgetOptions(sortTags = true, tagsDescending = true) to listOf("openB", "openA", "plainA", "doneB", "doneA"),
            WidgetOptions(sortTags = true, groupOrder = listOf(EntryGroup.COMPLETE, EntryGroup.BULLETS, EntryGroup.INCOMPLETE)) to listOf("doneA", "doneB", "plainA", "openA", "openB"),
            WidgetOptions(sortTags = true, tagsDescending = true, groupOrder = listOf(EntryGroup.COMPLETE, EntryGroup.BULLETS, EntryGroup.INCOMPLETE)) to listOf("doneB", "doneA", "plainA", "openB", "openA"),
        )
        for ((options, expected) in cases) assertEquals(options.toString(), expected, tasks(doc, options).map { it.body })
    }

    @Test fun checkboxFreeEntriesRemainTheirOwnGroupAndKeepPhysicalReferences() {
        val doc = TodoParser.parse("""
            ### 02.10.26
            - (#a) plain
            - [x] (#b) done
            - [ ] (#b) openB
            - [ ] (#a) openA
        """.trimIndent())
        assertEquals(4, doc.tasks.size)
        assertEquals(listOf("openB", "openA", "plain", "done"), tasks(doc, WidgetOptions()).map { it.body })
        assertEquals(listOf("openB", "openA", "plain", "done"), tasks(doc, WidgetOptions()).map { it.body })
        val sorted = tasks(doc, WidgetOptions(sortTags = true))
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

    @Test fun defaultGroupsPreserveFileOrderInsideEachGroup() {
        assertEquals(listOf("openB", "openA", "doneA"), tasks(modeDocument, WidgetOptions()).map { it.body })
    }
    @Test fun tagsSortInsideEachGroup() {
        assertEquals(listOf("openA", "openB", "doneA"), tasks(modeDocument, WidgetOptions(sortTags = true)).map { it.body })
    }
    @Test fun sortCompletionWithoutTagsPreservesOrderWithinCompletionGroups() {
        assertEquals(listOf("openB", "openA", "doneA"), tasks(modeDocument, WidgetOptions()).map { it.body })
    }
    @Test fun completionOutranksTags() {
        assertEquals(listOf("openA", "openB", "doneA"), tasks(modeDocument, WidgetOptions(sortTags = true)).map { it.body })
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
            tasks(doc, WidgetOptions(sortTags = true)).map { it.body })
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
            tasks(doc, WidgetOptions(sortTags = true)).map { it.body })
    }
    @Test fun labelsOnlyResetAtTupleBoundariesWithoutEmptyShorthand() {
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
        assertEquals(listOf("#a", "^^^", null, null, "#a", "#b", "#a", "^^^"),
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
        val rows = tasks(doc, WidgetOptions(sortTags = true))
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
        val rows = tasks(doc, WidgetOptions(sortTags = true))
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
        assertEquals(listOf("old1", "old2", "new"), rows.filterIsInstance<WidgetRow.Task>().map { it.body })
        assertTrue(rows.first() is WidgetRow.Create)
        assertEquals(4, rows.size)
        val first = rows.filterIsInstance<WidgetRow.Task>().first()
        assertFalse(first.completed)
        assertEquals(TaskRef.from(doc, doc.tasks[0]), first.ref)
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
        assertEquals(listOf(WidgetRow.Create(0, 0)), WidgetProjection.project(TodoParser.parse("notes"), WidgetOptions()))
    }
}
