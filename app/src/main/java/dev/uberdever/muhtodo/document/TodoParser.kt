package dev.uberdever.muhtodo.document

import java.time.DateTimeException
import java.time.LocalDate

object TodoParser {
    private val header = Regex("### ([0-9]{2})\\.([0-9]{2})\\.([0-9]{2})")
    // Reserve checkbox-like tokens while allowing Markdown links as plain bodies.
    private val checkboxLike = Regex("^\\[[^]]*\\](?:\\s|$)")

    fun parse(source: String): ParsedDocument {
        val lines = sourceLines(source)
        val sections = mutableListOf<DateSection>()
        val tasks = mutableListOf<Todo>()
        val knownTags = linkedSetOf<String>()
        var previousTags: List<String>? = null
        for ((index, line) in lines.withIndex()) {
            val text = if (index == 0) line.content.removePrefix("\uFEFF") else line.content
            val date = dateHeader(text)
            if (date != null) {
                if (sections.isNotEmpty()) sections[sections.lastIndex] = sections.last().copy(endLineExclusive = index)
                sections += DateSection(date, index, lines.size)
                previousTags = null
                continue
            }
            val section = sections.lastOrNull() ?: continue
            if (!text.startsWith("- ")) continue
            val hasCheckbox = text.startsWith("- [ ] ") || text.startsWith("- [x] ")
            val remainder = text.substring(if (hasCheckbox) 6 else 2)
            if (!hasCheckbox && checkboxLike.containsMatchIn(remainder)) continue
            val payload = payload(remainder, previousTags) ?: continue
            tasks += Todo(index, section.headerLineIndex, section.date, hasCheckbox && text[3] == 'x', payload.body, payload.tags, payload.form, hasCheckbox)
            previousTags = payload.tags
            if (payload.form == TagForm.EXPLICIT) knownTags += payload.tags
        }
        return ParsedDocument(source, sections, tasks, knownTags.toList())
    }

    private fun dateHeader(text: String): LocalDate? {
        val match = header.matchEntire(text) ?: return null
        return try {
            LocalDate.of(2000 + match.groupValues[3].toInt(), match.groupValues[2].toInt(), match.groupValues[1].toInt())
        } catch (_: DateTimeException) { null }
    }

    private data class Payload(val body: String, val tags: List<String>, val form: TagForm)

    private fun payload(text: String, previousTags: List<String>?): Payload? {
        if (text.isEmpty()) return null
        if (text.startsWith("^^^")) {
            if (!text.startsWith("^^^ ") || text.length <= 4 || previousTags == null) return null
            return Payload(text.substring(4), previousTags, TagForm.INHERITED)
        }
        if (text.startsWith("(#")) {
            val end = text.indexOf(')')
            if (end < 0 || end + 2 >= text.length || text[end + 1] != ' ') return null
            val tags = text.substring(1, end).split(' ')
            if (tags.any { !TagSyntax.isValidToken(it) }) return null
            return Payload(text.substring(end + 2), tags, TagForm.EXPLICIT)
        }
        return Payload(text, emptyList(), TagForm.NONE)
    }
}

internal data class SourceLine(val start: Int, val content: String, val ending: String) {
    val end: Int get() = start + content.length + ending.length
}

internal fun sourceLines(source: String): List<SourceLine> {
    val result = mutableListOf<SourceLine>()
    var start = 0
    while (start < source.length) {
        var end = start
        while (end < source.length && source[end] != '\n' && source[end] != '\r') end++
        val content = source.substring(start, end)
        val ending = when {
            end == source.length -> ""
            source[end] == '\r' && end + 1 < source.length && source[end + 1] == '\n' -> "\r\n"
            else -> source[end].toString()
        }
        result += SourceLine(start, content, ending)
        start = end + ending.length
    }
    return result
}
