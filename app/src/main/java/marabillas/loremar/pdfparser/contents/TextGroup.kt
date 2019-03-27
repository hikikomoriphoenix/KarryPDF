package marabillas.loremar.pdfparser.contents

internal class TextGroup : Iterable<ArrayList<TextElement>>, ContentGroup {
    private val lines = ArrayList<ArrayList<TextElement>>()

    override fun iterator(): Iterator<ArrayList<TextElement>> {
        return lines.iterator()
    }

    operator fun get(i: Int): ArrayList<TextElement> {
        return lines[i]
    }

    fun add(textElements: ArrayList<TextElement>) {
        lines.add(textElements)
    }
}