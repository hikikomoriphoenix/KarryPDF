package marabillas.loremar.karrypdf.font

import marabillas.loremar.karrypdf.contents.PageObject
import marabillas.loremar.karrypdf.contents.text.TextElement
import marabillas.loremar.karrypdf.contents.text.TextObject
import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.objects.*

internal class FontDecoder(
    private val context: KarryPDFContext,
    private val pageObjects: ArrayList<PageObject>,
    private val fonts: HashMap<String, Font>
) {
    private val mainSB = StringBuilder()
    private val secondarySB = StringBuilder()

    fun decodeEncoded() {
        pageObjects
            .asSequence()
            .filter { it is TextObject }
            .forEach {
                val textObject = it as TextObject
                textObject.forEachIndexed forEachTextElement@{ i, e ->
                    val cmap = fonts[e.fontResource]?.cmap

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
                                } else if (p is Numeric) {
                                    mainSB.append(p.value.toFloat())
                                }
                            }
                            mainSB.append(']')
                            newTj = mainSB.toPDFArray(context, secondarySB.clear(), -1, 0)
                        }
                        is PDFString -> {
                            if (cmap != null) {
                                newTj = cmap.decodeString(e.tj.original).toPDFString()
                            } else {
                                newTj = e.tj
                            }
                        }
                    }

                    val updated =
                        TextElement(
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