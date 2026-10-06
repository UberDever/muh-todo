package dev.uberdever.muhtodo.ui

import org.junit.Assert.*
import org.junit.Test

class PriorityAdjustmentTest {
    @Test fun dragUsesItsOriginalValueAndTotalTravelAndClampsAtTheEdges() {
        assertEquals(35, PriorityAdjustment.drag(30, 40f, 8f))
        assertEquals(69, PriorityAdjustment.drag(70, -8f, 8f))
        assertEquals(70, PriorityAdjustment.drag(70, 3f, 8f))
        assertEquals(0, PriorityAdjustment.drag(10, -800f, 8f))
        assertEquals(99, PriorityAdjustment.drag(90, 800f, 8f))
    }
}
