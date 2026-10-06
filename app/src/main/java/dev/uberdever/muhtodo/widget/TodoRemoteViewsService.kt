package dev.uberdever.muhtodo.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import dev.uberdever.muhtodo.AppServices
import dev.uberdever.muhtodo.R
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking

class TodoRemoteViewsService : RemoteViewsService() {
    internal fun taskViews(row: WidgetRow.Task, uri: Uri?): RemoteViews = RemoteViews(packageName, R.layout.widget_task).apply {
        setTextViewText(R.id.body, row.body)
        setTextViewText(R.id.tags, row.tagLabel)
        setViewVisibility(R.id.tags, if (row.tagLabel == null) View.GONE else View.VISIBLE)
        setViewVisibility(R.id.checkbox, if (row.hasCheckbox) View.VISIBLE else View.GONE)
        setTextViewText(R.id.checkbox, if (row.completed) "☑" else "☐")
        setContentDescription(R.id.checkbox, if (row.completed) "Mark incomplete: ${row.body}" else "Mark complete: ${row.body}")
        uri?.let {
            if (row.hasCheckbox) setOnClickFillInIntent(R.id.checkbox, WidgetActions.toggleFillIn(it, row.ref))
            setOnClickFillInIntent(R.id.task_text, WidgetActions.editFillIn(it, row.ref))
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
                return RemoteViews(packageName, R.layout.widget_date).apply { setTextViewText(R.id.date, "$it Open the app to select the file.") }
            }
            return when (val row = rows.getOrNull(position)) {
                is WidgetRow.DateHeader -> RemoteViews(packageName, R.layout.widget_date).apply {
                    setTextViewText(R.id.date, row.date.format(DateTimeFormatter.ofPattern("dd.MM.yy")))
                }
                is WidgetRow.Task -> taskViews(row, uri)
                null -> null
            }
        }
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 2
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = false
    }
}
