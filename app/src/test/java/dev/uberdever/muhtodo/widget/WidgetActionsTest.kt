package dev.uberdever.muhtodo.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import dev.uberdever.muhtodo.document.*
import dev.uberdever.muhtodo.editor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetActionsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uri = Uri.parse("content://test/todos")
    private val source = """
        ### 06.10.26
        - [ ] (#a) task
    """.trimIndent()
    private val doc = TodoParser.parse(source)
    private val ref = TaskRef.from(doc, doc.tasks.single())
    private class Store(var source: String) : DocumentStore {
        var writes = 0
        override fun read(uri: Uri) = source
        override fun write(uri: Uri, source: String) { this.source = source; writes++ }
    }
    private fun repo(store: Store): DocumentRepository {
        val preferences = DocumentPreferences(context).apply { setDocumentUri(uri) }
        return DocumentRepository(store, preferences, context.contentResolver)
    }
    @Test fun toggleMutatesCheckboxAndNeverStartsEditor() = runBlocking {
        val store = Store(source)
        val fill = WidgetActions.toggleFillIn(uri, ref)
        assertNull(WidgetActions.toggle(repo(store), fill))
        assertEquals(source.replace("[ ]", "[x]"), store.source)
        assertEquals(1, store.writes)
        val activity = Robolectric.buildActivity(WidgetActionActivity::class.java, fill).create().get()
        assertNull(shadowOf(activity).nextStartedActivity)
        assertTrue(activity.isFinishing)
        assertEquals(WidgetActions.TOGGLE, shadowOf(activity).broadcastIntents.last().action)
    }
    @Test fun bodyTapStartsExplicitEditorWithOriginalReference() {
        val activity = Robolectric.buildActivity(WidgetActionActivity::class.java, WidgetActions.editFillIn(uri, ref)).create().get()
        val started = shadowOf(activity).nextStartedActivity
        assertNotNull(started)
        assertEquals(EditorActivity::class.java.name, started.component?.className)
        assertEquals(EditorRequest.Edit(uri, ref), EditorIntents.decode(started))
        assertTrue(activity.isFinishing)
    }
    @Test fun plusUsesSameEditorInCreateMode() {
        assertEquals(EditorRequest.Create, EditorIntents.decode(EditorIntents.create(context)))
    }
    @Test fun malformedActionsDoNotWrite() = runBlocking {
        val store = Store(source)
        assertNotNull(WidgetActions.toggle(repo(store), Intent().setAction(WidgetActions.TOGGLE)))
        assertNotNull(WidgetActions.toggle(repo(store), WidgetActions.editFillIn(uri, ref)))
        assertEquals(0, store.writes)
    }
    @Test fun staleActionsFailWithoutWriting() = runBlocking {
        val store = Store(source.replace("task", "externally changed"))
        assertEquals("Document changed; reopen task.", WidgetActions.toggle(repo(store), WidgetActions.toggleFillIn(uri, ref)))
        assertEquals(0, store.writes)
    }
    @Test fun instancesHaveDistinctTemplatesAndMutableFillInRouting() {
        val one = WidgetActions.collectionTemplate(context, 1)
        val two = WidgetActions.collectionTemplate(context, 2)
        assertNotEquals(one, two)
        assertNull(shadowOf(one).savedIntent.action)
        assertNull(shadowOf(one).savedIntent.data)
        assertEquals(WidgetActionActivity::class.java.name, shadowOf(one).savedIntent.component?.className)
    }
}
