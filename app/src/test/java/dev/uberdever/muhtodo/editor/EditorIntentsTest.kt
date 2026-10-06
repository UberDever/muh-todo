package dev.uberdever.muhtodo.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import dev.uberdever.muhtodo.document.TaskRef
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class EditorIntentsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uri = Uri.parse("content://test/todos")
    private val ref = TaskRef(3, "- [ ] (#a) body", LocalDate.of(2026, 10, 6))

    @Test fun createIntentIsExplicitAndDecodes() {
        val intent = EditorIntents.create(context)
        assertEquals(EditorActivity::class.java.name, intent.component?.className)
        assertEquals(EditorRequest.Create, EditorIntents.decode(intent))
    }

    @Test fun editIntentRoundTripsOriginalSourceReference() {
        val intent = EditorIntents.edit(context, uri, ref)
        assertEquals(EditorActivity::class.java.name, intent.component?.className)
        assertEquals(EditorRequest.Edit(uri, ref), EditorIntents.decode(intent))
    }

    @Test fun incompleteOrWrongSchemeIntentsAreRejected() {
        assertNull(EditorIntents.decode(Intent()))
        val edit = EditorIntents.edit(context, uri, ref)
        edit.replaceExtras(null)
        assertNull(EditorIntents.decode(edit))
        assertNull(EditorIntents.decode(EditorIntents.edit(context, Uri.parse("file:///tmp/test"), ref)))
        assertNull(EditorIntents.decode(EditorIntents.edit(context, uri, ref.copy(lineIndex = -1))))
    }
}
