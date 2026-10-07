package dev.uberdever.muhtodo.ui

import androidx.core.graphics.ColorUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class TagColorsTest {
    @Test fun orderedTupleColorsMatchThePortableReference() {
        val examples = listOf(
            listOf("#3") to 0xFFA3ADB7.toInt(),
            listOf("#3", "#belongings") to 0xFFA9A0E2.toInt(),
            listOf("#3", "#belongings", "#relocation") to 0xFFBC96E7.toInt(),
            listOf("#3", "#belongings", "#storage") to 0xFF91A5DF.toInt(),
            listOf("#3", "#relocation", "#belongings") to 0xFF6FBEC8.toInt(),
            listOf("#belongings", "#3") to 0xFFA8A0E2.toInt(),
        )
        for ((tags, expected) in examples) assertEquals(tags.toString(), expected, TagColors.color(tags))
    }
    @Test fun priorityLightnessIncreasesAndChildrenKeepThePrefixFamily() {
        var previous = 0.0
        for (priority in 0..99) {
            val spec = TagColors.oklch(listOf("#$priority", "#belongings", "#relocation"))
            assertTrue(spec.lightness > previous)
            previous = spec.lightness
        }
        val prefix = TagColors.oklch(listOf("#3", "#belongings"))
        for (tag in listOf("#relocation", "#storage", "#packing", "#delivery")) {
            val child = TagColors.oklch(listOf("#3", "#belongings", tag))
            val hueDelta = kotlin.math.abs(prefix.hue - child.hue).let { minOf(it, 360 - it) }
            assertTrue(hueDelta <= 32)
            assertTrue(kotlin.math.abs(prefix.lightness - child.lightness) <= .018)
        }
    }
    @Test fun exactUnicodeTagsHaveStableReadableColors() {
        for (tag in listOf("#buy", "#cook", "#work", "#life", "#покупки", "#𐐀", "#0/a!") + (0..999).map { "#tag$it" }) {
            val color = TagColors.color(tag)
            assertEquals(color, TagColors.color(tag))
            assertEquals(255, android.graphics.Color.alpha(color))
            for (background in listOf(0xFF121417.toInt(), 0xFF1B1E23.toInt())) {
                assertTrue("$tag must be readable", ColorUtils.calculateContrast(color, background) >= 4.5)
                val chipBackground = ColorUtils.compositeColors((color and 0x00FFFFFF) or 0x1A000000, background)
                assertTrue("$tag must be readable on its tinted chip", ColorUtils.calculateContrast(color, chipBackground) >= 4.5)
            }
        }
        assertNotEquals(TagColors.color("#𐐀"), TagColors.color("#𐐁"))
    }
    @Test fun orderDuplicatesAndExactSpellingRemainPartOfTheColorIdentity() {
        assertNotEquals(TagColors.color(listOf("#buy", "#cook")), TagColors.color(listOf("#cook", "#buy")))
        assertNotEquals(TagColors.color(listOf("#buy")), TagColors.color(listOf("#buy", "#buy")))
        assertNotEquals(TagColors.color("#buy"), TagColors.color("#BUY"))
        assertNotEquals(TagColors.color(listOf("#35", "#buy")), TagColors.color(listOf("#buy", "#35")))
    }
}
