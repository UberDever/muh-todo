package dev.uberdever.muhtodo.widget

import android.content.Context
import android.content.Intent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import dev.uberdever.muhtodo.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetDocumentErrorTest {
    @Test fun missingDocumentRendersAnErrorInsteadOfFakeTasks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("todo-metadata", Context.MODE_PRIVATE).edit().clear().commit()
        val service = Robolectric.buildService(TodoRemoteViewsService::class.java).create().get()
        val factory = service.onGetViewFactory(Intent())
        factory.onDataSetChanged()
        assertEquals(1, factory.count)
        val row = factory.getViewAt(0)!!.apply(context, FrameLayout(context))
        assertTrue(row.findViewById<TextView>(R.id.date).text.contains("Select a Markdown document."))
        assertNull(row.findViewById<TextView>(R.id.body))
        factory.onDestroy()
    }
    @Test fun errorRowHasNoDateCreationButton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("todo-metadata", Context.MODE_PRIVATE).edit().clear().commit()
        val service = Robolectric.buildService(TodoRemoteViewsService::class.java).create().get()
        val factory = service.onGetViewFactory(Intent())
        factory.onDataSetChanged()
        val row = factory.getViewAt(0)!!.apply(context, FrameLayout(context))
        assertEquals(android.view.View.GONE, row.findViewById<android.view.View>(R.id.date_add).visibility)
    }

}
