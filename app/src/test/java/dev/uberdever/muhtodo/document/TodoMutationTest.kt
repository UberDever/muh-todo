package dev.uberdever.muhtodo.document

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TodoMutationTest {
    private val day = LocalDate.of(2026, 10, 2)
    private fun md(text: String) = text.trimIndent() + "\n"
    private fun fields(body: String = "new", tags: List<String> = emptyList(), date: LocalDate = day) = TaskFields(date, false, tags, body)
    private fun Todo.fields() = TaskFields(date, completed, tags, body)

    @Test fun toggleChangesOnlyCheckboxCharacter() {
        val source = "\uFEFF" + md("""
            ### 02.10.26
            comment [ ]
            - [ ] (#a #a) body [ ] **bold**
            unrelated
        """).replace("\n", "\r\n")
        val doc = TodoParser.parse(source)
        assertEquals(source.replace("- [ ]", "- [x]"), TodoMutation.toggle(doc, doc.tasks.single()))
        val done = TodoParser.parse(TodoMutation.toggle(doc, doc.tasks.single()))
        assertEquals(source, TodoMutation.toggle(done, done.tasks.single()))
    }

    @Test fun bodyEditKeepsInheritanceAndUnrelatedText() {
        val source = md("""
            ### 02.10.26
            - [ ] (#a) first
            comment
            - [ ] ^^^ old
        """)
        val doc = TodoParser.parse(source)
        val task = doc.tasks.last()
        assertEquals(source.replace("^^^ old", "^^^ edited"), TodoMutation.edit(doc, task, task.fields().copy(body = "edited")))
    }

    @Test fun tagEditPropagatesThroughFollowingChain() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] (#a) first
            comment
            - [ ] ^^^ second
            - [ ] ^^^ third
            - [ ] (#stop) fourth
        """))
        val result = TodoParser.parse(TodoMutation.edit(doc, doc.tasks.first(), fields("first", listOf("#b", "#b"))))
        assertEquals(List(3) { listOf("#b", "#b") } + listOf(listOf("#stop")), result.tasks.map { it.tags })
        assertEquals(TagForm.INHERITED, result.tasks[1].form)
    }

    @Test fun clearingTagsPropagatesAnEmptyTuple() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] (#a) first
            - [ ] ^^^ second
        """))
        val changed = TodoMutation.edit(doc, doc.tasks.first(), fields("first"))
        assertTrue(changed.contains("- [ ] first\n- [ ] ^^^ second"))
        assertTrue(TodoParser.parse(changed).tasks.all { it.tags.isEmpty() })
    }

    @Test fun movingMaterializesOnlyFirstSourceSuccessor() {
        val source = md("""
            ### 02.10.26
            - [ ] (#a) first
            comment
            - [ ] ^^^ second
            - [ ] ^^^ third
        """)
        val doc = TodoParser.parse(source)
        val changed = TodoMutation.edit(doc, doc.tasks[0], fields("first", listOf("#a"), day.plusDays(1)))
        assertTrue(changed.contains("comment\n- [ ] (#a) second\n- [ ] ^^^ third\n"))
        val result = TodoParser.parse(changed)
        assertEquals(day.plusDays(1), result.tasks.single { it.body == "first" }.date)
        assertTrue(result.tasks.all { it.tags == listOf("#a") })
    }

    @Test fun simultaneousMoveAndTagEditUsesOldTagsForSuccessor() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] (#a) first
            - [ ] ^^^ second
        """))
        val result = TodoParser.parse(TodoMutation.edit(doc, doc.tasks.first(), fields("first", listOf("#b"), day.minusDays(1))))
        assertEquals(listOf("#a"), result.tasks.single { it.body == "second" }.tags)
        assertEquals(listOf("#b"), result.tasks.single { it.body == "first" }.tags)
        assertEquals(2, result.sections.size)
    }

    @Test fun movingEmptyTagsMaterializesAsPlainTask() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] plain
            - [ ] ^^^ inherited
        """))
        val result = TodoMutation.edit(doc, doc.tasks.first(), fields("plain", date = day.plusDays(1)))
        assertTrue(result.contains("### 02.10.26\n- [ ] inherited\n"))
        assertFalse(result.contains("()"))
    }

    @Test fun inheritedMoveMaterializesWhenDestinationTagsDiffer() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] (#a) anchor
            - [ ] ^^^ moved
            ### 01.10.26
            - [ ] (#b) destination
        """))
        val changed = TodoMutation.edit(doc, doc.tasks[1], fields("moved", listOf("#a"), day.minusDays(1)))
        assertTrue(changed.endsWith("- [ ] (#b) destination\n- [ ] (#a) moved\n"))
    }

    @Test fun inheritedMoveKeepsMarkerWhenDestinationMatches() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] (#a) anchor
            - [ ] ^^^ moved
            ### 01.10.26
            - [ ] (#a) destination
        """))
        assertTrue(TodoMutation.edit(doc, doc.tasks[1], fields("moved", listOf("#a"), day.minusDays(1))).endsWith("- [ ] ^^^ moved\n"))
    }

    @Test fun insertionUsesLastMatchingSectionAfterLastRecognizedTask() {
        val source = md("""
            introductory text
            ### 02.10.26
            - [ ] first section
            ### 02.10.26
            comment before
            - [ ] (#a) last task
            trailing text
        """)
        val changed = TodoMutation.insert(TodoParser.parse(source), fields(tags = listOf("#a")), false)
        assertEquals(source.replace("- [ ] (#a) last task\n", "- [ ] (#a) last task\n- [ ] (#a) new\n"), changed)
    }

    @Test fun inheritanceToggleControlsSerializationEvenWhenTagsMatch() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] (#a) preceding
        """))
        assertTrue(TodoMutation.insert(doc, fields(tags = listOf("#a")), false).endsWith("- [ ] (#a) new\n"))
        val inherited = TodoMutation.insert(doc, fields(), true)
        assertTrue(inherited.endsWith("- [ ] ^^^ new\n"))
        assertEquals(listOf("#a"), TodoParser.parse(inherited).tasks.last().tags)
    }

    @Test fun emptyTupleCanBeInherited() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] preceding
        """))
        assertTrue(TodoMutation.insert(doc, fields(), true).endsWith("- [ ] ^^^ new\n"))
    }

    @Test fun sectionWithoutTasksInsertsImmediatelyAfterHeader() {
        val source = md("""
            ### 02.10.26
            unrelated
        """)
        assertEquals(source.replace("### 02.10.26\n", "### 02.10.26\n- [ ] new\n"), TodoMutation.insert(TodoParser.parse(source), fields(), false))
    }

    @Test fun missingSectionAppendsWithoutReorderingExistingText() {
        val source = md("""
            intro
            ### 02.10.26
            - [ ] existing
            tail
        """).removeSuffix("\n")
        val expected = source + "\n" + md("""
            ### 01.10.26
            - [ ] new
        """)
        assertEquals(expected, TodoMutation.insert(TodoParser.parse(source), fields(date = day.minusDays(1)), false))
    }

    @Test fun insertionPreservesBomAndCrLf() {
        val source = "\uFEFF" + md("""
            ### 02.10.26
            text
        """).replace("\n", "\r\n")
        assertEquals(source.replace("### 02.10.26\r\n", "### 02.10.26\r\n- [ ] new\r\n"), TodoMutation.insert(TodoParser.parse(source), fields(), false))
    }

    @Test fun emptyDocumentCreatesSection() {
        assertEquals(md("""
            ### 02.10.26
            - [ ] new
        """), TodoMutation.insert(TodoParser.parse(""), fields(), false))
    }

    @Test fun invalidDraftsAndUnavailableInheritanceAreRejected() {
        val doc = TodoParser.parse("")
        listOf(fields(""), fields(" "), fields("two\nlines"), fields(tags = listOf("#invalid tag")), fields(date = LocalDate.of(2100, 1, 1))).forEach {
            assertThrows(IllegalArgumentException::class.java) { TodoMutation.insert(doc, it, false) }
        }
        assertThrows(IllegalArgumentException::class.java) { TodoMutation.insert(doc, fields(), true) }
    }

    @Test fun ambiguousPlainBodyIsRejectedButTaggedBodyRoundTrips() {
        val doc = TodoParser.parse("")
        assertThrows(IllegalArgumentException::class.java) { TodoMutation.insert(doc, fields("(#work) literal"), false) }
        val changed = TodoMutation.insert(doc, fields("(#work) literal", listOf("#a")), false)
        assertEquals("(#work) literal", TodoParser.parse(changed).tasks.single().body)
    }

    @Test fun unrepresentableEmptyTagSuccessorRejectsMove() {
        val doc = TodoParser.parse(md("""
            ### 02.10.26
            - [ ] first
            - [ ] ^^^ (#a) literal
        """))
        assertThrows(IllegalArgumentException::class.java) { TodoMutation.edit(doc, doc.tasks.first(), fields("first", date = day.plusDays(1))) }
    }
}
