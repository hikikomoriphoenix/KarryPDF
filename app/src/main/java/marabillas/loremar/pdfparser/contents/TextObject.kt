package marabillas.loremar.pdfparser.contents

internal class TextObject : Iterable<TextElement> {
    val td = FloatArray(2)
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
}