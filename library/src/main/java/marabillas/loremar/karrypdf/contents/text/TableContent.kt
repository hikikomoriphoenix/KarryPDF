package marabillas.loremar.karrypdf.contents.text

import android.text.SpannableStringBuilder
import marabillas.loremar.karrypdf.contents.PageContent

class TableContent : PageContent {
    val rows = ArrayList<Row>()

    class Row {
        val cells = ArrayList<Cell>()
    }

    data class Cell(val content: SpannableStringBuilder)
}