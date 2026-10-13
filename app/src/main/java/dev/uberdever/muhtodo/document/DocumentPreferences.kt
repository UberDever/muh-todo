package dev.uberdever.muhtodo.document

import android.content.Context
import android.net.Uri

class DocumentPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("todo-metadata", Context.MODE_PRIVATE)
    fun documentUri(): Uri? = prefs.getString("document-uri", null)?.let(Uri::parse)
    fun setDocumentUri(uri: Uri) { prefs.edit().putString("document-uri", uri.toString()).apply() }
    fun widgetOptions(id: Int) = WidgetOptions(
        sortTags = prefs.getBoolean("widget-$id-tags", false),
        tagsDescending = prefs.getBoolean("widget-$id-tags-descending", false),
        sortDates = prefs.getBoolean("widget-$id-dates", false),
        datesOlderFirst = prefs.getBoolean("widget-$id-dates-older-first", false),
        groupOrder = prefs.getString("widget-$id-group-order", null)?.let { EntryGroupOrder.parse(it.split(",")) }
            ?: if (prefs.getBoolean("widget-$id-incomplete", false) && prefs.getBoolean("widget-$id-completion-descending", false))
                listOf(EntryGroup.COMPLETE, EntryGroup.BULLETS, EntryGroup.INCOMPLETE) else EntryGroupOrder.default,
    )
    fun setWidgetOptions(id: Int, options: WidgetOptions) {
        prefs.edit().putString("widget-$id-group-order", EntryGroupOrder.validated(options.groupOrder).joinToString(",") { it.name })
            .remove("widget-$id-incomplete").remove("widget-$id-completion-descending")
            .putBoolean("widget-$id-tags", options.sortTags)
            .putBoolean("widget-$id-tags-descending", options.tagsDescending)
            .putBoolean("widget-$id-dates", options.sortDates)
            .putBoolean("widget-$id-dates-older-first", options.datesOlderFirst).apply()
    }
    fun removeWidget(id: Int) {
        prefs.edit().remove("widget-$id-group-order").remove("widget-$id-incomplete").remove("widget-$id-tags")
            .remove("widget-$id-completion-descending").remove("widget-$id-tags-descending")
            .remove("widget-$id-dates").remove("widget-$id-dates-older-first").apply()
    }
}
