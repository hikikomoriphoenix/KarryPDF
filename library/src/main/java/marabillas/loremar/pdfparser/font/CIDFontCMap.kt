package marabillas.loremar.pdfparser.font

internal interface CIDFontCMap : CMap {
    fun cidToUnicode(cid: Int): Int
}