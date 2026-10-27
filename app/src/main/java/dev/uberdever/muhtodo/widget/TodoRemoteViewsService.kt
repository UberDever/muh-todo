package dev.uberdever.muhtodo.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import dev.uberdever.muhtodo.AppServices
import dev.uberdever.muhtodo.R
import dev.uberdever.muhtodo.ui.AgeColors
import kotlinx.coroutines.runBlocking

class TodoRemoteViewsService : RemoteViewsService() {
    internal fun creationViews(row: WidgetRow.Create): RemoteViews = RemoteViews(packageName, R.layout.widget_create).apply {
        setTextViewText(R.id.entry_count, "${row.open}/${row.all}")
        setContentDescription(R.id.entry_count, "${row.open} open of ${row.all} checkbox entries")
        setOnClickFillInIntent(R.id.create_entry, WidgetActions.createFillIn())
    }
    internal fun taskViews(row: WidgetRow.Task, uri: Uri?): RemoteViews = RemoteViews(packageName, R.layout.widget_task).apply {
        val alpha = if (row.hasCheckbox && row.completed) 153 else 255
        val textColor = TagLabels.withAlpha(getColor(R.color.todo_text), alpha)
        setTextColor(R.id.body, textColor)
        setTextColor(R.id.checkbox, textColor)
        setInt(R.id.create_after, "setImageAlpha", alpha)
        setTextViewText(R.id.body, row.body)
        val age = AgeColors.stage(row.ageDays)
        val background = when (age) {
            AgeColors.Stage.RECENT -> R.drawable.age_card_recent
            AgeColors.Stage.TWO_WEEKS -> R.drawable.age_card_two_weeks
            AgeColors.Stage.MONTH -> R.drawable.age_card_month
            AgeColors.Stage.THREE_MONTHS -> R.drawable.age_card_three_months
            AgeColors.Stage.SIX_MONTHS -> R.drawable.age_card_six_months
            AgeColors.Stage.YEAR -> R.drawable.age_card_year
        }
        setInt(R.id.task_card, "setBackgroundResource", background)
        setInt(R.id.age_accent, "setBackgroundColor", TagLabels.withAlpha(
            if (age == AgeColors.Stage.RECENT) 0xFF59616E.toInt() else age.color, alpha))
        setTextViewText(R.id.age_label, TagLabels.age(row.ageDays))
        setTextColor(R.id.age_label, TagLabels.withAlpha(if (age.badge) 0xFFFFFFFF.toInt() else age.color, alpha))
        setContentDescription(R.id.age_label, AgeColors.description(row.ageDays))
        setInt(R.id.age_label, "setBackgroundResource", if (age.badge) R.drawable.age_badge_urgent else 0)
        setTextViewText(R.id.tags, TagLabels.colored(row.tagLabel, row.tags, alpha))
        setViewVisibility(R.id.tags, if (row.tagLabel == null) View.GONE else View.VISIBLE)
        setViewVisibility(R.id.checkbox, View.VISIBLE)
        setTextViewText(R.id.checkbox, if (!row.hasCheckbox) "•" else if (row.completed) "☑" else "☐")
        setContentDescription(R.id.checkbox, when {
            !row.hasCheckbox -> "Edit entry: ${row.body}"
            row.completed -> "Mark incomplete: ${row.body}"
            else -> "Mark complete: ${row.body}"
        })
        setContentDescription(R.id.create_after, "New entry using: ${row.body}")
        uri?.let {
            // Replace a recycled checkbox's action as well as its appearance.
            setOnClickFillInIntent(R.id.checkbox, if (row.hasCheckbox) WidgetActions.toggleFillIn(it, row.ref) else WidgetActions.editFillIn(it, row.ref))
            setOnClickFillInIntent(R.id.task_text, WidgetActions.editFillIn(it, row.ref))
            setOnClickFillInIntent(R.id.create_after, WidgetActions.createAfterFillIn(it, row.ref))
        }
    }
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
    private inner class Factory(private val widgetId: Int) : RemoteViewsFactory {
        private var rows: List<WidgetRow> = emptyList()
        private var uri: Uri? = null
        private var error: String? = null
        override fun onCreate() {}
        override fun onDataSetChanged() {
            try {
                val repository = AppServices.repository(applicationContext)
                val snapshot = runBlocking { repository.read() }
                uri = snapshot.uri
                rows = WidgetProjection.project(snapshot.document, repository.preferences.widgetOptions(widgetId))
                error = null
            } catch (e: Exception) {
                rows = emptyList(); uri = null; error = e.message ?: "Could not read document."
            }
        }
        override fun onDestroy() { rows = emptyList(); uri = null }
        override fun getCount() = if (error != null) 1 else rows.size
        override fun getViewAt(position: Int): RemoteViews? {
            error?.let {
                return RemoteViews(packageName, R.layout.widget_date).apply {
                    setTextViewText(R.id.date, "$it Open the app to select the file.")
                    setViewVisibility(R.id.date_add, View.GONE)
                }
            }
            return when (val row = rows.getOrNull(position)) {
                is WidgetRow.Create -> creationViews(row)
                is WidgetRow.Task -> taskViews(row, uri)
                null -> null
            }
        }
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 3
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = false
    }
}
