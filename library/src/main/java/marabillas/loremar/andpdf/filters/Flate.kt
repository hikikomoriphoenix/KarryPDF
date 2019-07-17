/*
This file is derived from https://github.com/apache/pdfbox/blob/trunk/pdfbox/src/main/java/org/apache/pdfbox/filter/FlateFilter.java
and written into Kotlin. The original work is open-source and licensed under Apache 2.0.
Original authors: Ben Litchfield, Marcel Kammer
 */
package marabillas.loremar.andpdf.filters

import marabillas.loremar.andpdf.exceptions.InvalidStreamException
import marabillas.loremar.andpdf.objects.Dictionary
import marabillas.loremar.andpdf.objects.Numeric
import marabillas.loremar.andpdf.utils.logd
import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Class for FlateDecode filter.
 * Decompresses data encoded using the zlib/deflate compression method,
 * reproducing the original text or binary data.
 *
 * In case this class does not work, try using BitmapFactory.decode() in android to create bitmap from given compressed
 * data.
 */
internal class Flate(private val decodeParams: Dictionary?, private val repairing: Boolean = false) : Decoder {
    private val predictor: Int = (decodeParams?.get("Predictor") as Numeric?)?.value?.toInt() ?: 1
    private val bitsPerComponent: Int = (decodeParams?.get("BitsPerComponent") as Numeric?)?.value?.toInt() ?: 8
    private val columns: Int = (decodeParams?.get("Columns") as Numeric?)?.value?.toInt() ?: 1
    private var colors: Int = Math.min((decodeParams?.get("Colors") as Numeric?)?.value?.toInt() ?: 1, 32)

    constructor() : this(null)

    override fun decode(encoded: ByteArray): ByteArray {
        val input = encoded.inputStream()
        val out = ByteArrayOutputStream()

        val predictedOut = Predictor(
            predictor = predictor,
            bitsPerComponent = bitsPerComponent,
            columns = columns,
            colors = colors,
            bytes = encoded
        ).wrapPredictor(out)

        // Start decompress

        val buf = ByteArray(2048)
        // skip zlib header
        input.read(buf, 0, 2)
        var read = input.read(buf)
        if (read > 0) {
            // use nowrap mode to bypass zlib-header and checksum to avoid a DataFormatException
            val inflater = Inflater(true)
            inflater.setInput(buf, 0, read)
            val res = ByteArray(1024)
            var dataWritten = false
            while (true) {
                var resRead: Int
                try {
                    resRead = inflater.inflate(res)
                } catch (exception: DataFormatException) {
                    if (dataWritten) {
                        // some data could be read -> don't throw an exception
                        logd("FlateFilter: premature end of stream due to a DataFormatException")
                        break
                    } else {
                        inflater.end()
                        return handleCorruptedStream(encoded)
                    }
                }

                if (resRead != 0) {
                    predictedOut.write(res, 0, resRead)
                    dataWritten = true
                    continue
                }
                if (inflater.finished() || inflater.needsDictionary() || input.available() == 0) {
                    break
                }
                read = input.read(buf)
                inflater.setInput(buf, 0, read)
            }
            inflater.end()
        }
        predictedOut.flush()

        return out.toByteArray()
    }

    private fun handleCorruptedStream(corrupted: ByteArray): ByteArray {
        if (repairing) return byteArrayOf()

        val repaired = FlateRepair(corrupted, decodeParams).repair()
        if (repaired.isNotEmpty()) {
            return decode(repaired)
        } else {
            throw InvalidStreamException("corrupted stream data using Flate filter", null)
        }
    }
}