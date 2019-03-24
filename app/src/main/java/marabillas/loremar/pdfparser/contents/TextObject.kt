package marabillas.loremar.pdfparser.contents

class TextObject : Iterable<TextElement> {
    val td = FloatArray(2)
    private val elements = ArrayList<TextElement>()

    override fun iterator(): Iterator<TextElement> {
        return elements.iterator()
    }

    fun add(textElement: TextElement) {
        elements.add(textElement)
    }
}