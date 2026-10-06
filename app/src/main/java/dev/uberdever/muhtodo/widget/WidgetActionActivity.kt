package dev.uberdever.muhtodo.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import dev.uberdever.muhtodo.editor.EditorIntents
import dev.uberdever.muhtodo.editor.EditorRequest

/** Collection views share a template. Route its row actions without launching UI for a toggle. */
class WidgetActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.action) {
            EditorIntents.CREATE -> when (val request = EditorIntents.readCreation(intent)) {
                EditorRequest.Create -> startActivity(EditorIntents.create(this))
                is dev.uberdever.muhtodo.editor.EditorRequest.CreateOnDate -> startActivity(EditorIntents.create(this, request.date))
                else -> Unit
            }
            WidgetActions.TOGGLE -> if (EditorIntents.readReference(intent) != null)
                sendBroadcast(Intent(intent).setComponent(android.content.ComponentName(this, WidgetProvider::class.java)).setFlags(0))
            EditorIntents.CREATE_AFTER -> EditorIntents.readReference(intent)?.let { request ->
                startActivity(EditorIntents.createAfter(this, request.uri, request.ref))
            }
            EditorIntents.EDIT -> EditorIntents.readReference(intent)?.let { request ->
                startActivity(EditorIntents.edit(this, request.uri, request.ref))
            }
        }
        finish()
    }
}
