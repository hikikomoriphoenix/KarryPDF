package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.exceptions.UnsupportedPDFElementException

internal class TTFCMap2(data: ByteArray, pos: Long) : TTFCMapDefault() {
    init {
        throw UnsupportedPDFElementException(
            "TrueType fonts with Chinese, Korean, or Japanese characters may not be " +
                    "supported yet"
        )
    }
}