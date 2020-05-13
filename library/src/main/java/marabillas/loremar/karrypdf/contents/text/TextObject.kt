package marabillas.loremar.karrypdf.contents.text

import marabillas.loremar.karrypdf.contents.PageObject
import marabillas.loremar.karrypdf.utils.multiplyTransformMatrices

internal class TextObject : PageObject {
    var column = -1
    var transformMatrix = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)

    val scaleX; get() = transformMatrix[0]
    val scaleY; get() = transformMatrix[3]

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
        return first().ty
    }

    override fun getX(): Float {
        return first().tx
    }

    fun computeAllElementsTransformation() {
        elements.forEach {
            val elementMatrix = it.computeElementTransformMatrix()
            it.tx = elementMatrix[4]
            it.ty = elementMatrix[5]
            it.scaleX = elementMatrix[0]
            it.scaleY = elementMatrix[3]
        }
    }

    private fun TextElement.computeElementTransformMatrix(): FloatArray {
        val textSpaceMatrix = multiplyTransformMatrices(textParamsMatrix, textMatrix)
        return multiplyTransformMatrices(textSpaceMatrix, transformMatrix)
    }
}