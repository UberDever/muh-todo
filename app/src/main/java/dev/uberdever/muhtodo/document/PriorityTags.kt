package dev.uberdever.muhtodo.document

/** First decimal-only tag is priority; other tokens retain their spelling and order. */
object PriorityTags {
    fun numericValue(tag: String): Int? {
        if (!tag.startsWith('#') || tag.length < 2) return null
        var value = 0
        var offset = 1
        while (offset < tag.length) {
            val codePoint = tag.codePointAt(offset)
            if (Character.getType(codePoint) != Character.DECIMAL_DIGIT_NUMBER.toInt()) return null
            value = minOf(99, value * 10 + Character.digit(codePoint, 10))
            offset += Character.charCount(codePoint)
        }
        return value
    }
    private fun index(tags: List<String>) = tags.indexOfFirst { numericValue(it) != null }
    fun value(tags: List<String>): Int = tags.firstNotNullOfOrNull(::numericValue) ?: 0
    fun withoutPriority(tags: List<String>): List<String> {
        val index = index(tags)
        return tags.filterIndexed { i, _ -> i != index }
    }
    fun withPriority(tags: List<String>, priority: Int): List<String> {
        require(priority in 0..99)
        val index = index(tags)
        if (index >= 0) return tags.toMutableList().apply { this[index] = "#$priority" }
        return if (priority == 0) tags else listOf("#$priority") + tags
    }
    fun withOrdinaryTags(tags: List<String>, ordinaryTags: List<String>): List<String> {
        val index = index(tags)
        if (index < 0) return ordinaryTags
        val nextNumeric = index(ordinaryTags).let { if (it < 0) ordinaryTags.size else it }
        return ordinaryTags.toMutableList().apply { add(minOf(index, size, nextNumeric), tags[index]) }
    }
}
