package dev.uberdever.muhtodo.ui

/** Display-only age bands. Warm colors were selected in OKLCH, outside the tag hue range. */
object AgeColors {
    enum class Stage(val fromDays: Long, val color: Int, val emoji: String, val description: String, val badge: Boolean = false) {
        RECENT(0, 0xFFFFFFFF.toInt(), "", "Less than two weeks"),
        TWO_WEEKS(14, 0xFFFDC679.toInt(), "⏳", "Two weeks or more"),
        MONTH(30, 0xFFFCA76B.toInt(), "⚠️", "One month or more"),
        THREE_MONTHS(90, 0xFFFB7E5F.toInt(), "🔥", "Three months or more"),
        SIX_MONTHS(180, 0xFFFB5058.toInt(), "🚨", "Six months or more", true),
        YEAR(365, 0xFFFA2B53.toInt(), "☢️", "One year or more", true),
    }
    fun stage(days: Long) = Stage.entries.lastOrNull { days >= it.fromDays } ?: Stage.RECENT
    fun color(days: Long) = stage(days).color
    fun label(days: Long, includeUrgency: Boolean = true): String {
        val age = if (days < 365) "${days}d" else buildString {
            append("${days / 365}y")
            if (days % 365 != 0L) append(" ${days % 365}d")
        }
        val emoji = if (includeUrgency) stage(days).emoji else ""
        return if (emoji.isEmpty()) age else "$age $emoji"
    }
    fun description(days: Long, includeUrgency: Boolean = true) = if (days < 0) "${-days} days in the future"
        else "$days days old." + if (includeUrgency) " ${stage(days).description}." else ""
}
