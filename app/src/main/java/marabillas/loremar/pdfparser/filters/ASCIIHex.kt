package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.CaseInsensitiveMap
import java.math.BigInteger

/**
 * Class for ASCIIHexDecode filter.
 */
class ASCIIHex : Decoder {
    override fun decode(encoded: String): ByteArray {
        if (encoded.length % 2 != 0) throw IllegalArgumentException()

        val bytes = ByteArray(encoded.length / 2)
        var i = 0
        while (i < encoded.length - 1) {
            if (encoded[i] == ' ') {
                ++i
                continue
            }

            val pair = encoded.substring(i++, i++ + 1)
            bytes[(i - 2) / 2] = hexMap[pair]!!
        }

        return bytes
    }

    fun decodeToString(encoded: String): String {
        return String(decode(encoded))
    }

    fun decodeToBigInteger(encoded: String): BigInteger {
        return BigInteger(decode(encoded))
    }

    companion object {
        /**
         * A map that binds pairs of hexadecimals to their decimal values. Keys are  case-insensitive.
         */
        var hexMap = CaseInsensitiveMap<Byte>()
            private set

        init {
            val keys = arrayOfNulls<String>(256)
            var index = 0

            for (i in '0'..'9') {
                index = iter2ndDigit(i, keys, index)
            }

            for (i in 'A'..'F') {
                index = iter2ndDigit(i, keys, index)
            }

            val values = ByteArray(256) { it.toByte() }
            keys.mapIndexed { i, s ->
                hexMap.put(s.toString(), values[i])
            }
        }

        private fun iter2ndDigit(i: Char, keys: Array<String?>, start: Int): Int {
            var m = start
            for (j in '0'..'9') {
                keys[m++] = "$i$j"
            }
            for (j in 'A'..'F') {
                keys[m++] = "$i$j"
            }
            return m
        }
    }


}