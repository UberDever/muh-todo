package dev.uberdever.muhtodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.Toast
import dev.uberdever.muhtodo.AppServices
import dev.uberdever.muhtodo.R
import dev.uberdever.muhtodo.editor.EditorIntents
import kotlinx.coroutines.*

class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = render(context, manager, ids)
    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { AppServices.repository(context).preferences.removeWidget(it) }
    }
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WidgetActions.REFRESH -> WidgetActions.refreshAll(context)
            WidgetActions.TOGGLE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val error = WidgetActions.toggle(AppServices.repository(context), intent)
                        withContext(Dispatchers.Main) {
                            error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                            WidgetActions.refreshAll(context)
                        }
                    } finally { pending.finish() }
                }
            }
            else -> super.onReceive(context, intent)
        }
    }
    companion object {
        fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget)
                val adapter = Intent(context, TodoRemoteViewsService::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).setData(Uri.parse("muhtodo://widget/$id"))
                views.setRemoteAdapter(R.id.todo_list, adapter)
                views.setEmptyView(R.id.todo_list, R.id.empty)
                views.setPendingIntentTemplate(R.id.todo_list, WidgetActions.collectionTemplate(context, id))
                views.setOnClickPendingIntent(R.id.add, PendingIntent.getActivity(context, id, EditorIntents.create(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                views.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(context, id,
                    Intent(context, WidgetProvider::class.java).setAction(WidgetActions.REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                val open = Intent(context, dev.uberdever.muhtodo.MainActivity::class.java)
                views.setOnClickPendingIntent(R.id.empty, PendingIntent.getActivity(context, id, open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id, views)
                manager.notifyAppWidgetViewDataChanged(id, R.id.todo_list)
            }
        }
    }
}
