package marabillas.loremar.karrypdf.font.cmap

internal interface CIDFontCMap : CMap {
    fun cidToUnicode(cid: Int): Int
}