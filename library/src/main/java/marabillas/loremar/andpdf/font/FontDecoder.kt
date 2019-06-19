package marabillas.loremar.andpdf.font

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.andpdf.contents.PageObject
import marabillas.loremar.andpdf.contents.text.TextElement
import marabillas.loremar.andpdf.contents.text.TextObject
import marabillas.loremar.andpdf.objects.*
import marabillas.loremar.andpdf.utils.exts.toInt

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
                    mainSB.clear()
                    mainSB.append(e.tf, 2, e.tf.indexOf(' '))
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