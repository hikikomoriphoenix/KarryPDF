package marabillas.loremar.pdfparser.font

import marabillas.loremar.pdfparser.contents.PageObject
import marabillas.loremar.pdfparser.contents.TextElement
import marabillas.loremar.pdfparser.contents.TextObject
import marabillas.loremar.pdfparser.objects.*
import java.math.BigInteger

internal class FontDecoder(private val pageObjects: ArrayList<PageObject>, private val cmaps: HashMap<String, CMap>) {
    private val secondaryStringBuilder = StringBuilder()

    fun decodeEncoded() {
        pageObjects
            .asSequence()
            .filter { it is TextObject }
            .forEach {
                val textObject = it as TextObject
                textObject.forEachIndexed forEachTextElement@{ i, e ->
                    val f = e.tf.substringBefore(" ")
                    val cmap = cmaps[f]

                    var newTj: PDFObject? = null
                    when (e.tj) {
                        is PDFArray -> {
                            val sb = StringBuilder("[")
                            e.tj.forEach { p ->
                                if (p is PDFString) {
                                    if (cmap != null) {
                                        sb.append(
                                            cmap.decodeString(p.original)
                                        )
                                    } else {
                                        sb.append(
                                            p.original
                                        )
                                    }
                                } else {
                                    val n = p as Numeric
                                    sb.append(n.value.toFloat())
                                }
                            }
                            sb.append("]")
                            newTj = sb.toPDFArray(secondaryStringBuilder)
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
                        ts = e.ts
                    )

                    textObject.update(updated, i)
                }
            }
    }

    private fun convertLiteralToHex(s: PDFString): PDFString {
        val b = s.value.toByteArray()
        var hex = BigInteger(b).toString(16)
        //println("$hex ")
        if (hex.length % 2 != 0) hex = "0$hex"
        return "<$hex>".toPDFString()
    }
}