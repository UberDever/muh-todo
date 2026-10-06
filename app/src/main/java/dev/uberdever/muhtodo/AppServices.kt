package dev.uberdever.muhtodo

import android.content.Context
import dev.uberdever.muhtodo.document.ContentResolverDocumentStore
import dev.uberdever.muhtodo.document.DocumentPreferences
import dev.uberdever.muhtodo.document.DocumentRepository

object AppServices {
    @Volatile private var instance: DocumentRepository? = null

    fun repository(context: Context): DocumentRepository = instance ?: synchronized(this) {
        instance ?: context.applicationContext.let { app ->
            DocumentRepository(ContentResolverDocumentStore(app.contentResolver), DocumentPreferences(app), app.contentResolver).also { instance = it }
        }
    }
}
