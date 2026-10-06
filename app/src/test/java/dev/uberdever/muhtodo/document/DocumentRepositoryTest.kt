package dev.uberdever.muhtodo.document

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35], manifest = Config.NONE)
class DocumentRepositoryTest {
    private lateinit var context: Context
    private lateinit var preferences: DocumentPreferences
    private lateinit var repository: DocumentRepository
    private lateinit var store: MemoryStore
    private val uri = Uri.parse("content://test/todos")
    private val source = """
        ### 02.10.26
        - [ ] (#a) first
        tail
    """.trimIndent() + "\n"

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("todo-metadata", Context.MODE_PRIVATE).edit().clear().commit()
        preferences = DocumentPreferences(context)
        preferences.setDocumentUri(uri)
        store = MemoryStore(source)
        repository = DocumentRepository(store, preferences, context.contentResolver)
    }

    private fun ref(): TaskRef {
        val doc = TodoParser.parse(source)
        return TaskRef.from(doc, doc.tasks.first())
    }

    @Test fun readReturnsSelectedDocumentWithoutCopyingTasks() = runBlocking {
        val snapshot = repository.read()
        assertEquals(uri, snapshot.uri)
        assertEquals(source, snapshot.document.source)
        assertEquals(listOf("first"), snapshot.document.tasks.map { it.body })
    }

    @Test fun changedDocumentDoesNotWrite() = runBlocking {
        preferences.setDocumentUri(Uri.parse("content://test/other"))
        assertTrue(runCatching { repository.toggle(uri, ref()) }.exceptionOrNull() is DocumentChangedException)
        assertEquals(0, store.writes.size)
    }

    @Test fun changedTaskLineDoesNotWrite() = runBlocking {
        store.source = source.replace("first", "external edit")
        assertTrue(runCatching { repository.toggle(uri, ref()) }.exceptionOrNull() is DocumentChangedException)
        assertEquals(0, store.writes.size)
    }

    @Test fun missingOrInvalidTaskRefDoesNotWrite() = runBlocking {
        for (reference in listOf(ref().copy(lineIndex = -1), ref().copy(date = LocalDate.of(2026, 10, 3)), ref().copy(lineIndex = 400))) {
            assertTrue(runCatching { repository.toggle(uri, reference) }.exceptionOrNull() is DocumentChangedException)
        }
        assertEquals(0, store.writes.size)
    }

    @Test fun externalUnrelatedEditIsPreservedWhenSaving() = runBlocking {
        store.source = source.replace("tail", "external tail")
        repository.edit(uri, ref(), TaskFields(LocalDate.of(2026, 10, 2), false, listOf("#a"), "edited"))
        assertEquals(source.replace("tail", "external tail").replace("first", "edited"), store.source)
    }

    @Test fun concurrentAppCreatesDoNotLoseTasks() = runBlocking {
        coroutineScope {
            (1..8).map { n -> async(Dispatchers.Default) { repository.create(uri, TaskFields(LocalDate.of(2026, 10, 2), false, emptyList(), "task$n"), false) } }.awaitAll()
        }
        assertEquals((1..8).map { "task$it" }, TodoParser.parse(store.source).tasks.drop(1).map { it.body }.sorted())
        assertEquals(8, store.writes.size)
    }

    @Test fun readFailurePreventsWrites() = runBlocking {
        store.readFailure = DocumentAccessException("read failed")
        assertTrue(runCatching { repository.toggle(uri, ref()) }.exceptionOrNull() is DocumentAccessException)
        assertEquals(0, store.writes.size)
    }

    @Test fun writeFailureIsReported() = runBlocking {
        store.writeFailure = DocumentAccessException("write failed")
        assertTrue(runCatching { repository.toggle(uri, ref()) }.exceptionOrNull() is DocumentAccessException)
        assertEquals(source, store.source)
    }

    @Test fun preferencesSurviveRecreationAndWidgetDeletionIsLocal() {
        preferences.setWidgetOptions(11, WidgetOptions(true, false))
        preferences.setWidgetOptions(22, WidgetOptions(false, true))
        val reloaded = DocumentPreferences(context)
        assertEquals(uri, reloaded.documentUri())
        assertEquals(WidgetOptions(true, false), reloaded.widgetOptions(11))
        assertEquals(WidgetOptions(false, true), reloaded.widgetOptions(22))
        reloaded.removeWidget(11)
        assertEquals(WidgetOptions(), reloaded.widgetOptions(11))
        assertEquals(WidgetOptions(false, true), reloaded.widgetOptions(22))
        assertEquals(uri, reloaded.documentUri())
    }

    @Test fun documentSelectionPersistsActualReadWriteGrants() {
        val selected = Uri.parse("content://test/selected")
        repository.select(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        assertEquals(selected, preferences.documentUri())
        val grant = context.contentResolver.persistedUriPermissions.single { it.uri == selected }
        assertTrue(grant.isReadPermission)
        assertTrue(grant.isWritePermission)
    }

    @Test fun readOnlySelectionIsRejected() {
        assertThrows(DocumentAccessException::class.java) { repository.select(Uri.parse("content://test/read-only"), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        assertEquals(uri, preferences.documentUri())
    }

    private class MemoryStore(@Volatile var source: String) : DocumentStore {
        val writes = mutableListOf<String>()
        var readFailure: Exception? = null
        var writeFailure: Exception? = null
        override fun read(uri: Uri): String { readFailure?.let { throw it }; return source }
        override fun write(uri: Uri, source: String) { writeFailure?.let { throw it }; this.source = source; writes += source }
    }
}
