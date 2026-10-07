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
        val arrow = view.findViewById<android.widget.ImageView>(R.id.create_after)
        assertNotNull(arrow.drawable)
        assertSame(view, arrow.parent)
        assertEquals("New entry using: body", arrow.contentDescription.toString())
        val plain = checked.copy(ref = ref.copy(expectedLine = "- (#a) plain"), body = "plain", completed = false, hasCheckbox = false)
        service.taskViews(plain, uri).reapply(service, view)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.checkbox).visibility)
        assertEquals("•", view.findViewById<TextView>(R.id.checkbox).text.toString())
        assertEquals("Edit entry: plain", view.findViewById<View>(R.id.checkbox).contentDescription.toString())
        assertEquals("New entry using: plain", view.findViewById<android.widget.ImageView>(R.id.create_after).contentDescription.toString())
        assertEquals("plain", view.findViewById<TextView>(R.id.body).text.toString())
        assertEquals("0d #a", view.findViewById<TextView>(R.id.tags).text.toString())
        service.taskViews(checked.copy(completed = false), uri).reapply(service, view)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.checkbox).visibility)
        assertEquals("☐", view.findViewById<TextView>(R.id.checkbox).text.toString())
    }
    @Test fun creationRowHasNoDateAndUsesTodayWhenOpened() {
        val service = Robolectric.buildService(TodoRemoteViewsService::class.java).create().get()
        val view = service.creationViews().apply(service, FrameLayout(service))
        assertEquals("+ New entry", view.findViewById<TextView>(R.id.create_entry).text.toString())
        assertEquals("New todo", view.findViewById<View>(R.id.create_entry).contentDescription.toString())
        assertEquals(dev.uberdever.muhtodo.editor.EditorRequest.Create,
            dev.uberdever.muhtodo.editor.EditorIntents.decode(WidgetActions.createFillIn()))
    }
    @Test fun coloredTagsAreReplacedWhenARowIsReused() {
        val service = Robolectric.buildService(TodoRemoteViewsService::class.java).create().get()
        val ref = TaskRef(1, "- [ ] (#buy #cook) body", LocalDate.of(2026, 10, 2))
        val row = WidgetRow.Task(ref, false, "body", "#buy #cook", tags = listOf("#buy", "#cook"))
        val view = service.taskViews(row, null).apply(service, FrameLayout(service))
        val text = view.findViewById<TextView>(R.id.tags).text as android.text.Spanned
        val spans = text.getSpans(0, text.length, android.text.style.ForegroundColorSpan::class.java)
        assertEquals(2, spans.size)
        assertEquals(dev.uberdever.muhtodo.ui.TagColors.color(row.tags), spans[1].foregroundColor)
        assertEquals(3, text.getSpanStart(spans[1]))
        assertEquals(text.length, text.getSpanEnd(spans[1]))
        service.taskViews(row.copy(tagLabel = "^^^"), null).reapply(service, view)
        val shorthand = view.findViewById<TextView>(R.id.tags).text
        assertEquals("0d ^^^", shorthand.toString())
        val inherited = shorthand as android.text.Spanned
        val inheritedSpans = inherited.getSpans(0, inherited.length, android.text.style.ForegroundColorSpan::class.java)
        assertEquals(2, inheritedSpans.size)
        assertEquals(spans[1].foregroundColor, inheritedSpans[1].foregroundColor)
        service.taskViews(row.copy(tagLabel = null), null).reapply(service, view)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.tags).visibility)
        assertEquals("0d", view.findViewById<TextView>(R.id.tags).text.toString())
    }

}
