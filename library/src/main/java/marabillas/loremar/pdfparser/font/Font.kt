package marabillas.loremar.pdfparser.font

import android.graphics.Typeface
import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.font.ttf.TTFParser
import marabillas.loremar.pdfparser.objects.*

internal class Font() {
    var typeface: Typeface = FontMappings[FontName.DEFAULT]
        private set
    var cmap: CMap? = null
        private set
    var widths = SparseArrayCompat<Float>()
        private set

    constructor(dictionary: Dictionary, referenceResolver: ReferenceResolver) : this() {
        dictionary.resolveReferences()
        val fontDescriptor = dictionary["FontDescriptor"]
        if (fontDescriptor is Dictionary) {
            fontDescriptor.resolveReferences()
        }

        // Get type of font
        val subtype = dictionary["Subtype"]
        if (subtype is Name) {
            println("Using ${subtype.value} font")
        }

        // Get typeface
        val baseFont = dictionary["BaseFont"]
        if (baseFont is Name) {
            typeface = FontIdentifier().identifyFont(baseFont.value)
            println("Basefont=${baseFont.value}")
        }

        // Get cmap
        val toUnicode = dictionary["ToUnicode"]
        if (toUnicode is Reference) {
            val stream = referenceResolver.resolveReferenceToStream(toUnicode)
            val b = stream?.decodeEncodedStream()
            b?.let {
                cmap = ToUnicodeCMap(String(b)).parse()
            }
        }

        // If a Simple Font
        if (subtype is Name && subtype.value != "Type0") {
            // Get FirstChar
            var firstChar = 0
            val fc = dictionary["FirstChar"]
            if (fc is Numeric) {
                firstChar = fc.value.toInt()
            }

            // Get MissingWidth
            var missingWidth: Float? = null
            if (fontDescriptor is Dictionary) {
                val mw = fontDescriptor["MissingWidth"]
                if (mw is Numeric) {
                    missingWidth = mw.value.toFloat()
                }
            }

            // Get widths
            val ws = dictionary["Widths"]
            if (ws is PDFArray && missingWidth != null) {
                // Assign missing character width
                widths.put(-1, missingWidth)

                for (i in 0 until ws.count()) {
                    widths.put(firstChar + i, (ws[i] as Numeric).value.toFloat())
                }
            } else {
                if (fontDescriptor is Dictionary) {
                    getWidthsFromFontProgram(fontDescriptor, referenceResolver)
                }
            }
        } else {
            // Else a Composite Font
            /*TODO(
                "For Type0(Composite) fonts, get CIDFont from DescendantFonts entry and get predefined or embedded" +
                        "cmap from Encoding entry. With CIDFont, get missingWidth from DW and widths array from W. W maps widths" +
                        "with CIDs. CID value for a character code is obtained from cmap. For Type2(TrueType) CIDFont, use " +
                        "CIDToGIDMap. If widths are not obtained from above, go to FontDescriptor and get widths from embedded font" +
                        "program."
            )*/
        }
    }

    private fun getWidthsFromFontProgram(fontDescriptor: Dictionary, referenceResolver: ReferenceResolver) {
        val fontFile = fontDescriptor["FontFile"]
        val fontFile2 = fontDescriptor["FontFile2"]
        val fontFile3 = fontDescriptor["FontFile3"]

        when {
            fontFile is Reference -> {
                TODO("Getting widths from fontFile not implemented")
            }
            fontFile2 is Reference -> {
                val fontProgram = referenceResolver.resolveReferenceToStream(fontFile2)
                val data = fontProgram?.decodeEncodedStream()
                if (data is ByteArray) {
                    widths = TTFParser(data).getCharacterWidths()
                }
            }
            fontFile3 is Stream -> {
                TODO("Getting widths from fontFile3 not implemented")
            }
        }
    }
}