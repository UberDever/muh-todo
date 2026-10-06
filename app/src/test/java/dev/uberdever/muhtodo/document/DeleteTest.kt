package dev.uberdever.muhtodo.document

import org.junit.Assert.*
import org.junit.Test

class DeleteTest {
    @Test fun deletingMaterializesTheFirstInheritedSuccessorAcrossUnrelatedLines() {
        val source = """
            notes
            ### 06.10.26
            - [ ] (#old) preceding
            - (#a #a #b) delete me
            <!-- unrelated -->

            - [x] ^^^ keep me
            - ^^^ chain
            ### 06.10.26
            - [ ] (#other) repeated section
        """.trimIndent()
        val doc = TodoParser.parse(source)
        val result = TodoMutation.delete(doc, doc.tasks[1])
        assertEquals(source.replace("- (#a #a #b) delete me\n", "").replace("- [x] ^^^ keep me", "- [x] (#a #a #b) keep me"), result)
        assertEquals(listOf(listOf("#old"), listOf("#a", "#a", "#b"), listOf("#a", "#a", "#b"), listOf("#other")), TodoParser.parse(result).tasks.map { it.tags })
    }
    @Test fun deletingEmptyTagAnchorKeepsFollowingPlainEntryAndCrLfBom() {
        val source = "\uFEFF" + """
            ### 06.10.26
            - [ ] delete
            unrelated
            - ^^^ stays
            - [ ] ^^^ chain
        """.trimIndent().replace("\n", "\r\n")
        val doc = TodoParser.parse(source)
        val result = TodoMutation.delete(doc, doc.tasks.first())
        assertEquals(source.replace("- [ ] delete\r\n", "").replace("- ^^^ stays", "- stays"), result)
        assertTrue(TodoParser.parse(result).tasks.all { it.tags.isEmpty() })
    }
    @Test fun deletingFinalEntryPreservesItsDateAndDoesNotCrossIntoTheNextSection() {
        val source = """
            ### 06.10.26
            - [ ] (#a) last
            ### 07.10.26
            - ^^^ unrelated orphan
        """.trimIndent()
        val doc = TodoParser.parse(source)
        assertEquals(source.replace("- [ ] (#a) last\n", ""), TodoMutation.delete(doc, doc.tasks.single()))
        val eof = TodoParser.parse("### 06.10.26\n- last")
        assertEquals("### 06.10.26\n", TodoMutation.delete(eof, eof.tasks.single()))
    }
    @Test fun unrepresentableEmptyTagSuccessorFailsInsteadOfCorruptingTheDocument() {
        val source = """
            ### 06.10.26
            - [ ] delete
            - ^^^ (#looks-like-tags) actual body
        """.trimIndent()
        val doc = TodoParser.parse(source)
        val failure = runCatching { TodoMutation.delete(doc, doc.tasks.first()) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertEquals(source, doc.source)
        assertEquals(emptyList<String>(), doc.tasks.last().tags)
        assertEquals("(#looks-like-tags) actual body", doc.tasks.last().body)
    }

}
