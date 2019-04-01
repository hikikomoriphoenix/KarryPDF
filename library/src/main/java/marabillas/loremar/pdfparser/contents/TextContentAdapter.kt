package marabillas.loremar.pdfparser.contents

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import marabillas.loremar.pdfparser.font.CustomTypefaceSpan
import marabillas.loremar.pdfparser.font.FontMappings
import marabillas.loremar.pdfparser.font.FontName
import marabillas.loremar.pdfparser.objects.PDFString

internal class TextContentAdapter(
    private val contentGroups: ArrayList<ContentGroup>,
    private val pageFonts: HashMap<String, Typeface>
) {
    private var spanBuilder = SpannableStringBuilder()
    private val pageContents = ArrayList<PageContent>()

    fun getContents(): ArrayList<PageContent> {
        contentGroups.forEachIndexed { i, it ->
            when (it) {
                is TextGroup -> {
                    when {
                        i > 0 && contentGroups[i - 1] is Table -> {
                            spanBuilder = SpannableStringBuilder()
                        }
                        i > 0 && contentGroups[i - 1] is TextGroup -> {
                            spanBuilder.append("\n\n")
                        }
                    }
                    processTextGroup(it)
                }
                is Table -> {
                    if (spanBuilder.isNotEmpty()) {
                        val textContent = TextContent(spanBuilder)
                        pageContents.add(textContent)
                    }
                    val table = processTable(it)
                    pageContents.add(table)
                }
            }
        }

        if (spanBuilder.isNotEmpty()) {
            val textContent = TextContent(spanBuilder)
            pageContents.add(textContent)
        }

        return pageContents
    }

    private fun processTextGroup(textGroup: TextGroup) {
        textGroup.forEach {
            if (it != textGroup.first())
                spanBuilder.append("\n")
            it.forEach { e ->
                val s = SpannableString((e.tj as PDFString).value)
                val f = e.tf
                    .substringBefore(" ")
                    .substringAfter("/")
                    .trim()
                val t = pageFonts[f] ?: FontMappings[FontName.DEFAULT]

                val span = CustomTypefaceSpan(t)
                s.setSpan(span, 0, s.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)

                spanBuilder.append(s)
            }
        }
    }

    private fun processTable(table: Table): TableContent {
        val content = TableContent()
        table.forEach { row ->
            val rowContent = TableContent.Row()
            row.forEach { cell ->
                spanBuilder = SpannableStringBuilder()
                cell.forEach { textGroup ->
                    processTextGroup(textGroup)
                }
                val cellContent = TableContent.Cell(spanBuilder)
                rowContent.cells.add(cellContent)
            }
            content.rows.add(rowContent)
        }
        return content
    }
}