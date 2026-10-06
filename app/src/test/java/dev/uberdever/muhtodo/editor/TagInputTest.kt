package dev.uberdever.muhtodo.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

class TagInputTest {
    @Test fun insertsAtCursorAndUsesExactlyOneSeparatorOnBothSides() {
        for ((text, cursor) in listOf("#buy #cook" to 5, "#buy   #cook" to 6)) {
            val inserted = TagInput.insert(TextFieldValue(text, TextRange(cursor)), "#eat")
            assertEquals("#buy #eat #cook", inserted.text)
            assertEquals(TextRange(9), inserted.selection)
        }
    }
    @Test fun replacesSelectionAndRemovesExtraSpacesWithoutReorderingOrDeduplicating() {
        val text = "  #buy   #old   #buy  "
        val start = text.indexOf("#old")
        val inserted = TagInput.insert(TextFieldValue(text, TextRange(start, start + 4)), "#cook")
        assertEquals("#buy #cook #buy", inserted.text)
        assertEquals(TextRange(10), inserted.selection)
    }
    @Test fun appendsWithoutLeadingTrailingOrRepeatedSpaces() {
        for (text in listOf("", " ", "#buy", "#buy ", "  #buy   ")) {
            val inserted = TagInput.insert(TextFieldValue(text, TextRange(text.length)), "#cook")
            assertEquals(if (text.isBlank()) "#cook" else "#buy #cook", inserted.text)
            assertEquals(TextRange(inserted.text.length), inserted.selection)
        }
    }
    @Test fun knownTagSuggestionsSortByAscendingCodePointsWithoutChangingThePool() {
        val tags = listOf("#𐐀", "#cook", "#buy", "#Ａ", "#123", "#A")
        assertEquals(listOf("#123", "#A", "#buy", "#cook", "#Ａ", "#𐐀"), TagInput.knownTags(tags))
        assertEquals("#𐐀", tags.first())
    }
}
