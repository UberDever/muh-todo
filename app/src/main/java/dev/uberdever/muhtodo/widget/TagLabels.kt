package dev.uberdever.muhtodo.widget

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import dev.uberdever.muhtodo.ui.TagColors
import dev.uberdever.muhtodo.ui.AgeColors

object TagLabels {
    internal fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    fun withAge(days: Long, label: String?, tags: List<String>, alpha: Int = 255): CharSequence {
        val age = AgeColors.label(days)
        val text = age + if (label == null) "" else " $label"
        return SpannableString(text).apply {
            setSpan(ForegroundColorSpan(withAlpha(AgeColors.color(days), alpha)), 0, age.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, age.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (label != null) setSpan(ForegroundColorSpan(withAlpha(TagColors.color(tags), alpha)), age.length + 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun colored(label: String?, tags: List<String>): CharSequence? {
        if (label == null) return null
        return SpannableString(label).apply {
            setSpan(ForegroundColorSpan(TagColors.color(tags)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
