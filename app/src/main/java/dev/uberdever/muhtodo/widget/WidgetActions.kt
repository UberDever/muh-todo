package dev.uberdever.muhtodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dev.uberdever.muhtodo.document.*
import dev.uberdever.muhtodo.editor.EditorIntents
import kotlinx.coroutines.CancellationException

object WidgetActions {
    const val TOGGLE = "dev.uberdever.muhtodo.TOGGLE"
    const val REFRESH = "dev.uberdever.muhtodo.REFRESH"
    fun configurationIntent(context: Context, widgetId: Int) = Intent(context, WidgetConfigurationActivity::class.java)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        .setData(Uri.parse("muhtodo://widget/$widgetId/settings"))
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    fun toggleFillIn(uri: Uri, ref: TaskRef) = EditorIntents.reference(Intent().setAction(TOGGLE), uri, ref)
    fun createAfterFillIn(uri: Uri, ref: TaskRef) = EditorIntents.reference(Intent().setAction(EditorIntents.CREATE_AFTER), uri, ref)
    fun editFillIn(uri: Uri, ref: TaskRef) = EditorIntents.reference(Intent().setAction(EditorIntents.EDIT), uri, ref)
    fun collectionTemplate(context: Context, widgetId: Int): PendingIntent = PendingIntent.getActivity(context, widgetId,
        Intent(context, WidgetActionActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
    suspend fun toggle(repository: DocumentRepository, intent: Intent): String? {
        if (intent.action != TOGGLE) return "Invalid widget action; refresh the widget."
        val request = EditorIntents.readReference(intent) ?: return "Invalid widget action; refresh the widget."
        return try { repository.toggle(request.uri, request.ref); null }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { e.message ?: "Could not update task." }
    }
    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
        WidgetProvider.render(context, manager, ids)
    }
}
