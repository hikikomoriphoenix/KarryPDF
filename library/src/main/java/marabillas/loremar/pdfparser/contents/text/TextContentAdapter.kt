package marabillas.loremar.pdfparser.contents.text

import android.graphics.Color
import android.support.v4.util.SparseArrayCompat
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import marabillas.loremar.pdfparser.contents.ContentGroup
import marabillas.loremar.pdfparser.contents.PageContent
import marabillas.loremar.pdfparser.font.CustomTypefaceSpan
import marabillas.loremar.pdfparser.font.Font
import marabillas.loremar.pdfparser.font.FontMappings
import marabillas.loremar.pdfparser.font.FontName
import marabillas.loremar.pdfparser.objects.PDFString
import marabillas.loremar.pdfparser.utils.exts.toInt

internal class TextContentAdapter {
    private var spanBuilder = SpannableStringBuilder()
    private val pageContents = ArrayList<PageContent>()
    private val sb = StringBuilder()

    private fun resetAdapter() {
        spanBuilder = SpannableStringBuilder()
        pageContents.clear()
        sb.clear()
    }

    fun getContents(contentGroups: ArrayList<ContentGroup>, fonts: SparseArrayCompat<Font>): ArrayList<PageContent> {
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

    private fun processTextGroup(textGroup: TextGroup, fonts: SparseArrayCompat<Font>) {
        var line: ArrayList<TextElement>
        for (i in 0 until textGroup.size()) {
            line = textGroup[i]
            if (line != textGroup[0])
                spanBuilder.append('\n')
            for (j in 0 until line.size) {
                val s = SpannableString((line[j].tj as PDFString).value)

                // Style with typeface
                sb.clear().append(line[j].tf, 2, line[j].tf.indexOf(' '))
                val t = fonts[sb.toInt()]?.typeface ?: FontMappings[FontName.DEFAULT]
                val span = CustomTypefaceSpan(t)
                s.setSpan(span, 0, s.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)

                // Style with color
                if (line[j].rgb[0] != -1f) {
                    val red = Math.round(line[j].rgb[0] * 255)
                    val green = Math.round(line[j].rgb[1] * 255)
                    val blue = Math.round(line[j].rgb[2] * 255)
                    val colorSpan = ForegroundColorSpan(Color.rgb(red, green, blue))
                    s.setSpan(colorSpan, 0, s.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
                }

                spanBuilder.append(s)
            }
        }
    }

    private fun processTable(table: Table, fonts: SparseArrayCompat<Font>): TableContent {
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
}