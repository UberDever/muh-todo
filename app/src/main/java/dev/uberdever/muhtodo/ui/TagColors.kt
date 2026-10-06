package dev.uberdever.muhtodo.ui

import dev.uberdever.muhtodo.document.PriorityTags
import java.security.MessageDigest

/** Versioned, pool-independent ordered path colors. See docs/design/tuple-colors-priority.md. */
object TagColors {
    internal data class Oklch(val lightness: Double, val chroma: Double, val hue: Double)
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
    private fun units(digest: ByteArray): DoubleArray = DoubleArray(4) { chunk ->
        var bits = 0L
        for (i in 0..7) bits = (bits shl 8) or (digest[chunk * 8 + i].toLong() and 255)
        (bits ushr 11).toDouble() / 9007199254740992.0
    }
    private fun weight(index: Int) = when (index) {
        0 -> 1.0
        1 -> .35
        2 -> .15
        else -> .07 * StrictMath.pow(.5, (index - 3).toDouble())
    }
    fun color(tag: String): Int = color(listOf(tag))
    fun color(tags: List<String>): Int {
        val spec = oklch(tags)
        val channels = linearRgb(spec.lightness, spec.chroma, spec.hue).map { linear ->
            val encoded = if (linear <= .0031308) 12.92 * linear else 1.055 * StrictMath.pow(linear, 1 / 2.4) - .055
            StrictMath.rint(encoded.coerceIn(0.0, 1.0) * 255).toInt()
        }
        return (255 shl 24) or (channels[0] shl 16) or (channels[1] shl 8) or channels[2]
    }
    internal fun oklch(tags: List<String>): Oklch {
        val priority = PriorityTags.value(tags) / 99.0
        var lightness = .74 + .08 * priority
        var relativeChroma = .62 + .24 * priority
        var offset = 0.0
        var root: Double? = null
        var depth = 0
        var path = hash("muh-todo:semantic-path:v1".toByteArray(Charsets.UTF_8))
        tags.forEachIndexed { index, tag ->
            val digest = hash(("muh-todo:tuple-color:v1\u0000" + tag).toByteArray(Charsets.UTF_8))
            val u = units(digest)
            if (PriorityTags.numericValue(tag) != null) {
                offset += 2 * weight(index) * (2 * u[1] - 1)
            } else {
                path = hash(path + digest)
                if (root == null) {
                    root = 360 * u[0]
                    lightness += .008 * (2 * u[2] - 1)
                    relativeChroma += .03 * (2 * u[3] - 1)
                } else {
                    val direction = units(path)
                    val magnitude = .65 + .35 * direction[0]
                    val vector = DoubleArray(3) { 2 * direction[it + 1] - 1 }
                    val length = StrictMath.sqrt(vector.sumOf { it * it })
                    if (length == 0.0) { vector[0] = 1.0 } else for (i in vector.indices) vector[i] /= length
                    val strength = weight(depth) * magnitude
                    offset += 90 * strength * vector[0]
                    lightness += .05 * strength * vector[1]
                    relativeChroma += .20 * strength * vector[2]
                }
                depth++
            }
        }
        lightness = lightness.coerceIn(.70, .88)
        relativeChroma = relativeChroma.coerceIn(.45, .95)
        val hue = (((root ?: 250.0) + offset) % 360 + 360) % 360
        val chroma = if (root == null) .018 + .022 * priority else relativeChroma * maximumChroma(lightness, hue)
        return Oklch(lightness, chroma, hue)
    }
    private fun maximumChroma(lightness: Double, hue: Double): Double {
        var lower = 0.0
        var upper = .4
        repeat(24) {
            val middle = (lower + upper) / 2
            if (linearRgb(lightness, middle, hue).all { it in 0.0..1.0 }) lower = middle else upper = middle
        }
        return lower
    }
    private fun linearRgb(lightness: Double, chroma: Double, hue: Double): DoubleArray {
        val radians = StrictMath.toRadians(hue)
        val a = chroma * StrictMath.cos(radians)
        val b = chroma * StrictMath.sin(radians)
        val l = lightness + .3963377774 * a + .2158037573 * b
        val m = lightness - .1055613458 * a - .0638541728 * b
        val s = lightness - .0894841775 * a - 1.2914855480 * b
        val ll = l * l * l; val mm = m * m * m; val ss = s * s * s
        return doubleArrayOf(
            4.0767416621 * ll - 3.3077115913 * mm + .2309699292 * ss,
            -1.2684380046 * ll + 2.6097574011 * mm - .3413193965 * ss,
            -.0041960863 * ll - .7034186147 * mm + 1.7076147010 * ss,
        )
    }
}
