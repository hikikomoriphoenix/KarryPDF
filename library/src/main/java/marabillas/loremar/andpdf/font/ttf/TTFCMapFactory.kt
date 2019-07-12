package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.exceptions.font.InvalidTTFCMapException
import marabillas.loremar.andpdf.utils.loge

internal class TTFCMapFactory {
    fun getTTFCMap(format: Int, data: ByteArray, pos: Long): TTFCMap? {
        return try {
            when (format) {
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
        } catch (e: InvalidTTFCMapException) {
            loge("Invalid TTF", null)
            return null
        } catch (e: Exception) {
            loge("Invalid TTF", e)
            return null
        }
    }
}