package marabillas.loremar.pdfparser.contents.text

import marabillas.loremar.pdfparser.contents.PageObject

internal class TextObject : Iterable<TextElement>,
    PageObject {
    val td = FloatArray(2)
    var scaleX = 1f
    var scaleY = 1f
    var column = -1

    private val elements = ArrayList<TextElement>()

    override fun iterator(): Iterator<TextElement> {
        return elements.iterator()
    }

    fun add(textElement: TextElement) {
        elements.add(textElement)
    }

    fun update(updated: TextElement, index: Int) {
        elements[index] = updated
    }

    override fun getY(): Float {
        return td[1]
    }

    override fun getX(): Float {
        return td[0]
    }
}