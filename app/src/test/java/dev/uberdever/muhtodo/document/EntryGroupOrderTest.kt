package dev.uberdever.muhtodo.document

import org.junit.Assert.*
import org.junit.Test

class EntryGroupOrderTest {
    @Test fun movingAGroupKeepsAllThreeExactlyOnceAndClampsAtTheEnds() {
        val initial = EntryGroupOrder.default
        val moved = EntryGroupOrder.move(initial, EntryGroup.COMPLETE, 0)
        assertEquals(listOf(EntryGroup.COMPLETE, EntryGroup.INCOMPLETE, EntryGroup.BULLETS), moved)
        assertEquals(initial, EntryGroupOrder.move(moved, EntryGroup.COMPLETE, 99))
        assertEquals(initial, EntryGroupOrder.move(initial, EntryGroup.INCOMPLETE, -1))
    }
    @Test fun invalidStoredOrdersFallBackToACompletePermutation() {
        for (tokens in listOf(emptyList(), listOf("INCOMPLETE"), listOf("COMPLETE", "COMPLETE", "BULLETS"),
            listOf("INCOMPLETE", "COMPLETE", "UNKNOWN"))) {
            assertEquals(EntryGroupOrder.default, EntryGroupOrder.parse(tokens))
        }
        assertEquals(listOf(EntryGroup.BULLETS, EntryGroup.COMPLETE, EntryGroup.INCOMPLETE),
            EntryGroupOrder.parse(listOf("BULLETS", "COMPLETE", "INCOMPLETE")))
    }
}
