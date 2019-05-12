package marabillas.loremar.pdfparser.font

import android.graphics.Typeface
import marabillas.loremar.pdfparser.objects.*

internal class Font() {
    var typeface: Typeface = FontMappings[FontName.DEFAULT]
        private set
    var cmap: CMap? = null
        private set
    var widths: FloatArray? = null
        private set
    var firstChar: Int = 0
        private set
    var missingWidth: Float? = null
        private set

    constructor(dictionary: Dictionary, referenceResolver: ReferenceResolver) : this() {
        // Get typeface
        dictionary.resolveReferences()
        val baseFont = dictionary["BaseFont"]
        if (baseFont is Name) {
            typeface = FontIdentifier().identifyFont(baseFont.value)
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

        // Get widths
        val ws = dictionary["Widths"]
        if (ws is PDFArray) {
            widths = FloatArray(ws.count())
            ws.forEachIndexed { i, width ->
                widths?.set(i, (width as Numeric).value.toFloat())
            }
        }

        // Get FirstChar
        val fc = dictionary["FirstChar"]
        if (fc is Numeric) {
            firstChar = fc.value.toInt()
        }

        // Get MissingWidth
        val fontDescriptor = dictionary["FontDescriptor"]
        if (fontDescriptor is Dictionary) {
            val mw = fontDescriptor["MissingWidth"]
            if (mw is Numeric) {
                missingWidth = mw.value.toFloat()
            }
        }
    }
}