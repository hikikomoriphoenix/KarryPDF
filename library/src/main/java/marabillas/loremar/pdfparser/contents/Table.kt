package marabillas.loremar.pdfparser.contents

internal class Table : Iterable<Table.Row>, ContentGroup {
    private val table = ArrayList<Row>()

    override fun iterator(): Iterator<Row> {
        return table.iterator()
    }

    operator fun get(i: Int): Row {
        return table[i]
    }

    fun add(row: Row) {
        table.add(row)
    }

    class Row : Iterable<Cell> {
        private val cells = ArrayList<Cell>()

        override fun iterator(): Iterator<Cell> {
            return cells.iterator()
        }

        operator fun get(i: Int): Cell {
            return cells[i]
        }

        fun add(cell: Cell) {
            cells.add(cell)
        }
    }

    class Cell : Iterable<TextGroup> {
        private val txtGrps = ArrayList<TextGroup>()

        override fun iterator(): Iterator<TextGroup> {
            return txtGrps.iterator()
        }

        operator fun get(i: Int): TextGroup {
            return txtGrps[i]
        }

        fun add(textGroup: TextGroup) {
            txtGrps.add(textGroup)
        }
    }
}