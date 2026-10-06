package dev.uberdever.muhtodo.document

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CheckboxFreeEntryTest {
    private val day = LocalDate.of(2026, 10, 2)
    private fun md(text: String) = text.trimIndent() + "\n"
    private fun Todo.fields() = TaskFields(date, completed, tags, body, hasCheckbox)

    @Test fun plainAndCheckboxEntriesShareTagInheritanceAndDateSections() {
        val source = md("""
            - outside
            ### 02.10.26
            - (#work #work) plain
            unrelated
            - ^^^ plainInherited
            - [ ] ^^^ unchecked
            - [x] ^^^ checked
            ### 03.10.26
            - ^^^ orphan
            - no tags
            - ^^^ empty tuple
        """)
        val doc = TodoParser.parse(source)
        assertEquals(6, doc.tasks.size)
        assertEquals(listOf(false, false, true, true, false, false), doc.tasks.map { it.hasCheckbox })
        assertEquals(listOf("#work", "#work"), doc.tasks[2].tags)
        assertTrue(doc.tasks[3].completed)
        assertFalse(doc.tasks.first().completed)
        assertEquals(emptyList<String>(), doc.tasks.last().tags)
        assertEquals(listOf("#work"), doc.knownTags)
        assertEquals(source, doc.source)
    }
    @Test fun plainEntriesKeepStrictListSyntaxAndMalformedMetadataIsUnrelated() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
              - indented
            * different bullet
            -${" "}
            - [X] wrong checkbox
            - [maybe] wrong checkbox
            - (#broken tag) ignored
            - ^^^ orphan
            - [docs](https://example.com) **body** #literal ^^^
        """))
        assertEquals(listOf("[docs](https://example.com) **body** #literal ^^^"), doc.tasks.map { it.body })
        assertFalse(doc.tasks.single().hasCheckbox)
    }
    @Test fun creationCanOmitCheckboxWithExplicitOrInheritedTags() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] (#a) first
            trailing text
        """))
        val fields = TaskFields(day, false, listOf("#b", "#b"), "plain", hasCheckbox = false)
        assertEquals(doc.source.replace("trailing text", "- (#b #b) plain\ntrailing text"), TodoMutation.insert(doc, fields, false))
        assertEquals(doc.source.replace("trailing text", "- ^^^ plain\ntrailing text"), TodoMutation.insert(doc, fields, true))
    }
    @Test fun plainEditsPreserveRepresentationAndLineEndings() {
        val source = "\uFEFF" + md("""
            ### 02.10.26
            - (#a) first
            comment
            - ^^^ second
            - [ ] ^^^ third
        """).replace("\n", "\r\n")
        val doc = TodoParser.parse(source)
        assertEquals(3, doc.tasks.size)
        val second = doc.tasks[1]
        assertEquals(source.replace("^^^ second", "^^^ edited"),
            TodoMutation.edit(doc, second, second.fields().copy(body = "edited")))
        val changed = TodoParser.parse(TodoMutation.edit(doc, doc.tasks.first(), doc.tasks.first().fields().copy(tags = listOf("#b"))))
        assertEquals(List(3) { listOf("#b") }, changed.tasks.map { it.tags })
        assertEquals(listOf(false, false, true), changed.tasks.map { it.hasCheckbox })
    }
    @Test fun movingPreservesPlainSuccessorAndMovedEntriesWithoutAddingCheckboxes() {
        val source = md("""
            ### 02.10.26
            - [ ] (#a) first
            comment
            - ^^^ second
            - ^^^ third
        """)
        val doc = TodoParser.parse(source)
        val moved = TodoMutation.edit(doc, doc.tasks.first(), doc.tasks.first().fields().copy(date = day.plusDays(1)))
        assertTrue(moved.contains("comment\n- (#a) second\n- ^^^ third\n"))
        val plainDoc = TodoParser.parse(moved)
        val plain = plainDoc.tasks.single { it.body == "second" }
        val movedAgain = TodoMutation.edit(plainDoc, plain, plain.fields().copy(date = day.plusDays(2)))
        val result = TodoParser.parse(movedAgain)
        assertFalse(result.tasks.single { it.body == "second" }.hasCheckbox)
        assertFalse(result.tasks.single { it.body == "third" }.hasCheckbox)
        assertEquals(listOf("#a"), result.tasks.single { it.body == "third" }.tags)
    }
    @Test fun editorCanAddAndRemoveACheckboxExplicitly() {
        val source = md("""
            ### 02.10.26
            - [x] (#a) original
        """)
        val doc = TodoParser.parse(source)
        val plain = TodoMutation.edit(doc, doc.tasks.single(), TaskFields(day, false, listOf("#a"), "original", false))
        assertEquals(source.replace("- [x] ", "- "), plain)
        val plainDoc = TodoParser.parse(plain)
        assertFalse(plainDoc.tasks.single().hasCheckbox)
        assertEquals(source.replace("[x]", "[ ]"), TodoMutation.edit(plainDoc, plainDoc.tasks.single(), TaskFields(day, false, listOf("#a"), "original")))
    }
    @Test fun plainEntriesRejectCompletionTogglesAndContradictoryFields() {
        assertThrows(IllegalArgumentException::class.java) {
            TodoMutation.insert(TodoParser.parse(""), TaskFields(day, true, emptyList(), "plain", false), false)
        }
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - plain
        """))
        assertEquals(1, doc.tasks.size)
        assertThrows(IllegalArgumentException::class.java) { TodoMutation.toggle(doc, doc.tasks.single()) }
    }
    @Test fun checkboxFreeTaskCannotBeToggledEvenThroughADirectMutationCall() {
        val source = md("""
            ### 02.10.26
            - plain
        """)
        val task = Todo(1, 0, day, false, "plain", emptyList(), TagForm.NONE, false)
        val document = ParsedDocument(source, listOf(DateSection(day, 0, 2)), listOf(task), emptyList())
        assertThrows(IllegalArgumentException::class.java) { TodoMutation.toggle(document, task) }
        assertEquals(source, document.source)
    }
    @Test fun plainBodiesCannotSilentlyTurnIntoCheckboxMetadata() {
        for (body in listOf("[ ] literal", "[x] literal", "[X] literal")) {
            assertThrows(IllegalArgumentException::class.java) {
                TodoMutation.insert(TodoParser.parse(""), TaskFields(day, false, emptyList(), body, false), false)
            }
        }
    }
}
