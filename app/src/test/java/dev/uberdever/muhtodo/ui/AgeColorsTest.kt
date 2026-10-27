package dev.uberdever.muhtodo.ui

import org.junit.Assert.*
import org.junit.Test

class AgeColorsTest {
    @Test fun plainAgeFormattingOmitsUrgencyButKeepsTheExactElapsedTime() {
        assertEquals("1y 35d", AgeColors.label(400, includeUrgency = false))
        assertEquals("400 days old.", AgeColors.description(400, includeUrgency = false))
        assertEquals("-5d", AgeColors.label(-5, includeUrgency = false))
    }

    @Test fun stageBoundariesAreInclusiveAndRemainDistinctAfterOneYear() {
        val stages = AgeColors.Stage.entries
        assertEquals(AgeColors.Stage.RECENT, AgeColors.stage(-1))
        for (index in 1 until stages.size) {
            val stage = stages[index]
            assertEquals(stages[index - 1], AgeColors.stage(stage.fromDays - 1))
            assertEquals(stage, AgeColors.stage(stage.fromDays))
        }
        assertEquals(AgeColors.Stage.YEAR, AgeColors.stage(Long.MAX_VALUE))
        assertEquals(AgeColors.Stage.YEAR.color, AgeColors.color(10_000))
    }
    @Test fun stageColorsAndWhiteBadgeTextAreReadableOnDarkBackgrounds() {
        fun luminance(color: Int): Double = listOf(16 to .2126, 8 to .7152, 0 to .0722).sumOf { (shift, weight) ->
            val channel = ((color ushr shift) and 255) / 255.0
            weight * if (channel <= .04045) channel / 12.92 else StrictMath.pow((channel + .055) / 1.055, 2.4)
        }
        val background = luminance(0xFF121417.toInt())
        for (stage in AgeColors.Stage.entries) {
            assertTrue(stage.name, (luminance(stage.color) + .05) / (background + .05) >= 4.5)
        }
        assertTrue(1.05 / (luminance(0xFFB9162D.toInt()) + .05) >= 4.5)
    }

    @Test fun colorsChangeOnlyAtTheFiveAgeThresholds() {
        val bands = listOf(0L..13L, 14L..29L, 30L..89L, 90L..179L, 180L..364L, 365L..2000L)
        for (band in bands) {
            assertEquals("Stage must be stable within $band", AgeColors.color(band.first), AgeColors.color(band.last))
        }
        assertEquals(6, bands.map { AgeColors.color(it.first) }.toSet().size)
        assertEquals(0xFFFFFFFF.toInt(), AgeColors.color(-10))
    }
    @Test fun urgencyEmojiAndCompactYearLabelsShowTheAgeStage() {
        assertEquals("5d", AgeColors.label(5))
        assertEquals("21d ⏳", AgeColors.label(21))
        assertEquals("35d ⚠️", AgeColors.label(35))
        assertEquals("114d 🔥", AgeColors.label(114))
        assertEquals("214d 🚨", AgeColors.label(214))
        assertEquals("1y ☢️", AgeColors.label(365))
        assertEquals("1y 21d ☢️", AgeColors.label(386))
        assertEquals("2y ☢️", AgeColors.label(730))
        assertEquals("-5d", AgeColors.label(-5))
    }
}
