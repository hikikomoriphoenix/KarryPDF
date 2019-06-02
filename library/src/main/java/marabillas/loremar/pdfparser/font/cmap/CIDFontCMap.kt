package marabillas.loremar.pdfparser.font.cmap

internal interface CIDFontCMap : CMap {
    fun cidToUnicode(cid: Int): Int
}