package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.CaseInsensitiveMap
import java.math.BigInteger

/**
 * Class for ASCIIHexDecode filter.
 */
internal class ASCIIHex : Decoder {
    override fun decode(encoded: ByteArray): ByteArray {
        // TODO Change to a more GC-friendly implementation getting rid of substring use

        val s = String(encoded, Charsets.US_ASCII)
        if (s.length % 2 != 0) throw IllegalArgumentException()

        val bytes = ByteArray(s.length / 2)
        var i = 0
        while (i < s.length - 1) {
            if (s[i] == ' ') {
                ++i
                continue
            }

            val pair = s.substring(i++, i++ + 1)
            bytes[(i - 2) / 2] = hexMap[pair]!!
        }

        return bytes
    }

    fun decodeToString(encoded: ByteArray): String {
        return String(decode(encoded), Charsets.US_ASCII)
    }

    fun decodeToBigInteger(encoded: ByteArray): BigInteger {
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