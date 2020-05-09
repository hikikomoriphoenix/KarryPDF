package marabillas.loremar.karrypdf.font

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

internal class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateMeasureState(p: TextPaint?) {
        p?.typeface = typeface
    }

    override fun updateDrawState(tp: TextPaint?) {
        tp?.typeface = typeface
    }
}