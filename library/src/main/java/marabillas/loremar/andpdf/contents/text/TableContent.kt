package marabillas.loremar.andpdf.contents.text

import android.text.SpannableStringBuilder
import marabillas.loremar.andpdf.contents.PageContent

class TableContent : PageContent {
    val rows = ArrayList<Row>()

    class Row {
        val cells = ArrayList<Cell>()
    }

    data class Cell(val content: SpannableStringBuilder)
}