package dev.uberdever.muhtodo.widget

import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import dev.uberdever.muhtodo.R
import dev.uberdever.muhtodo.document.TaskRef
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35])
class WidgetEntryRenderingTest {
    @Test fun reusedRowKeepsPlainEntryBulletAlignedAndRestoresTheNextCheckbox() {
        val service = Robolectric.buildService(TodoRemoteViewsService::class.java).create().get()
        val ref = TaskRef(1, "- [x] (#a) body", LocalDate.of(2026, 10, 2))
        val uri = Uri.parse("content://test/todos")
        val checked = WidgetRow.Task(ref, true, "body", "#a")
        val view = service.taskViews(checked, uri).apply(service, FrameLayout(service))
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.checkbox).visibility)
        assertEquals("☑", view.findViewById<TextView>(R.id.checkbox).text.toString())
        val plain = checked.copy(ref = ref.copy(expectedLine = "- (#a) plain"), body = "plain", completed = false, hasCheckbox = false)
        service.taskViews(plain, uri).reapply(service, view)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.checkbox).visibility)
        assertEquals("•", view.findViewById<TextView>(R.id.checkbox).text.toString())
        assertEquals("Edit entry: plain", view.findViewById<View>(R.id.checkbox).contentDescription.toString())
        assertEquals("plain", view.findViewById<TextView>(R.id.body).text.toString())
        assertEquals("#a", view.findViewById<TextView>(R.id.tags).text.toString())
        service.taskViews(checked.copy(completed = false), uri).reapply(service, view)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.checkbox).visibility)
        assertEquals("☐", view.findViewById<TextView>(R.id.checkbox).text.toString())
    }
}
