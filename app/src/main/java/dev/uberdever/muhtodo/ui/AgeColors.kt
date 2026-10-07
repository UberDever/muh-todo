package dev.uberdever.muhtodo.ui

/** View-only age: keep the actual day count, cap only the color's urgency. */
object AgeColors {
    fun label(days: Long) = "${days}d"
    fun color(days: Long): Int = if (days <= 0) 0xFFFFFFFF.toInt() else TagColors.toArgb(oklch(days))
    internal fun oklch(days: Long): TagColors.Oklch {
        val amount = days.coerceIn(0, 60) / 60.0
        val lightness = .98 - .22 * amount
        val hue = 40 * amount
        val chroma = minOf(.30 * amount, .99 * TagColors.maximumChroma(lightness, hue))
        return TagColors.Oklch(lightness, chroma, hue)
    }
}
