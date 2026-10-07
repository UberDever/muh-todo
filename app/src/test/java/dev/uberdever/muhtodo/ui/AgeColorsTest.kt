package dev.uberdever.muhtodo.ui

import org.junit.Assert.*
import org.junit.Test

class AgeColorsTest {
    @Test fun ageStartsWhiteAndCapsColorNotTheLabelAtSixtyDays() {
        assertEquals(0xFFFFFFFF.toInt(), AgeColors.color(0))
        assertEquals(AgeColors.color(0), AgeColors.color(-3))
        assertEquals(AgeColors.color(60), AgeColors.color(140))
        assertNotEquals(AgeColors.color(1), AgeColors.color(34))
        assertNotEquals(AgeColors.color(34), AgeColors.color(60))
        assertEquals(140, AgeColors.label(140).removeSuffix("d").toInt())
    }
    @Test fun agesUseAnIncreasingReservedHueRangeExcludedFromTagColors() {
        var previousHue = -1.0
        for (days in 1L..60L) {
            val spec = AgeColors.oklch(days)
            assertTrue(spec.hue > previousHue)
            assertTrue(spec.hue in 0.0..60.0)
            previousHue = spec.hue
            assertTrue(TagColors.linearRgb(spec.lightness, spec.chroma, spec.hue).all { it in 0.0..1.0 })
        }
        for (i in 0..1000) {
            val spec = TagColors.oklch(listOf("#$i", "#tag$i", "#child"))
            assertTrue(spec.hue >= 80.0)
        }
    }
}
