package dev.uberdever.muhtodo.document

import org.junit.Assert.*
import org.junit.Test

class PriorityTagsTest {
    @Test fun decimalDigitsArePrioritiesAndHugeValuesSaturateWithoutOverflow() {
        assertEquals(35, PriorityTags.numericValue("#0035"))
        assertEquals(35, PriorityTags.numericValue("#٣٥"))
        assertEquals(99, PriorityTags.numericValue("#" + "9".repeat(1000)))
        for (tag in listOf("#", "#-3", "#3.5", "#1/2", "#work", "#²")) {
            assertNull(tag, PriorityTags.numericValue(tag))
        }
    }
    @Test fun firstNumericTagControlsPriorityAndExistingPositionIsPreserved() {
        val tags = listOf("#buy", "#0035", "#cook", "#69", "#buy")
        assertEquals(35, PriorityTags.value(tags))
        assertEquals(listOf("#buy", "#cook", "#69", "#buy"), PriorityTags.withoutPriority(tags))
        assertEquals(listOf("#buy", "#20", "#cook", "#69", "#buy"), PriorityTags.withPriority(tags, 20))
        val zero = PriorityTags.withPriority(tags, 0)
        assertEquals(listOf("#buy", "#0", "#cook", "#69", "#buy"), zero)
        assertEquals(0, PriorityTags.value(zero))
    }
    @Test fun newZeroIsOmittedAndNonzeroIsPrepended() {
        val tags = listOf("#buy", "#buy")
        assertEquals(tags, PriorityTags.withPriority(tags, 0))
        assertEquals(listOf("#69", "#buy", "#buy"), PriorityTags.withPriority(tags, 69))
        assertEquals(0, PriorityTags.value(emptyList()))
    }
    @Test fun semanticEditingPreservesRawPriorityAndOtherNumericTokens() {
        val tags = listOf("#buy", "#9999", "#cook", "#69")
        assertEquals(listOf("#work", "#9999", "#69", "#work"),
            PriorityTags.withOrdinaryTags(tags, listOf("#work", "#69", "#work")))
        assertEquals(listOf("#9999"), PriorityTags.withOrdinaryTags(tags, emptyList()))
    }
    @Test fun removingTagsBeforePriorityDoesNotPromoteAnotherNumericTag() {
        assertEquals(listOf("#35", "#69"),
            PriorityTags.withOrdinaryTags(listOf("#buy", "#35", "#69"), listOf("#69")))
    }
}
