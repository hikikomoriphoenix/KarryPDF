package marabillas.loremar.pdfparser.contents.text

import marabillas.loremar.pdfparser.contents.ContentGroup

internal class Table : ContentGroup {
    private val table = ArrayList<Row>()

    operator fun get(i: Int): Row {
        return table[i]
    }

    fun size(): Int {
        return table.size
    }

    fun add(row: Row) {
        table.add(row)
    }

    class Row {
        private val cells = ArrayList<Cell>()

        operator fun get(i: Int): Cell {
            return cells[i]
        }

        fun size(): Int {
            return cells.size
        }

        fun add(cell: Cell) {
            cells.add(cell)
        }
    }

    class Cell {
        private val txtGrps = ArrayList<TextGroup>()

        operator fun get(i: Int): TextGroup {
            return txtGrps[i]
        }

        fun size(): Int {
            return txtGrps.size
        }

        fun add(textGroup: TextGroup) {
            txtGrps.add(textGroup)
        }
    }
}