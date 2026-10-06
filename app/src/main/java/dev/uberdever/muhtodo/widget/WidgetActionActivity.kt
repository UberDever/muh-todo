package dev.uberdever.muhtodo.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import dev.uberdever.muhtodo.editor.EditorIntents

/** Collection views share a template. Route its row actions without launching UI for a toggle. */
class WidgetActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.action) {
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
