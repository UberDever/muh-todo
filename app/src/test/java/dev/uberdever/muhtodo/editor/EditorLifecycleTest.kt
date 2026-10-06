package dev.uberdever.muhtodo.editor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import dev.uberdever.muhtodo.document.*
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class EditorLifecycleTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uri = Uri.parse("content://test/todos")
    private val day = LocalDate.of(2026, 10, 6)
    private val original = """
        ### 06.10.26
        - [ ] original
    """.trimIndent()
    private fun repo(store: DocumentStore) = DocumentRepository(store,
        DocumentPreferences(context).apply { setDocumentUri(uri) }, context.contentResolver)
    private fun await(condition: () -> Boolean) {
        val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!condition() && System.nanoTime() < until) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(5) }
        assertTrue("Asynchronous operation did not finish", condition())
    }
    @Test fun arrowCreationSavesBelowItsAnchorAndRefreshesOnce() {
        val source = """
            ### 06.10.26
            - [x] (#a) anchor
            - (#b) last
        """.trimIndent()
        val snapshot = DocumentSnapshot(uri, TodoParser.parse(source))
        val ref = TaskRef.from(snapshot.document, snapshot.document.tasks.first())
        var text = source
        var writes = 0
        var refreshes = 0
        val store = object : DocumentStore {
            override fun read(uri: Uri) = text
            override fun write(uri: Uri, source: String) { text = source; writes++ }
        }
        val owner = EditorViewModel(repo(store)) { refreshes++ }
        owner.open(EditorRequest.CreateAfter(uri, ref), null)
        await { owner.state != null || owner.loadError != null }
        val state = owner.state!!
        assertEquals(listOf("#a"), state.inheritedTags)
        owner.update(state.copy(fields = state.fields.copy(body = "child", hasCheckbox = false)))
        owner.save()
        await { owner.state?.saved == true }
        assertEquals("""
            ### 06.10.26
            - [x] (#a) anchor
            - ^^^ child
            - (#b) last
        """.trimIndent(), text)
        assertEquals(1, writes)
        assertEquals(1, refreshes)
    }
    @Test fun staleInheritedAnchorRestoresBodyButCannotSaveAfterProcessRecreation() {
        val snapshot = DocumentSnapshot(uri, TodoParser.parse(original))
        val ref = TaskRef.from(snapshot.document, snapshot.document.tasks.single())
        val draft = EditorState.createAfter(snapshot, ref).let { it.copy(fields = it.fields.copy(body = "keep me")) }
        val saved = Bundle().also(draft::writeDraft)
        val store = object : DocumentStore {
            override fun read(uri: Uri) = original.replace("original", "changed externally")
            override fun write(uri: Uri, source: String) { fail("A stale anchor cannot write") }
        }
        val owner = EditorViewModel(repo(store)) {}
        owner.open(EditorRequest.CreateAfter(uri, ref), saved)
        await { owner.state != null || owner.loadError != null }
        assertEquals("keep me", owner.state?.fields?.body)
        assertEquals(ref, owner.state?.insertAfter)
        assertNotNull(owner.state?.validationError())
        owner.save()
        assertFalse(owner.state!!.saved)
    }
    @Test fun staleTaskStillRestoresEnteredDraftAfterProcessRecreation() {
        val snapshot = DocumentSnapshot(uri, TodoParser.parse(original))
        val ref = TaskRef.from(snapshot.document, snapshot.document.tasks.single())
        val draft = EditorState.edit(snapshot, ref).let { it.copy(fields = it.fields.copy(body = "do not lose me")) }
        val bundle = Bundle().also(draft::writeDraft)
        val store = object : DocumentStore {
            override fun read(uri: Uri) = original.replace("original", "external change")
            override fun write(uri: Uri, source: String) { fail("Stale draft cannot write") }
        }
        val owner = EditorViewModel(repo(store)) {}
        owner.open(EditorRequest.Edit(uri, ref), bundle)
        await { owner.state != null || owner.loadError != null }
        assertEquals("do not lose me", owner.state?.fields?.body)
        assertNotNull(owner.state?.validationError())
        assertFalse(owner.state!!.saved)
    }
    @Test fun rotationDuringBlockedCreateRetainsSingleSaveAndRefreshesOnce() {
        val enteredWrite = CountDownLatch(1)
        val releaseWrite = CountDownLatch(1)
        var writes = 0
        var refreshes = 0
        val store = object : DocumentStore {
            override fun read(uri: Uri) = ""
            override fun write(uri: Uri, source: String) {
                enteredWrite.countDown()
                assertTrue(releaseWrite.await(10, TimeUnit.SECONDS))
                writes++
            }
        }
        val repository = repo(store)
        val viewModelStore = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return EditorViewModel(repository) { refreshes++ } as T
            }
        }
        fun recreatedActivityOwner() = object : ViewModelStoreOwner { override val viewModelStore = viewModelStore }
        val first = ViewModelProvider(recreatedActivityOwner(), factory)[EditorViewModel::class.java]
        val state = EditorState.create(DocumentSnapshot(uri, TodoParser.parse("")), day)
        first.update(state.copy(fields = state.fields.copy(body = "once")))
        try {
            first.save()
            await { enteredWrite.count == 0L }
            val rotated = ViewModelProvider(recreatedActivityOwner(), factory)[EditorViewModel::class.java]
            rotated.open(EditorRequest.Create, Bundle())
            assertSame(first, rotated)
            assertTrue(rotated.state!!.saving)
            rotated.save() // Re-entry must not queue a second create.
            releaseWrite.countDown()
            await { rotated.state?.saved == true }
            assertEquals(1, writes)
            assertEquals(1, refreshes)
        } finally { releaseWrite.countDown(); viewModelStore.clear() }
    }
    @Test fun datedCreationPrefillsAnEditableDate() {
        val store = object : DocumentStore {
            override fun read(uri: Uri) = original
            override fun write(uri: Uri, source: String) {}
        }
        val owner = EditorViewModel(repo(store)) {}
        val chosen = LocalDate.of(2025, 1, 2)
        owner.open(EditorRequest.CreateOnDate(chosen), null)
        await { owner.state != null || owner.loadError != null }
        assertEquals(chosen, owner.state!!.fields.date)
        assertTrue(owner.state!!.fields.hasCheckbox)
        assertEquals(day, owner.state!!.withDate(day).fields.date)
    }
    @Test fun deletionIgnoresInvalidUnsavedFieldsAndRefreshesOnlyOnce() {
        var text = original
        var writes = 0
        var refreshes = 0
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val store = object : DocumentStore {
            override fun read(uri: Uri) = text
            override fun write(uri: Uri, source: String) {
                entered.countDown()
                assertTrue(release.await(10, TimeUnit.SECONDS))
                text = source; writes++
            }
        }
        val doc = TodoParser.parse(original)
        val owner = EditorViewModel(repo(store)) { refreshes++ }
        owner.open(EditorRequest.Edit(uri, TaskRef.from(doc, doc.tasks.single())), null)
        await { owner.state != null || owner.loadError != null }
        owner.update(owner.state!!.let { it.copy(fields = it.fields.copy(body = "", tags = listOf("invalid"))) })
        try {
            owner.delete()
            await { entered.count == 0L }
            owner.open(EditorRequest.Edit(uri, TaskRef.from(doc, doc.tasks.single())), Bundle())
            owner.delete()
            owner.save()
            release.countDown()
            await { owner.state?.saved == true }
            assertEquals("### 06.10.26\n", text)
            assertEquals(1, writes)
            assertEquals(1, refreshes)
            owner.delete()
            assertEquals(1, writes)
        } finally { release.countDown() }
    }
    @Test fun failedDeletionRetainsTheEditorAndCreationCannotDelete() {
        val store = object : DocumentStore {
            override fun read(uri: Uri) = original
            override fun write(uri: Uri, source: String) { throw DocumentAccessException("write failed") }
        }
        val doc = TodoParser.parse(original)
        val owner = EditorViewModel(repo(store)) { fail("Failed delete cannot refresh") }
        owner.open(EditorRequest.Edit(uri, TaskRef.from(doc, doc.tasks.single())), null)
        await { owner.state != null || owner.loadError != null }
        owner.delete()
        await { owner.state?.error != null }
        assertFalse(owner.state!!.saved)
        assertFalse(owner.state!!.saving)
        assertEquals("write failed", owner.state!!.error)
        assertEquals("original", owner.state!!.fields.body)
        val creator = EditorViewModel(repo(store)) { fail("Creation cannot delete") }
        creator.open(EditorRequest.Create, null)
        await { creator.state != null || creator.loadError != null }
        creator.delete()
        assertFalse(creator.state!!.saving)
        assertNull(creator.state!!.error)
    }

}
