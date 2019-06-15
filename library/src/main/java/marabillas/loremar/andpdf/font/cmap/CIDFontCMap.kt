package marabillas.loremar.andpdf.font.cmap

internal interface CIDFontCMap : CMap {
    fun cidToUnicode(cid: Int): Int
}