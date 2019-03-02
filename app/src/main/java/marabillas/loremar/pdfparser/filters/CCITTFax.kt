package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.Dictionary
import java.io.IOException
import kotlin.experimental.and
import kotlin.experimental.inv

class CCITTFax(decodeParams: Dictionary?, private val height: Int) : Decoder {
    private val k: Int = decodeParams?.entries?.get("K")?.toInt() ?: 0
    private val encodedByteAlign: Boolean = decodeParams?.entries?.get("EncodedByteAlign") == "true"
    private val cols: Int = decodeParams?.entries?.get("Columns")?.toInt() ?: 1728
    private var rows: Int = decodeParams?.entries?.get("Rows")?.toInt() ?: 0
    private val blackIsOne: Boolean = decodeParams?.entries?.get("BlackIs1") == "true"

    override fun decode(encoded: String): ByteArray {
        rows = if (rows > 0 && height > 0) {
            // PDFBOX-771, PDFBOX-3727: rows in DecodeParms sometimes contains an incorrect value
            height
        } else {
            // at least one of the values has to have a valid value
            Math.max(rows, height)
        }

        // Decompress data.
        val arraySize = (cols + 7) / 8 * rows
        // TODO possible options??
        val decompressed = ByteArray(arraySize)
        val s: CCITTFaxDecoderStream
        val type: Int
        var tiffOptions: Long
        if (k == 0) {
            tiffOptions = (if (encodedByteAlign) TIFFExtension.GROUP3OPT_BYTEALIGNED else 0).toLong()
            type = TIFFExtension.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE
        } else {
            if (k > 0) {
                tiffOptions = (if (encodedByteAlign) TIFFExtension.GROUP3OPT_BYTEALIGNED else 0).toLong()
                tiffOptions = tiffOptions or TIFFExtension.GROUP3OPT_2DENCODING.toLong()
                type = TIFFExtension.COMPRESSION_CCITT_T4
            } else {
                // k < 0
                tiffOptions = (if (encodedByteAlign) TIFFExtension.GROUP4OPT_BYTEALIGNED else 0).toLong()
                type = TIFFExtension.COMPRESSION_CCITT_T6
            }
        }
        s = CCITTFaxDecoderStream(encoded.byteInputStream(), cols, type, TIFFExtension.FILL_LEFT_TO_RIGHT, tiffOptions)
        readFromDecoderStream(s, decompressed)

        // invert bitmap
        if (!blackIsOne) {
            // Inverting the bitmap
            // Note the previous approach with starting from an IndexColorModel didn't work
            // reliably. In some cases the image wouldn't be painted for some reason.
            // So a safe but slower approach was taken.
            invertBitmap(decompressed)
        }

        return decompressed
    }

    @Throws(IOException::class)
    internal fun readFromDecoderStream(decoderStream: CCITTFaxDecoderStream, result: ByteArray) {
        var pos = 0
        var read: Int

        while (true) {
            read = decoderStream.read(result, pos, result.size - pos)
            if (read <= -1) break
            pos += read
            if (pos >= result.size) {
                break
            }
        }
        decoderStream.close()
    }


    private fun invertBitmap(bufferData: ByteArray) {
        var i = 0
        val c = bufferData.size
        while (i < c) {
            bufferData[i] = (bufferData[i].inv() and 0xFF.toByte())
            i++
        }
    }
}