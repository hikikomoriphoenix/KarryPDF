package marabillas.loremar.pdfparser.contents

import android.text.SpannableStringBuilder

class TableContent : PageContent {
    val rows = ArrayList<Row>()

    class Row {
        val cells = ArrayList<Cell>()
    }

    data class Cell(val content: SpannableStringBuilder)
}