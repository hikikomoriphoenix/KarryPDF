package marabillas.loremar.pdfparser.font

import marabillas.loremar.pdfparser.contents.PageObject
import marabillas.loremar.pdfparser.contents.TextElement
import marabillas.loremar.pdfparser.contents.TextObject
import marabillas.loremar.pdfparser.objects.*
import java.math.BigInteger

internal class FontDecoder(private val pageObjects: ArrayList<PageObject>, private val cmaps: HashMap<String, CMap>) {
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
                                    val s = decodeString(p, cmap)
                                    sb.append(s.original)
                                } else {
                                    val n = p as Numeric
                                    sb.append(n.value.toFloat())
                                }
                            }
                            sb.append("]")
                            newTj = PDFArray(sb.toString()).parse()
                        }
                        is PDFString -> {
                            newTj = decodeString(e.tj, cmap)
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

    private fun decodeString(s: PDFString, cmap: CMap?): PDFString {
        cmap?.let {
            var pdfS = s
            if (pdfS.original.startsWith("(") && pdfS.original.endsWith(")"))
                pdfS = convertLiteralToHex(pdfS)
            val encoded = pdfS.original.removeSurrounding("<", ">")
            val decoded = it.decodeString(encoded)
            return "($decoded)".toPDFString()
        }
        return s
    }

    private fun convertLiteralToHex(s: PDFString): PDFString {
        val b = s.value.toByteArray()
        var hex = BigInteger(b).toString(16)
        //println("$hex ")
        if (hex.length % 2 != 0) hex = "0$hex"
        return "<$hex>".toPDFString()
    }
}