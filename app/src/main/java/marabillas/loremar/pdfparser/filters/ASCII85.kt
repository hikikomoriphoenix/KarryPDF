package marabillas.loremar.pdfparser.filters

import java.io.ByteArrayOutputStream

/**
 * Class for ASCII85Decode filter.
 */
class ASCII85 : Decoder {
    override fun decode(encoded: String): ByteArray {
        val s = encoded.substringAfter("<~").substringBefore("~>")
        val out = ByteArrayOutputStream()
        var i: Long = 0
        var j = 0
        s.takeWhile { it != '~' }.forEach {
            if (it == 'z' && j == 0) {
                repeat(4) { out.write(0) }
            } else if (it == 'y' && j == 0) {
                repeat(4) { out.write(' '.toInt()) }
            } else if (it == 'x' && j == 0) {
                repeat(4) { out.write(-1) }
            } else if (it in '!'..'u') {
                i = i * 85L + (it - '!').toLong()
                j++
                if (j >= 5) {
                    out.write((i shr 24).toInt())
                    out.write((i shr 16).toInt())
                    out.write((i shr 8).toInt())
                    out.write((i).toInt())
                    i = 0; j = 0
                }
            }
        }

        when (j) {
            4 -> {
                i = i * 85L + 84L
                out.write((i shr 24).toInt())
                out.write((i shr 16).toInt())
                out.write((i shr 8).toInt())
            }
            3 -> {
                i = i * 85L + 84L
                i = i * 85L + 84L
                out.write((i shr 24).toInt())
                out.write((i shr 16).toInt())
            }
            2 -> {
                i = i * 85L + 84L
                i = i * 85L + 84L
                i = i * 85L + 84L
                out.write((i shr 24).toInt())
            }
        }

        return out.toByteArray()
    }
}