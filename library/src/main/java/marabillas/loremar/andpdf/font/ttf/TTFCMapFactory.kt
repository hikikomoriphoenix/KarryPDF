package marabillas.loremar.andpdf.font.ttf

internal class TTFCMapFactory {
    fun getTTFCMap(format: Int, data: ByteArray, pos: Long): TTFCMap? {
        return when (format) {
            0 -> TTFCMap0(data, pos)
            2 -> TTFCMap2(data, pos)
            4 -> TTFCMap4(data, pos)
            6 -> TTFCMap6(data, pos)
            8 -> TTFCMap8(data, pos)
            10 -> TTFCMap10(data, pos)
            12 -> TTFCMap12(data, pos)
            14 -> TTFCMap14(data, pos)
            else -> null
        }
    }
}