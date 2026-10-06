package dev.uberdever.muhtodo.document

import java.time.LocalDate

data class ParsedDocument(val source: String, val sections: List<DateSection>, val tasks: List<Todo>, val knownTags: List<String>)
data class DateSection(val date: LocalDate, val headerLineIndex: Int, val endLineExclusive: Int)
data class Todo(val lineIndex: Int, val sectionHeaderLine: Int, val date: LocalDate, val completed: Boolean, val body: String, val tags: List<String>, val form: TagForm)
enum class TagForm { NONE, EXPLICIT, INHERITED }
data class TaskFields(val date: LocalDate, val completed: Boolean, val tags: List<String>, val body: String)
