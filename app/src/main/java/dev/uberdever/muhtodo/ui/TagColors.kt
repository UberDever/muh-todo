package dev.uberdever.muhtodo.ui

import androidx.core.graphics.ColorUtils
import java.math.BigInteger
import java.security.MessageDigest

/** Display-only, stable across processes, locales and known-tag pool changes. */
object TagColors {
    fun color(tag: String): Int {
        val digest = MessageDigest.getInstance("SHA-256").digest(tag.toByteArray(Charsets.UTF_8))
        val hue = BigInteger(1, digest).mod(BigInteger.valueOf(360)).toFloat()
        // Light pastels stay readable even at the darker blue/red hues.
        return ColorUtils.HSLToColor(floatArrayOf(hue, 0.82f, 0.74f))
    }
}
