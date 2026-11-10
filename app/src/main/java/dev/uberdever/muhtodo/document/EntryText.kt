package dev.uberdever.muhtodo.document

/** Optional single-word title, separated from the body by a literal ". ". */
data class EntryText(val title: String?, val body: String) {
    val text: String get() = if (title == null) body else "$title. $body"

    companion object {
        private val prefix = Regex("([^\\s.]+)\\. (.+)")

        fun parse(text: String): EntryText {
            val match = prefix.matchEntire(text)
            return if (match != null && validTitle(match.groupValues[1]))
                EntryText(match.groupValues[1], match.groupValues[2])
            else EntryText(null, text)
        }

        fun validTitle(title: String): Boolean = title.isNotEmpty() && title.codePoints().allMatch {
            it != '.'.code && !Character.isWhitespace(it) && !Character.isSpaceChar(it) && !Character.isISOControl(it)
        }
    }
}
