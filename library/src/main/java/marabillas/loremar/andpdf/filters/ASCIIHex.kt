package marabillas.loremar.andpdf.filters

import java.math.BigInteger

/**
 * Class for ASCIIHexDecode filter.
 */
internal class ASCIIHex : Decoder {
    override fun decode(encoded: ByteArray): ByteArray {
        if (encoded.size % 2 != 0) throw IllegalArgumentException("Hex string to decode is not divisible by 2")

        val bytes = ByteArray(encoded.size / 2)
        var i = 0
        while (i + 1 < encoded.size) {
            if (encoded[i].toChar() == ' ' || encoded[i].toChar() == '\n' || encoded[i].toChar() == '\r') {
                i++
                continue
            }

            val c1 = encoded[i++].toChar().toLowerCase()
            val c2 = encoded[i++].toChar().toLowerCase()
            bytes[(i - 2) / 2] = hexToByteMap[c1]?.get(c2) ?: 0.toByte()
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
        val hexToByteMap = HashMap<Char, HashMap<Char, Byte>>()

        init {
            var i = 0
            for (c1 in '0'..'9') {
                val secondaryMap = HashMap<Char, Byte>()
                for (c2 in '0'..'9') {
                    secondaryMap[c2] = i++.toByte()
                }
                for (c2 in 'a'..'f') {
                    secondaryMap[c2] = i++.toByte()
                }
                hexToByteMap[c1] = secondaryMap
            }
            for (c1 in 'a'..'f') {
                val secondaryMap = HashMap<Char, Byte>()
                for (c2 in '0'..'9') {
                    secondaryMap[c2] = i++.toByte()
                }
                for (c2 in 'a'..'f') {
                    secondaryMap[c2] = i++.toByte()
                }
                hexToByteMap[c1] = secondaryMap
            }
        }
    }
}