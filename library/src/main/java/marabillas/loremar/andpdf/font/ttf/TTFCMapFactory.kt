package marabillas.loremar.andpdf.font.ttf

import android.util.Log
import marabillas.loremar.andpdf.exceptions.font.InvalidTTFCMapException

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
            Log.e("${javaClass.name}.getTTFCMap", e.message)
            return null
        } catch (e: Exception) {
            Log.e(javaClass.name, "${e.javaClass.name}: ${e.message}")
            val elements = e.stackTrace
            for (element in elements) {
                Log.e(javaClass.name, "$element")
            }
            return null
        }
    }
}