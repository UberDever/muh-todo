package dev.uberdever.muhtodo.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.uberdever.muhtodo.document.TagSyntax

internal object TagInput {
    fun insert(value: TextFieldValue, tag: String): TextFieldValue {
        fun clean(text: String) = text.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
        val before = clean(value.text.substring(0, value.selection.min))
        val after = clean(value.text.substring(value.selection.max))
        val prefix = if (before.isEmpty()) "" else "$before "
        val suffix = if (after.isEmpty()) "" else " $after"
        val text = prefix + tag + suffix
        return TextFieldValue(text, TextRange(text.length))
    }
    fun knownTags(tags: List<String>): List<String> = tags.sortedWith(TagSyntax::compareTokens)
}
