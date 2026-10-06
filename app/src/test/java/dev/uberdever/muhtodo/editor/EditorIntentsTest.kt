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
    @Test fun datedCreationRejectsMalformedOrOutOfRangeDates() {
        val date = LocalDate.of(2026, 1, 2)
        assertEquals(EditorRequest.CreateOnDate(date), EditorIntents.decode(EditorIntents.create(context, date)))
        for (value in listOf("garbage", "2026-02-30", "1999-01-01", "2100-01-01")) {
            assertNull(EditorIntents.decode(EditorIntents.create(context).putExtra("create.date", value)))
        }
    }

}
