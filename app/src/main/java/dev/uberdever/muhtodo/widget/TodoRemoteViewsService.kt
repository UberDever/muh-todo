package dev.uberdever.muhtodo.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import dev.uberdever.muhtodo.AppServices
import dev.uberdever.muhtodo.R
import kotlinx.coroutines.runBlocking

class TodoRemoteViewsService : RemoteViewsService() {
    internal fun creationViews(): RemoteViews = RemoteViews(packageName, R.layout.widget_create).apply {
        setOnClickFillInIntent(R.id.create_entry, WidgetActions.createFillIn())
    }
    internal fun taskViews(row: WidgetRow.Task, uri: Uri?): RemoteViews = RemoteViews(packageName, R.layout.widget_task).apply {
        setTextViewText(R.id.body, row.body)
        setTextViewText(R.id.tags, TagLabels.withAge(row.ageDays, row.tagLabel, row.tags))
        setViewVisibility(R.id.tags, View.VISIBLE)
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
                WidgetRow.Create -> creationViews()
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
