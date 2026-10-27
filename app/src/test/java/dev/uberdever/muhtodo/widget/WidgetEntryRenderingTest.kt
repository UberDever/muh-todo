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
    @Test fun recycledRowsSwitchEveryAgeStageAndClearOldBadges() {
        val service = Robolectric.buildService(TodoRemoteViewsService::class.java).create().get()
        val ref = TaskRef(1, "- [ ] (#buy) body", LocalDate.of(2026, 10, 2))
        val base = WidgetRow.Task(ref, false, "body", "#buy", tags = listOf("#buy"))
        val view = service.taskViews(base, null).apply(service, FrameLayout(service))
        for (days in listOf(365L, 180L, 90L, 30L, 14L, 0L, -5L)) {
            service.taskViews(base.copy(ageDays = days), null).reapply(service, view)
            val age = view.findViewById<TextView>(R.id.age_label)
            val stage = dev.uberdever.muhtodo.ui.AgeColors.stage(days)
            assertEquals(dev.uberdever.muhtodo.ui.AgeColors.label(days), age.text.toString())
            assertEquals(if (stage.badge) 0xFFFFFFFF.toInt() else stage.color, age.currentTextColor)
            assertEquals(stage.badge, age.background != null)
            val accent = view.findViewById<View>(R.id.age_accent).background as android.graphics.drawable.ColorDrawable
            assertEquals(if (stage == dev.uberdever.muhtodo.ui.AgeColors.Stage.RECENT) 0xFF59616E.toInt() else stage.color, accent.color)
            val card = view.findViewById<View>(R.id.task_card).background as android.graphics.drawable.GradientDrawable
            val fillAlpha = if (stage == dev.uberdever.muhtodo.ui.AgeColors.Stage.RECENT) 8 else if (stage.badge) 28 else 18
            assertEquals(TagLabels.withAlpha(stage.color, fillAlpha), card.color!!.defaultColor)
            val richAge = age.text as android.text.Spanned
            val emphasis = richAge.getSpans(0, richAge.length, android.text.style.StyleSpan::class.java)
            assertEquals(android.graphics.Typeface.BOLD, emphasis.single().style)
            assertEquals("#buy", view.findViewById<TextView>(R.id.tags).text.toString())
            assertEquals("body", view.findViewById<TextView>(R.id.body).text.toString())
        }
        service.taskViews(base.copy(ageDays = 386, completed = true), null).reapply(service, view)
        assertEquals("1y 21d ☢️", view.findViewById<TextView>(R.id.age_label).text.toString())
        assertEquals(153, android.graphics.Color.alpha(view.findViewById<TextView>(R.id.age_label).currentTextColor))
        assertTrue(view.findViewById<TextView>(R.id.age_label).contentDescription.contains("386 days old"))
    }

    @Test fun completedCheckboxRowsAreDimmedAndRecyclingRestoresFullOpacity() {
        val service = Robolectric.buildService(TodoRemoteViewsService::class.java).create().get()
        val row = WidgetRow.Task(TaskRef(1, "- [x] done", LocalDate.of(2026, 10, 2)), true, "done", null)
        val view = service.taskViews(row, null).apply(service, FrameLayout(service))
        assertEquals(153, android.graphics.Color.alpha(view.findViewById<TextView>(R.id.body).currentTextColor))
        assertEquals(153, view.findViewById<android.widget.ImageView>(R.id.create_after).imageAlpha)
        service.taskViews(row.copy(completed = false), null).reapply(service, view)
        assertEquals(255, android.graphics.Color.alpha(view.findViewById<TextView>(R.id.body).currentTextColor))
        assertEquals(255, view.findViewById<android.widget.ImageView>(R.id.create_after).imageAlpha)
        service.taskViews(row.copy(hasCheckbox = false), null).reapply(service, view)
        assertEquals(255, android.graphics.Color.alpha(view.findViewById<TextView>(R.id.body).currentTextColor))
        assertEquals(255, view.findViewById<android.widget.ImageView>(R.id.create_after).imageAlpha)
    }

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
        assertSame(view.findViewById<View>(R.id.task_card), arrow.parent)
        assertEquals("New entry using: body", arrow.contentDescription.toString())
        val plain = checked.copy(ref = ref.copy(expectedLine = "- (#a) plain"), body = "plain", completed = false, hasCheckbox = false)
        service.taskViews(plain, uri).reapply(service, view)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.checkbox).visibility)
        assertEquals("•", view.findViewById<TextView>(R.id.checkbox).text.toString())
        assertEquals("Edit entry: plain", view.findViewById<View>(R.id.checkbox).contentDescription.toString())
        assertEquals("New entry using: plain", view.findViewById<android.widget.ImageView>(R.id.create_after).contentDescription.toString())
        assertEquals("plain", view.findViewById<TextView>(R.id.body).text.toString())
        assertEquals("#a", view.findViewById<TextView>(R.id.tags).text.toString())
        assertEquals("0d", view.findViewById<TextView>(R.id.age_label).text.toString())
        service.taskViews(checked.copy(completed = false), uri).reapply(service, view)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.checkbox).visibility)
        assertEquals("☐", view.findViewById<TextView>(R.id.checkbox).text.toString())
    }
    @Test fun creationRowHasNoDateAndUsesTodayWhenOpened() {
        val service = Robolectric.buildService(TodoRemoteViewsService::class.java).create().get()
        val view = service.creationViews(WidgetRow.Create(3, 7)).apply(service, FrameLayout(service))
        assertEquals("+ New entry", view.findViewById<TextView>(R.id.create_label).text.toString())
        assertEquals("3/7", view.findViewById<TextView>(R.id.entry_count).text.toString())
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
        assertEquals(1, spans.size)
        assertEquals(dev.uberdever.muhtodo.ui.TagColors.color(row.tags), spans[0].foregroundColor)
        assertEquals(0, text.getSpanStart(spans[0]))
        assertEquals(text.length, text.getSpanEnd(spans[0]))
        service.taskViews(row.copy(tagLabel = "^^^"), null).reapply(service, view)
        val shorthand = view.findViewById<TextView>(R.id.tags).text
        assertEquals("^^^", shorthand.toString())
        val inherited = shorthand as android.text.Spanned
        val inheritedSpans = inherited.getSpans(0, inherited.length, android.text.style.ForegroundColorSpan::class.java)
        assertEquals(1, inheritedSpans.size)
        assertEquals(spans[0].foregroundColor, inheritedSpans[0].foregroundColor)
        service.taskViews(row.copy(tagLabel = null), null).reapply(service, view)
        assertEquals(View.GONE, view.findViewById<View>(R.id.tags).visibility)
        assertEquals("0d", view.findViewById<TextView>(R.id.age_label).text.toString())
    }

}
