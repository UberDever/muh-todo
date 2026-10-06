package dev.uberdever.muhtodo.document

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

interface DocumentStore {
    fun read(uri: Uri): String
    fun write(uri: Uri, source: String)
}

class ContentResolverDocumentStore(private val resolver: ContentResolver) : DocumentStore {
    override fun read(uri: Uri): String = access("Could not read document.") {
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IOException("No input stream")
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
    }

    override fun write(uri: Uri, source: String) = access("Could not write document.") {
        // Encode first: invalid text must not truncate the original document.
        val buffer = Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(source))
        val bytes = ByteArray(buffer.remaining()).also(buffer::get)
        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
        stream.use { it.write(bytes) }
    }

    private inline fun <T> access(message: String, operation: () -> T): T = try {
        operation()
    } catch (e: IOException) {
        throw DocumentAccessException(message, e)
    } catch (e: SecurityException) {
        throw DocumentAccessException("Document permission unavailable; select the file again.", e)
    } catch (e: IllegalArgumentException) {
        throw DocumentAccessException(message, e)
    }
}
