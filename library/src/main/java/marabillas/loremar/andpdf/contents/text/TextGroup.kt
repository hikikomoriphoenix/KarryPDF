package marabillas.loremar.andpdf.contents.text

import marabillas.loremar.andpdf.contents.ContentGroup

internal class TextGroup : ContentGroup {
    private val lines = ArrayList<ArrayList<TextElement>>()
    private val xList = mutableListOf<Float>()
    var isAList = false

    operator fun get(i: Int): ArrayList<TextElement> {
        return lines[i]
    }

    fun add(textElements: ArrayList<TextElement>) {
        lines.add(textElements)
    }

    fun remove(textElements: ArrayList<TextElement>) {
        lines.remove(textElements)
    }

    fun size(): Int {
        return lines.size
    }

    fun addX(x: Float) {
        xList.add(x)
    }

    fun deleteXAt(lineIndex: Int) {
        xList.removeAt(lineIndex)
    }

    fun getLineX(lineIndex: Int): Float {
        return xList[lineIndex]
    }
}