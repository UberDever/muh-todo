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
    @Test fun commonTagsAreVisiblySeparated() {
        val colors = listOf("#buy", "#cook", "#work", "#life").map(TagColors::color)
        for (i in colors.indices) for (j in 0 until i) {
            val distance = kotlin.math.sqrt(listOf(16, 8, 0).sumOf { shift ->
                val d = ((colors[i] shr shift) and 255) - ((colors[j] shr shift) and 255)
                d * d.toDouble()
            })
            assertTrue("Common tag colors too close: $distance", distance >= 60)
        }
    }
}
