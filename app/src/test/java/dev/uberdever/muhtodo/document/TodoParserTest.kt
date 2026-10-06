package dev.uberdever.muhtodo.document

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TodoParserTest {
    private fun markdown(source: String) = source.trimIndent() + "\n"

    @Test fun inheritanceSkipsUnrelatedLines() {
        val source = markdown("""
            ### 02.10.26
            - [ ] (#work/project #123 #work/project) first
            comment

            - [x] ^^^ second
            - [ ] ^^^ third
        """)
        val doc = TodoParser.parse(source)
        assertEquals(3, doc.tasks.size)
        assertEquals(listOf("#work/project", "#123", "#work/project"), doc.tasks[2].tags)
        assertEquals(TagForm.INHERITED, doc.tasks[1].form)
        assertTrue(doc.tasks[1].completed)
        assertEquals(source, doc.source)
        assertEquals(listOf("#work/project", "#123"), doc.knownTags)
    }

    @Test fun datesMustBeExactAndValid() {
        val source = markdown("""
            ### 29.02.24
            - [ ] valid
            ### 29.02.23
            - [ ] invalid header stays in previous section
            ### 2.10.26
            ### 02.10.26${" "}
            ### 01.13.26
            ### 31.04.26
            ### 01.01.00
            - [ ] millennium
        """)
        val doc = TodoParser.parse(source)
        assertEquals(listOf(LocalDate.of(2024, 2, 29), LocalDate.of(2000, 1, 1)), doc.sections.map { it.date })
        assertEquals(LocalDate.of(2024, 2, 29), doc.tasks[1].date)
        assertEquals(LocalDate.of(2000, 1, 1), doc.tasks[2].date)
    }

    @Test fun onlyExactCheckboxesInsideSectionsAreRecognized() {
        val source = markdown("""
            - [ ] outside
            ### 02.10.26
            - [ ] open
            - [x] done
              - [ ] indent
            - [X] upper
            - [*] star
            - [ ]
            - [ ]${" "}
            - [maybe] other
        """)
        assertEquals(listOf("open", "done"), TodoParser.parse(source).tasks.map { it.body })
    }

    @Test fun malformedMetadataDoesNotInterruptInheritance() {
        val source = markdown("""
            ### 02.10.26
            - [ ] (#a) start
            - [ ] (#broken tag) ignored
            - [ ] (#a  #b) ignored
            - [ ] (#a)
            - [ ] ^^^
            - [ ] ^^^suffix ignored
            - [ ] ^^^ inherited
        """)
        val tasks = TodoParser.parse(source).tasks
        assertEquals(listOf("start", "inherited"), tasks.map { it.body })
        assertEquals(listOf("#a"), tasks.last().tags)
    }

    @Test fun inheritanceCannotCrossHeadersOrStartSections() {
        val source = markdown("""
            ### 01.10.26
            - [ ] ^^^ orphan
            - [ ] (#a) first
            ### 02.10.26
            - [ ] ^^^ orphan too
            - [ ] plain
            - [ ] ^^^ empty tuple
        """)
        val tasks = TodoParser.parse(source).tasks
        assertEquals(listOf("first", "plain", "empty tuple"), tasks.map { it.body })
        assertEquals(emptyList<String>(), tasks.last().tags)
    }

    @Test fun bodyRemainderIsPreserved() {
        val source = markdown("""
            ### 02.10.26
            - [ ] (remember this) **bold** #tag ^^^
            - [ ] (#a)  (#b) ^^^ body
        """)
        val tasks = TodoParser.parse(source).tasks
        assertEquals("(remember this) **bold** #tag ^^^", tasks[0].body)
        assertEquals(" (#b) ^^^ body", tasks[1].body)
    }

    @Test fun practicalTagGrammarAllowsDigitsUnicodeAndPunctuation() {
        val tags = listOf("#123", "#2026/work", "#покупки", "#\uD801\uDC00", "#v1.2", "#ready?", "#urgent!", "#_+*<>=:-")
        val doc = TodoParser.parse(markdown("""
            ### 02.10.26
            - [ ] (${tags.joinToString(" ")}) test
        """))
        assertEquals(tags, doc.tasks.single().tags)
        tags.forEach { assertTrue(it, TagSyntax.isValidToken(it)) }
        listOf("#", "#has space", "#|quoted|", "#a,b", "#a#b").forEach { assertFalse(it, TagSyntax.isValidToken(it)) }
    }

    @Test fun tupleOrderAndDuplicatesAreMeaningful() {
        val doc = TodoParser.parse(markdown("""
            ### 02.10.26
            - [ ] (#a #b #a) first
            - [ ] (#b #a) second
        """))
        assertEquals(listOf("#a", "#b", "#a"), doc.tasks.first().tags)
        assertNotEquals(doc.tasks.first().tags, doc.tasks.last().tags)
    }

    @Test fun repeatedDatesRemainSeparatePhysicalSections() {
        val doc = TodoParser.parse(markdown("""
            ### 02.10.26
            - [ ] first
            ### 02.10.26
            - [ ] second
        """))
        assertEquals(2, doc.sections.size)
        assertEquals(0, doc.tasks[0].sectionHeaderLine)
        assertEquals(2, doc.tasks[1].sectionHeaderLine)
        assertEquals(2, doc.sections[0].endLineExclusive)
    }

    @Test fun bomAndLineEndingsRemainInOriginalSource() {
        val source = "\uFEFF" + markdown("""
            ### 02.10.26
            - [ ] first
            comment
            - [x] second
        """).removeSuffix("\n").replaceFirst("\n", "\r\n").replace("comment\n", "comment\r\n")
        val doc = TodoParser.parse(source)
        assertEquals(source, doc.source)
        assertEquals(2, doc.tasks.size)
        assertEquals(3, doc.tasks.last().lineIndex)
    }

    @Test fun whitespaceBodiesAreRecognizedButEmptyBodiesAreNot() {
        val tasks = TodoParser.parse(markdown("""
            ### 02.10.26
            - [ ] ${" "}
            - [ ]${" "}
        """)).tasks
        assertEquals(listOf(" "), tasks.map { it.body })
    }
}
