package marabillas.loremar.pdfparser.font

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.contents.PageObject
import marabillas.loremar.pdfparser.contents.text.TextElement
import marabillas.loremar.pdfparser.contents.text.TextObject
import marabillas.loremar.pdfparser.objects.*
import marabillas.loremar.pdfparser.utils.exts.toInt

internal class FontDecoder(private val pageObjects: ArrayList<PageObject>, private val fonts: SparseArrayCompat<Font>) {
    private val mainSB = StringBuilder()
    private val secondarySB = StringBuilder()

    fun decodeEncoded() {
        pageObjects
            .asSequence()
            .filter { it is TextObject }
            .forEach {
                val textObject = it as TextObject
                textObject.forEachIndexed forEachTextElement@{ i, e ->
                    val fEnd = mainSB.clear().append(e.tf).indexOf(' ')
                    mainSB.delete(fEnd, mainSB.length)
                    mainSB.delete(0, 2)
                    val cmap = fonts[mainSB.toInt()]?.cmap

                    var newTj: PDFObject? = null
                    when (e.tj) {
                        is PDFArray -> {
                            mainSB.clear().append('[')
                            e.tj.forEach { p ->
                                if (p is PDFString) {
                                    if (cmap != null) {
                                        mainSB.append(
                                            cmap.decodeString(p.original)
                                        )
                                    } else {
                                        mainSB.append(
                                            p.original
                                        )
                                    }
                                } else {
                                    val n = p as Numeric
                                    mainSB.append(n.value.toFloat())
                                }
                            }
                            mainSB.append(']')
                            newTj = mainSB.toPDFArray(secondarySB.clear())
                        }
                        is PDFString -> {
                            if (cmap != null) {
                                newTj = cmap.decodeString(e.tj.original).toPDFString()
                            } else {
                                newTj = e.tj
                            }
                        }
                    }

                    val updated = TextElement(
                        td = e.td.copyOf(),
                        tj = newTj ?: e.tj,
                        tf = e.tf,
                        ts = e.ts,
                        rgb = e.rgb
                    )

                    textObject.update(updated, i)
                }
            }
    }
}