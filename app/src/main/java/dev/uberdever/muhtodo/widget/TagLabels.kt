package dev.uberdever.muhtodo.widget

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import dev.uberdever.muhtodo.ui.TagColors

object TagLabels {
    fun colored(label: String?, tags: List<String>): CharSequence? {
        if (label == null) return null
        return SpannableString(label).apply {
            setSpan(ForegroundColorSpan(TagColors.color(tags)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
