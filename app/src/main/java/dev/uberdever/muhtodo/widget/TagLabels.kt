package dev.uberdever.muhtodo.widget

import android.graphics.Typeface
import android.text.style.StyleSpan
import dev.uberdever.muhtodo.ui.AgeColors
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import dev.uberdever.muhtodo.ui.TagColors

object TagLabels {
    internal fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    fun age(days: Long): CharSequence = SpannableString(AgeColors.label(days)).apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun colored(label: String?, tags: List<String>, alpha: Int = 255): CharSequence? {
        if (label == null) return null
        return SpannableString(label).apply {
            setSpan(ForegroundColorSpan(withAlpha(TagColors.color(tags), alpha)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
