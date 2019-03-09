package marabillas.loremar.pdfparser.filters

import java.io.ByteArrayOutputStream

/**
 * Class for RunLengthDecode filter.
 * This class is derived from  PDFBox's RunLengthDecodeFilter class and written into Kotlin. The original work is
 * licensed under Apache 2.0.
 * Original Author: Ben Litchfield
 *
 * Decompresses data encoded using a byte-oriented run-length encoding algorithm,
 * reproducing the original text or binary data
 */
internal class RunLength : Decoder {
    private val runLengthEOD = 128

    override fun decode(encoded: ByteArray): ByteArray {
        val input = encoded.inputStream()
        val out = ByteArrayOutputStream()

        var dupAmount: Int
        val buffer = ByteArray(128)
        while (true) {
            dupAmount = input.read()
            if (dupAmount == -1 || dupAmount == runLengthEOD) break

            if (dupAmount <= 127) {
                var amountToCopy = dupAmount + 1
                var compressedRead: Int
                while (amountToCopy > 0) {
                    compressedRead = input.read(buffer, 0, amountToCopy)
                    // EOF reached?
                    if (compressedRead == -1) {
                        break
                    }
                    out.write(buffer, 0, compressedRead)
                    amountToCopy -= compressedRead
                }
            } else {
                val dupByte = input.read()
                // EOF reached?
                if (dupByte == -1) {
                    break
                }
                for (i in 0 until 257 - dupAmount) {
                    out.write(dupByte)
                }
            }
        }

        return out.toByteArray()
    }
}