package marabillas.loremar.pdfparser.font.ttf

import marabillas.loremar.pdfparser.exceptions.UnsupportedPDFElementException

internal class TTFCMap2(data: ByteArray, pos: Long) : TTFCMapDefault() {
    init {
        throw UnsupportedPDFElementException(
            "TrueType fonts with Chinese, Korean, or Japanese characters may not be " +
                    "supported yet"
        )
    }
}