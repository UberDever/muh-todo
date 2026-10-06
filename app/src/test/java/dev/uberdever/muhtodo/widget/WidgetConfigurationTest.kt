package dev.uberdever.muhtodo.widget

import android.app.Activity
import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import dev.uberdever.muhtodo.R
import dev.uberdever.muhtodo.document.DocumentPreferences
import dev.uberdever.muhtodo.document.WidgetOptions
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35])
class WidgetConfigurationTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val manager = AppWidgetManager.getInstance(app)
    private val preferences = DocumentPreferences(app)
    private val uri = Uri.parse("content://test/user-owned-todos")

    @Before fun setUp() {
        app.getSharedPreferences("todo-metadata", Context.MODE_PRIVATE).edit().clear().commit()
        preferences.setDocumentUri(uri)
        for (id in listOf(11, 22)) {
            shadowOf(manager).addBoundWidget(id, AppWidgetProviderInfo().apply {
                provider = ComponentName(app, WidgetProvider::class.java)
                initialLayout = R.layout.widget
            })
        }
    }
    private fun intent(id: Int) = Intent(app, WidgetConfigurationActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)

    @Test fun reopeningLoadsOnlyTheSelectedInstancesSavedOptions() {
        preferences.setWidgetOptions(11, WidgetOptions(true, false))
        preferences.setWidgetOptions(22, WidgetOptions(false, true))
        val first = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(11)).create().get()
        val second = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(22)).create().get()
        assertEquals(WidgetOptions(true, false), first.options)
        assertEquals(WidgetOptions(false, true), second.options)
    }
    @Test fun savePersistsOneInstanceAndReturnsItsIdWithoutChangingTheDocument() {
        preferences.setWidgetOptions(22, WidgetOptions(false, true))
        val activity = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(11)).create().get()
        activity.options = WidgetOptions(true, true)
        activity.saveConfiguration()
        assertEquals(WidgetOptions(true, true), DocumentPreferences(app).widgetOptions(11))
        assertEquals(WidgetOptions(false, true), DocumentPreferences(app).widgetOptions(22))
        assertEquals(uri, DocumentPreferences(app).documentUri())
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
        assertEquals(11, shadowOf(activity).resultIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
    }
    @Test fun cancelLeavesBothSavedInstancesUntouched() {
        preferences.setWidgetOptions(11, WidgetOptions(true, false))
        preferences.setWidgetOptions(22, WidgetOptions(false, true))
        val activity = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(11)).create().get()
        activity.options = WidgetOptions(false, false)
        activity.finish()
        assertEquals(WidgetOptions(true, false), preferences.widgetOptions(11))
        assertEquals(WidgetOptions(false, true), preferences.widgetOptions(22))
        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
    }
    @Test fun recreationKeepsUnsavedOptionsWithoutPersistingThem() {
        val controller = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(11)).create()
        controller.get().options = WidgetOptions(true, true)
        val saved = Bundle()
        controller.saveInstanceState(saved)
        val recreated = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(11)).create(saved).get()
        assertEquals(WidgetOptions(true, true), recreated.options)
        assertEquals(WidgetOptions(), preferences.widgetOptions(11))
    }
    @Test fun settingsTapRoutesToTheSpecificWidgetsConfigurationActivity() {
        WidgetProvider.render(app, manager, intArrayOf(11, 22))
        for (id in listOf(11, 22)) {
            val settings = shadowOf(manager).getViewFor(id).findViewById<android.view.View>(R.id.settings)
            assertTrue(settings.performClick())
            val launched = shadowOf(app).nextStartedActivity
            assertNotNull(launched)
            assertEquals(WidgetConfigurationActivity::class.java.name, launched.component?.className)
            assertEquals(id, launched.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
        }
    }
    @Test fun invalidOrOtherApplicationsWidgetCannotSaveOptions() {
        shadowOf(manager).addBoundWidget(33, AppWidgetProviderInfo().apply {
            provider = ComponentName("other.app", WidgetProvider::class.java.name)
        })
        for (id in listOf(-1, 44, 33)) {
            val activity = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(id)).create().get()
            assertTrue(activity.isFinishing)
            activity.options = WidgetOptions(true, true)
            activity.saveConfiguration()
            assertEquals(WidgetOptions(), preferences.widgetOptions(id))
            assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
        }
    }
}
