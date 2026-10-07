package dev.uberdever.muhtodo.document

import android.content.Context
import android.net.Uri

class DocumentPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("todo-metadata", Context.MODE_PRIVATE)
    fun documentUri(): Uri? = prefs.getString("document-uri", null)?.let(Uri::parse)
    fun setDocumentUri(uri: Uri) { prefs.edit().putString("document-uri", uri.toString()).apply() }
    fun widgetOptions(id: Int) = WidgetOptions(
        // Retain the old enablement key so upgrades preserve incomplete-first settings.
        sortCompletion = prefs.getBoolean("widget-$id-incomplete", false),
        sortTags = prefs.getBoolean("widget-$id-tags", false),
        completionDescending = prefs.getBoolean("widget-$id-completion-descending", false),
        tagsDescending = prefs.getBoolean("widget-$id-tags-descending", false),
        sortDates = prefs.getBoolean("widget-$id-dates", false),
        datesOlderFirst = prefs.getBoolean("widget-$id-dates-older-first", false),
    )
    fun setWidgetOptions(id: Int, options: WidgetOptions) {
        prefs.edit().putBoolean("widget-$id-incomplete", options.sortCompletion)
            .putBoolean("widget-$id-tags", options.sortTags)
            .putBoolean("widget-$id-completion-descending", options.completionDescending)
            .putBoolean("widget-$id-tags-descending", options.tagsDescending)
            .putBoolean("widget-$id-dates", options.sortDates)
            .putBoolean("widget-$id-dates-older-first", options.datesOlderFirst).apply()
    }
    fun removeWidget(id: Int) {
        prefs.edit().remove("widget-$id-incomplete").remove("widget-$id-tags")
            .remove("widget-$id-completion-descending").remove("widget-$id-tags-descending")
            .remove("widget-$id-dates").remove("widget-$id-dates-older-first").apply()
    }
}
