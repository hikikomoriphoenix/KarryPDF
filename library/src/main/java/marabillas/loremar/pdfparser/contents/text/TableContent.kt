package marabillas.loremar.pdfparser.contents.text

import android.text.SpannableStringBuilder
import marabillas.loremar.pdfparser.contents.PageContent

class TableContent : PageContent {
    val rows = ArrayList<Row>()

    class Row {
        val cells = ArrayList<Cell>()
    }

    data class Cell(val content: SpannableStringBuilder)
}