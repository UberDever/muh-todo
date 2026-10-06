package dev.uberdever.muhtodo.document

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class InsertAfterTest {
    private val day = LocalDate.of(2026, 10, 6)
    @Test fun inheritsSelectedEntryInItsOwnRepeatedSectionAndPreservesSuccessors() {
        val doc = TodoParser.parse("""
            notes
            ### 06.10.26
            - [x] (#b #a #b) anchor
            unrelated markdown
            - ^^^ existing successor
            ### 06.10.26
            - (#different) later section
        """.trimIndent() + "\n")
        val text = TodoMutation.insertAfter(doc, doc.tasks.first(), TaskFields(day, false, doc.tasks.first().tags, "new"))
        assertEquals("""
            notes
            ### 06.10.26
            - [x] (#b #a #b) anchor
            - [ ] ^^^ new
            unrelated markdown
            - ^^^ existing successor
            ### 06.10.26
            - (#different) later section
        """.trimIndent() + "\n", text)
        val parsed = TodoParser.parse(text)
        assertEquals(listOf("#b", "#a", "#b"), parsed.tasks[1].tags)
        assertEquals(doc.tasks[1].tags, parsed.tasks[2].tags)
    }
    @Test fun supportsEmptyInheritedTagsPlainChildrenAndMissingFinalNewline() {
        val source = """
            ### 06.10.26
            - untagged anchor
        """.trimIndent()
        val doc = TodoParser.parse(source)
        val text = TodoMutation.insertAfter(doc, doc.tasks.single(), TaskFields(day, false, emptyList(), "plain child", hasCheckbox = false))
        assertEquals(source + "\n- ^^^ plain child", text)
        val child = TodoParser.parse(text).tasks.last()
        assertEquals(TagForm.INHERITED, child.form)
        assertFalse(child.hasCheckbox)
        assertTrue(child.tags.isEmpty())
    }
    @Test fun preservesBomAndCrLfWhenInsertingBelowAnInheritedAnchor() {
        val source = "\uFEFF" + """
            ### 06.10.26
            - (#a) first
            - ^^^ anchor
            trailing
        """.trimIndent().replace("\n", "\r\n") + "\r\n"
        val doc = TodoParser.parse(source)
        val text = TodoMutation.insertAfter(doc, doc.tasks.last(), TaskFields(day, false, doc.tasks.last().tags, "child"))
        assertEquals(source.replace("- ^^^ anchor\r\n", "- ^^^ anchor\r\n- [ ] ^^^ child\r\n"), text)
    }
    @Test fun editedPrefilledTagsBecomeExplicitAndFollowingInheritanceUsesThem() {
        val source = """
            ### 06.10.26
            - (#a) anchor
            - ^^^ successor
        """.trimIndent()
        val doc = TodoParser.parse(source)
        val text = TodoMutation.insertAfter(doc, doc.tasks.first(), TaskFields(day, false, listOf("#b", "#b"), "edited tags"))
        assertEquals("""
            ### 06.10.26
            - (#a) anchor
            - [ ] (#b #b) edited tags
            - ^^^ successor
        """.trimIndent(), text)
        assertEquals(listOf("#b", "#b"), TodoParser.parse(text).tasks.last().tags)
        val cleared = TodoMutation.insertAfter(doc, doc.tasks.first(), TaskFields(day, false, emptyList(), "cleared tags"))
        assertEquals("""
            ### 06.10.26
            - (#a) anchor
            - [ ] cleared tags
            - ^^^ successor
        """.trimIndent(), cleared)
        assertTrue(TodoParser.parse(cleared).tasks.last().tags.isEmpty())
    }
    @Test fun refusesToInsertBelowAnAnchorInAnotherDate() {
        val doc = TodoParser.parse("""
            ### 06.10.26
            - (#a) anchor
        """.trimIndent())
        assertThrows(IllegalArgumentException::class.java) {
            TodoMutation.insertAfter(doc, doc.tasks.single(), TaskFields(day.plusDays(1), false, emptyList(), "child"))
        }
    }
}
