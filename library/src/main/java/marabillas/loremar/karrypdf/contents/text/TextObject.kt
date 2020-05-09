package marabillas.loremar.karrypdf.contents.text

import marabillas.loremar.karrypdf.contents.PageObject

internal class TextObject : PageObject {
    val td = FloatArray(2)
    var scaleX = 1f
    var scaleY = 1f
    var column = -1

    private val elements = ArrayList<TextElement>()

    inline fun forEach(action: (TextElement) -> Unit) {
        for (i in 0 until elements.size) action(elements[i])
    }

    inline fun forEachIndexed(action: (index: Int, TextElement) -> Unit) {
        var index = 0
        for (element in elements) action(index++, element)
    }

    fun asSequence(): Sequence<TextElement> {
        return elements.asSequence()
    }

    fun first(): TextElement {
        return elements.first()
    }

    fun last(): TextElement {
        return elements.last()
    }

    fun add(textElement: TextElement) {
        elements.add(textElement)
    }

    fun count(): Int {
        return elements.count()
    }

    fun elementAt(i: Int): TextElement {
        return elements[i]
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