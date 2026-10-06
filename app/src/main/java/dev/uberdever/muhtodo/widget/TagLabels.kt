package dev.uberdever.muhtodo.widget

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import dev.uberdever.muhtodo.ui.TagColors

object TagLabels {
    fun colored(label: String?): CharSequence? {
        if (label == null || label == "^^^") return label
        return SpannableString(label).apply {
            var start = 0
            label.split(" ").forEach { tag ->
                setSpan(ForegroundColorSpan(TagColors.color(tag)), start, start + tag.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                start += tag.length + 1
            }
        }
    }
}
