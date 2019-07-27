package marabillas.loremar.andpdf.contents.text

import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import marabillas.loremar.andpdf.contents.ContentGroup
import marabillas.loremar.andpdf.contents.PageContent
import marabillas.loremar.andpdf.font.CustomTypefaceSpan
import marabillas.loremar.andpdf.font.Font
import marabillas.loremar.andpdf.font.FontMappings
import marabillas.loremar.andpdf.font.FontName
import marabillas.loremar.andpdf.objects.PDFString

internal class TextContentAdapter {
    private var spanBuilder = SpannableStringBuilder()
    private val pageContents = ArrayList<PageContent>()
    private val sb = StringBuilder()

    private fun resetAdapter() {
        spanBuilder = SpannableStringBuilder()
        pageContents.clear()
        sb.clear()
    }

    fun getContents(contentGroups: ArrayList<ContentGroup>, fonts: HashMap<String, Font>): ArrayList<PageContent> {
        resetAdapter()
        var contentGroup: ContentGroup
        for (i in 0 until contentGroups.size) {
            contentGroup = contentGroups[i]
            if (contentGroup is TextGroup) {
                if (i > 0 && contentGroups[i - 1] is TextGroup) {
                    spanBuilder.append("\n\n")
                } else if (i > 0 && contentGroups[i - 1] is Table) {
                    spanBuilder = SpannableStringBuilder()
                }
                processTextGroup(contentGroup, fonts)
            } else if (contentGroup is Table) {
                if (spanBuilder.isNotEmpty()) {
                    val textContent = TextContent(spanBuilder)
                    pageContents.add(textContent)
                }
                val table = processTable(contentGroup, fonts)
                pageContents.add(table)

                // New SpannableStringBuilder for next content
                spanBuilder = SpannableStringBuilder()
            }
        }

        if (spanBuilder.isNotEmpty()) {
            val textContent = TextContent(spanBuilder)
            pageContents.add(textContent)
        }

        return pageContents
    }

    private fun processTextGroup(textGroup: TextGroup, fonts: HashMap<String, Font>) {
        var line: ArrayList<TextElement>
        for (i in 0 until textGroup.size()) {
            line = textGroup[i]
            if (line != textGroup[0])
                spanBuilder.append('\n')
            for (j in 0 until line.size) {
                sb.clear().append((line[j].tj as PDFString).value)
                val isFirst = (j == 0)
                reduceLongSpaces(sb, isFirst)
                val s = SpannableString(sb)

                // Style with typeface
                val t = fonts[line[j].fontResource]?.typeface ?: FontMappings[FontName.DEFAULT]
                val span = CustomTypefaceSpan(t)
                s.setSpan(span, 0, s.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)

                // Style with color
                if (line[j].rgb[0] != -1f) {
                    darkenBrighFontColors(line[j].rgb)
                    val red = Math.round(line[j].rgb[0] * 255f)
                    val green = Math.round(line[j].rgb[1] * 255f)
                    val blue = Math.round(line[j].rgb[2] * 255f)
                    val colorSpan = ForegroundColorSpan(Color.rgb(red, green, blue))
                    s.setSpan(colorSpan, 0, s.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
                }

                spanBuilder.append(s)
            }
        }
    }

    private fun darkenBrighFontColors(rgb: FloatArray) {
        if (rgb[0] > 0.5f && rgb[1] > 0.5f && rgb[2] > 0.5f) {
            // Get the largest value. The amount of darkening will be based on this.
            var max = 0
            rgb.forEachIndexed { i, c ->
                if (c > rgb[max])
                    max = i
            }
            // Get darken rate based on value. The brighter the value, the darker it will become, wherein a value of 1
            // will be darkened to 0%.
            var darkenRate = (1f - rgb[max]) / 0.5f
            // Allow not more than 50% darken rate.
            if (darkenRate > 0.5f) darkenRate = 0.5f
            rgb[0] *= darkenRate
            rgb[1] *= darkenRate
            rgb[2] *= darkenRate
        }
    }

    private fun processTable(table: Table, fonts: HashMap<String, Font>): TableContent {
        val content = TableContent()
        for (i in 0 until table.size()) {
            val rowContent = TableContent.Row()
            for (j in 0 until table[i].size()) {
                spanBuilder = SpannableStringBuilder()
                for (k in 0 until table[i][j].size()) {
                    processTextGroup(table[i][j][k], fonts)
                }
                val cellContent = TableContent.Cell(spanBuilder)
                rowContent.cells.add(cellContent)
            }
            content.rows.add(rowContent)
        }
        return content
    }

    private fun reduceLongSpaces(s: StringBuilder, isFirst: Boolean) {
        var isSpace = false
        var isIndent = false
        if (isFirst) {
            isIndent = true
        }
        s.forEachIndexed { i, c ->
            if (isIndent) {
                if (!isSpace(c)) {
                    isIndent = false
                    return@forEachIndexed
                } else {
                    return@forEachIndexed
                }
            }

            if (!isSpace && isSpace(c)) {
                isSpace = true
            } else if (isSpace && isSpace(c)) {
                s.deleteCharAt(i)
            } else if (isSpace && !isSpace(c)) {
                isSpace = false
            }
        }
    }

    private fun isSpace(c: Char): Boolean {
        // Reference: http://jkorpela.fi/chars/spaces.html
        return when (c) {
            ' ' -> true
            '\u00A0' -> true
            '\u1680' -> true
            '\u180E' -> true
            '\u2000' -> true
            '\u2001' -> true
            '\u2002' -> true
            '\u2003' -> true
            '\u2004' -> true
            '\u2005' -> true
            '\u2006' -> true
            '\u2007' -> true
            '\u2008' -> true
            '\u2009' -> true
            '\u200A' -> true
            '\u200B' -> true
            '\u202F' -> true
            '\u205F' -> true
            '\u3000' -> true
            '\uFEFF' -> true
            '\u0009' -> true
            '\u200C' -> true
            '\u2060' -> true
            else -> false
        }
    }
}