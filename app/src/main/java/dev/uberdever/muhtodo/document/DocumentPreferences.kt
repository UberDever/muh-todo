package dev.uberdever.muhtodo.document

import android.content.Context
import android.net.Uri

class DocumentPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("todo-metadata", Context.MODE_PRIVATE)
    fun documentUri(): Uri? = prefs.getString("document-uri", null)?.let(Uri::parse)
    fun setDocumentUri(uri: Uri) { prefs.edit().putString("document-uri", uri.toString()).apply() }
    fun widgetOptions(id: Int) = WidgetOptions(prefs.getBoolean("widget-$id-incomplete", false), prefs.getBoolean("widget-$id-tags", false))
    fun setWidgetOptions(id: Int, options: WidgetOptions) {
        prefs.edit().putBoolean("widget-$id-incomplete", options.incompleteFirst).putBoolean("widget-$id-tags", options.sortTags).apply()
    }
    fun removeWidget(id: Int) { prefs.edit().remove("widget-$id-incomplete").remove("widget-$id-tags").apply() }
}
