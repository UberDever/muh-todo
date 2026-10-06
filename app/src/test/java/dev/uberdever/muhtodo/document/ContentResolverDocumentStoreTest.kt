package dev.uberdever.muhtodo.document

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileNotFoundException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35], manifest = Config.NONE)
class ContentResolverDocumentStoreTest {
    private lateinit var file: File
    private lateinit var provider: FileProvider
    private lateinit var store: ContentResolverDocumentStore
    private val uri = Uri.parse("content://test-file/todos.md")

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        file = File(context.cacheDir, "document-store-test.md")
        provider = FileProvider(file)
        provider.attachInfo(context, ProviderInfo().apply {
            authority = "test-file"
            exported = true
            applicationInfo = context.applicationInfo
        })
        ShadowContentResolver.registerProviderInternal("test-file", provider)
        store = ContentResolverDocumentStore(context.contentResolver)
    }

    @Test fun utf8BomAndLineEndingsRoundTripWithoutCopyingTheFile() {
        val text = "\uFEFF" + """
            ### 02.10.26
            - [ ] (#покупки) молоко
            unrelated
        """.trimIndent().replace("\n", "\r\n")
        file.writeBytes(text.toByteArray(Charsets.UTF_8))
        assertEquals(text, store.read(uri))
        store.write(uri, text)
        assertArrayEquals(text.toByteArray(Charsets.UTF_8), file.readBytes())
    }

    @Test fun shorterWriteTruncatesRatherThanAppending() {
        file.writeText("old document with trailing bytes")
        store.write(uri, "new")
        assertEquals("new", file.readText())
        assertEquals("wt", provider.lastMode)
    }

    @Test fun invalidUtf8IsRejected() {
        file.writeBytes(byteArrayOf(0xc3.toByte(), 0x28))
        assertThrows(DocumentAccessException::class.java) { store.read(uri) }
    }

    @Test fun unpairedSurrogateIsRejectedBeforeTruncation() {
        file.writeText("original")
        assertThrows(DocumentAccessException::class.java) { store.write(uri, "\uD800") }
        assertEquals("original", file.readText())
    }

    @Test fun providerFailuresAreReportedWithoutAutomaticRetry() {
        provider.fail = true
        assertThrows(DocumentAccessException::class.java) { store.read(uri) }
        assertThrows(DocumentAccessException::class.java) { store.write(uri, "new") }
        assertEquals(2, provider.openCount)
    }

    class FileProvider(private val file: File) : ContentProvider() {
        var lastMode: String? = null
        var fail = false
        var openCount = 0
        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            openCount++
            if (fail) throw FileNotFoundException("provider failure")
            lastMode = mode
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
        }
        override fun onCreate() = true
        override fun getType(uri: Uri) = "text/markdown"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
