package dev.uberdever.muhtodo.document

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object TodoMutation {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yy", Locale.ROOT)

    fun toggle(document: ParsedDocument, task: Todo): String {
        requireTask(document, task)
        require(task.hasCheckbox) { "This entry has no checkbox." }
        val start = sourceLines(document.source)[task.lineIndex].start
        return document.source.replaceRange(start + 3, start + 4, if (task.completed) " " else "x")
    }

    fun edit(document: ParsedDocument, task: Todo, fields: TaskFields): String {
        requireTask(document, task)
        validate(fields)
        val lines = sourceLines(document.source)
        if (fields.date == task.date) {
            val form = if (fields.tags == task.tags) task.form else explicitForm(fields.tags)
            val preceding = document.tasks.lastOrNull { it.sectionHeaderLine == task.sectionHeaderLine && it.lineIndex < task.lineIndex }
            val content = checkedLine(fields, form, preceding?.tags)
            return replaceLine(document.source, lines[task.lineIndex], content)
        }

        var source = document.source
        val successor = document.tasks.firstOrNull { it.sectionHeaderLine == task.sectionHeaderLine && it.lineIndex > task.lineIndex }
        if (successor?.form == TagForm.INHERITED) {
            val oldFields = TaskFields(successor.date, successor.completed, successor.tags, successor.body, successor.hasCheckbox)
            val content = checkedLine(oldFields, explicitForm(successor.tags), null)
            source = replaceLine(source, lines[successor.lineIndex], content)
        }
        val removed = lines[task.lineIndex]
        source = source.removeRange(removed.start, removed.end)
        val remaining = TodoParser.parse(source)
        val predecessor = predecessor(remaining, fields.date)
        val form = if (task.form == TagForm.INHERITED && task.tags == fields.tags && predecessor?.tags == fields.tags) {
            TagForm.INHERITED
        } else explicitForm(fields.tags)
        return insertLine(remaining, fields, form)
    }

    fun insert(document: ParsedDocument, fields: TaskFields, inheritTags: Boolean): String {
        val preceding = predecessor(document, fields.date)
        require(!inheritTags || preceding != null) { "There is no preceding task to inherit tags from." }
        val effective = if (inheritTags) fields.copy(tags = preceding!!.tags) else fields
        validate(effective)
        return insertLine(document, effective, if (inheritTags) TagForm.INHERITED else explicitForm(effective.tags))
    }

    private fun insertLine(document: ParsedDocument, fields: TaskFields, form: TagForm): String {
        val section = document.sections.lastOrNull { it.date == fields.date }
        val preceding = predecessor(document, fields.date)
        val content = checkedLine(fields, form, preceding?.tags)
        val lines = sourceLines(document.source)
        val fallback = lines.lastOrNull { it.ending.isNotEmpty() }?.ending ?: "\n"
        if (section == null) {
            val source = document.source
            val separator = if (source.isEmpty() || source == "\uFEFF" || source.endsWith('\n') || source.endsWith('\r')) "" else fallback
            return source + separator + "### ${fields.date.format(dateFormat)}" + fallback + content + fallback
        }
        val anchor = lines[preceding?.lineIndex ?: section.headerLineIndex]
        if (anchor.ending.isEmpty()) return document.source + fallback + content
        return document.source.substring(0, anchor.end) + content + anchor.ending + document.source.substring(anchor.end)
    }

    private fun predecessor(document: ParsedDocument, date: LocalDate): Todo? {
        val section = document.sections.lastOrNull { it.date == date } ?: return null
        return document.tasks.lastOrNull { it.sectionHeaderLine == section.headerLineIndex }
    }

    private fun checkedLine(fields: TaskFields, form: TagForm, precedingTags: List<String>?): String {
        val checkbox = when {
            !fields.hasCheckbox -> "- "
            fields.completed -> "- [x] "
            else -> "- [ ] "
        }
        val metadata = when (form) {
            TagForm.NONE -> ""
            TagForm.EXPLICIT -> "(${fields.tags.joinToString(" ")}) "
            TagForm.INHERITED -> "^^^ "
        }
        val line = checkbox + metadata + fields.body
        val context = buildString {
            append("### ${fields.date.format(dateFormat)}\n")
            if (form == TagForm.INHERITED && precedingTags != null) {
                append("- [ ] ")
                if (precedingTags.isNotEmpty()) append("(${precedingTags.joinToString(" ")}) ")
                append("preceding task\n")
            }
            append(line)
        }
        val parsed = TodoParser.parse(context).tasks.lastOrNull()
        require(parsed != null && parsed.body == fields.body && parsed.tags == fields.tags && parsed.completed == fields.completed && parsed.form == form && parsed.hasCheckbox == fields.hasCheckbox) {
            "Body looks like task metadata. Add a checkbox or tags, or change the body."
        }
        return line
    }

    private fun explicitForm(tags: List<String>) = if (tags.isEmpty()) TagForm.NONE else TagForm.EXPLICIT

    private fun replaceLine(source: String, line: SourceLine, content: String) = source.replaceRange(line.start, line.start + line.content.length, content)

    private fun validate(fields: TaskFields) {
        require(fields.date.year in 2000..2099) { "Date must be between 2000 and 2099." }
        require(fields.body.isNotBlank() && '\n' !in fields.body && '\r' !in fields.body) { "Enter a nonblank, single-line task." }
        require(fields.tags.all(TagSyntax::isValidToken)) { "Enter space-separated tags, e.g. #buy #cook." }
        require(fields.hasCheckbox || !fields.completed) { "An entry without a checkbox cannot have a completion state." }
    }

    private fun requireTask(document: ParsedDocument, task: Todo) {
        require(task in document.tasks) { "Task no longer exists in this document." }
    }
}
