package dev.uberdever.muhtodo.editor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import dev.uberdever.muhtodo.document.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class EditorStateTest {
    private val today = LocalDate.of(2026, 10, 6)
    private val uri = Uri.parse("content://test/todos")
    private fun snapshot(text: String = "") = DocumentSnapshot(uri, TodoParser.parse(text.trimIndent()))

    @Test fun creationDefaultsToTodayWithoutInheritance() {
        val state = EditorState.create(snapshot(), today)
        assertEquals(today, state.fields.date)
        assertFalse(state.fields.completed)
        assertFalse(state.inheritTags)
        assertFalse(state.canInherit)
    }

    @Test fun dateChangeRecomputesLastSectionPredecessor() {
        val state = EditorState.create(snapshot("""
            ### 06.10.26
            - [ ] (#a) first
            ### 06.10.26
            - [ ] (#b #b) last
            ### 05.10.26
            - [ ] (#old) older
        """), today)
        assertTrue(state.canInherit)
        assertEquals(listOf("#b", "#b"), state.inheritedTags)
        val earlier = state.copy(inheritTags = true).withDate(today.minusDays(1))
        assertTrue(earlier.inheritTags)
        assertEquals(listOf("#old"), earlier.inheritedTags)
        assertFalse(earlier.withDate(today.plusDays(1)).inheritTags)
        assertFalse(earlier.withDate(today.plusDays(1)).canInherit)
    }

    @Test fun inheritedEditUsesEffectiveOrderedTags() {
        val snapshot = snapshot("""
            ### 06.10.26
            - [ ] (#a #b #a) first
            - [x] ^^^ second
        """)
        val state = EditorState.edit(snapshot, TaskRef.from(snapshot.document, snapshot.document.tasks.last()))
        assertEquals(listOf("#a", "#b", "#a"), state.fields.tags)
        assertEquals("second", state.fields.body)
        assertTrue(state.fields.completed)
        assertNull(state.copy(fields = state.fields.copy(body = "edited")).validationError())
    }

    @Test fun invalidAndUnrepresentableDraftsCannotSave() {
        val state = EditorState.create(snapshot(), today)
        assertNotNull(state.validationError())
        assertNotNull(state.copy(fields = state.fields.copy(body = "two\nlines")).validationError())
        assertNotNull(state.copy(fields = state.fields.copy(body = "valid", tags = listOf("#bad tag"))).validationError())
        assertNotNull(state.copy(fields = state.fields.copy(body = "(#work) literal")).validationError())
        assertNull(state.copy(fields = state.fields.copy(body = "(#work) literal", tags = listOf("#valid"))).validationError())
    }

    @Test fun staleTaskCannotOpenEditor() {
        val snapshot = snapshot("""
            ### 06.10.26
            - [ ] current
        """)
        val ref = TaskRef.from(snapshot.document, snapshot.document.tasks.single()).copy(expectedLine = "- [ ] old")
        assertThrows(DocumentChangedException::class.java) { EditorState.edit(snapshot, ref) }
    }

    @Test fun unsavedDraftRestoresAcrossActivityRecreation() {
        val initial = EditorState.create(snapshot(), today)
        val entered = initial.copy(fields = initial.fields.copy(body = "unsaved", completed = true, tags = listOf("#b", "#a", "#b")))
        val bundle = Bundle()
        entered.writeDraft(bundle)
        assertEquals(entered.fields, initial.restoreDraft(bundle).fields)
        assertThrows(DocumentChangedException::class.java) { initial.copy(snapshot = initial.snapshot.copy(uri = Uri.parse("content://test/other"))).restoreDraft(bundle) }
    }

    @Test fun failedSaveKeepsEnteredFieldsAndDoesNotReportSuccess() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = DocumentPreferences(context).apply { setDocumentUri(uri) }
        val store = object : DocumentStore {
            override fun read(uri: Uri) = ""
            override fun write(uri: Uri, source: String) { throw DocumentAccessException("write failed") }
        }
        val repo = DocumentRepository(store, preferences, context.contentResolver)
        val entered = EditorState.create(snapshot(), today).let { it.copy(fields = it.fields.copy(body = "keep me", tags = listOf("#a"))) }
        val result = entered.save(repo)
        assertEquals(entered.fields, result.fields)
        assertEquals("write failed", result.error)
        assertFalse(result.saved)
        assertFalse(result.saving)
    }
}
