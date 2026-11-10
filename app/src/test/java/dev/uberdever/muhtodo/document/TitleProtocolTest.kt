package dev.uberdever.muhtodo.document

import dev.uberdever.muhtodo.widget.WidgetProjection
import dev.uberdever.muhtodo.widget.WidgetRow
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TitleProtocolTest {
    @Test fun titleIsSeparatedFromBodyAfterCheckboxAndTags() {
        val document = TodoParser.parse("""
            ### 10.11.26
            - [ ] (#50 #work) Stuff. Make stuff good
            - [x] ^^^ Archive. Put the old notes away
            - (#ideas) Garden. Consider planting mint
        """.trimIndent())
        assertEquals(listOf("Make stuff good", "Put the old notes away", "Consider planting mint"),
            document.tasks.map { it.body })
        assertEquals(listOf("Stuff", "Archive", "Garden"), document.tasks.map { it.title })
        assertEquals(listOf("#50", "#work"), document.tasks[1].tags)
        assertTrue(document.tasks[1].completed)
        assertFalse(document.tasks[2].hasCheckbox)
    }

    @Test fun titleSyntaxIsOnlyAtStartAndRequiresOneWordPeriodSpaceAndBody() {
        val bodies = listOf("Buy milk. Then bread", "https://example.com. Read this", "v2.0. Read this",
            "Title.No space", "Title.", "Title. ", " Title. Leading space", "Two words. Body",
            "Two\u00a0words. Body", "Two\u2003words. Body")
        val source = "### 10.11.26\n" + bodies.joinToString("\n") { "- [ ] $it" }
        val tasks = TodoParser.parse(source).tasks
        assertEquals(bodies, tasks.map { it.body })
        assertTrue(tasks.all { it.title == null })
    }

    @Test fun unicodeTitleAndBodyWhitespaceRoundTripWithoutNormalization() {
        val source = "\uFEFF" + """
            ### 10.11.26
            - [ ] (#дом) Покупки.  Купить чай. Потом хлеб  
            - 🌱. Plant mint
        """.trimIndent().replace("\n", "\r\n")
        val doc = TodoParser.parse(source)
        assertEquals(listOf("Покупки", "🌱"), doc.tasks.map { it.title })
        for (task in doc.tasks) assertEquals(source, TodoMutation.edit(doc, task, task.fields()))
        assertEquals(source.replace("- [ ]", "- [x]"), TodoMutation.toggle(doc, doc.tasks.first()))
    }

    @Test fun creatingEditingAndClearingTitleKeepBodyAndMetadata() {
        val fields = TaskFields(day, false, listOf("#35", "#work"), "Make stuff good", title = "Stuff")
        val created = TodoMutation.insert(TodoParser.parse(""), fields, false)
        assertTrue(created.endsWith("- [ ] (#35 #work) Stuff. Make stuff good\n"))
        val doc = TodoParser.parse(created)
        val edited = TodoParser.parse(TodoMutation.edit(doc, doc.tasks.single(), fields.copy(title = "Project")))
        assertEquals("Project", edited.tasks.single().title)
        assertEquals("Make stuff good", edited.tasks.single().body)
        val cleared = TodoParser.parse(TodoMutation.edit(edited, edited.tasks.single(), fields.copy(title = null)))
        assertNull(cleared.tasks.single().title)
        assertEquals(fields.body, cleared.tasks.single().body)
    }

    @Test fun deletingOrMovingParentPreservesMaterializedSuccessorTitle() {
        val source = """
            ### 10.11.26
            - [ ] (#50 #work) Parent. Move me
            <!-- preserve me -->
            - ^^^ Child. Keep these tags and title
            - [x] ^^^ Last. Keep the inheritance chain
        """.trimIndent()
        val doc = TodoParser.parse(source)
        val moved = TodoMutation.edit(doc, doc.tasks.first(), doc.tasks.first().fields().copy(date = day.plusDays(1)))
        val deleted = TodoMutation.delete(doc, doc.tasks.first())
        for (result in listOf(moved, deleted)) {
            assertTrue(result.contains("<!-- preserve me -->\n- (#50 #work) Child. Keep these tags and title"))
            assertTrue(result.contains("- [x] ^^^ Last. Keep the inheritance chain"))
            val successor = TodoParser.parse(result).tasks.first()
            assertEquals("Child", successor.title)
            assertEquals("Keep these tags and title", successor.body)
        }
        assertEquals("Parent", TodoParser.parse(moved).tasks.last().title)
    }

    @Test fun titlesDoNotInheritButTagsStillDo() {
        val doc = TodoParser.parse("""
            ### 10.11.26
            - [ ] (#50 #work) Parent. First body
        """.trimIndent())
        val fields = TaskFields(day, false, listOf("#50", "#work"), "Second body", title = "Child")
        val created = TodoMutation.insertAfter(doc, doc.tasks.single(), fields)
        assertTrue(created.endsWith("- [ ] ^^^ Child. Second body"))
        val inherited = TodoParser.parse(created).tasks.last()
        assertEquals("Child", inherited.title)
        assertEquals(TagForm.INHERITED, inherited.form)
        val noTitle = TodoParser.parse(TodoMutation.insertAfter(doc, doc.tasks.single(), fields.copy(title = null)))
        assertNull(noTitle.tasks.last().title)
    }

    @Test fun titleCanProtectLeadingMetadataInBody() {
        val fields = TaskFields(day, false, emptyList(), "^^^ is literal body text", title = "Notes")
        val task = TodoParser.parse(TodoMutation.insert(TodoParser.parse(""), fields, false)).tasks.single()
        assertEquals("Notes", task.title)
        assertEquals(fields.body, task.body)
    }

    @Test fun invalidTitlesAreRejectedWithoutWriting() {
        val doc = TodoParser.parse("")
        for (title in listOf("", "two words", "two\u00a0words", "a.b", "line\nbreak", "control\u0000")) {
            val fields = TaskFields(day, false, emptyList(), "body", title = title)
            assertThrows(title, IllegalArgumentException::class.java) { TodoMutation.insert(doc, fields, false) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            TodoMutation.insert(doc, TaskFields(day, false, emptyList(), "", title = "Title"), false)
        }
    }

    @Test fun bodyTypedWithTitleSyntaxRemainsTextuallyLossless() {
        val fields = TaskFields(day, false, emptyList(), "Milk. Buy milk")
        val result = TodoMutation.insert(TodoParser.parse(""), fields, false)
        assertTrue(result.endsWith("- [ ] Milk. Buy milk\n"))
        assertEquals("Milk", TodoParser.parse(result).tasks.single().title)
    }

    @Test fun widgetShowsBothTitleAndBodyWithoutChangingSortOrder() {
        val doc = TodoParser.parse("""
            ### 10.11.26
            - [ ] (#work) Zebra. First in file
            - [ ] ^^^ Apple. Second in file
            - Plain. A plain entry
        """.trimIndent())
        val rows = WidgetProjection.project(doc, WidgetOptions(sortTags = true), day).filterIsInstance<WidgetRow.Task>()
        assertEquals(listOf("Zebra. First in file", "Apple. Second in file", "Plain. A plain entry"), rows.map { it.body })
        assertEquals("^^^", rows[1].tagLabel)
    }

    private val day = LocalDate.of(2026, 11, 10)
    private fun Todo.fields() = TaskFields(date, completed, tags, body, hasCheckbox, title)
}
