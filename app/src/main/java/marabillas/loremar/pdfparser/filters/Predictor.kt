/*
This file is derived from https://github.com/apache/pdfbox/blob/trunk/pdfbox/src/main/java/org/apache/pdfbox/filter/Predictor.java
and written into Kotlin. The original work is open-source and licensed under Apache 2.0.
 */

package marabillas.loremar.pdfparser.filters

import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

/**
 * Helper class to contain predictor decoding used by Flate and LZW filter.
 * To see the history, look at the FlateFilter class.
 */
class Predictor(private val predictor: Int, val bitsPerComponent: Int, val columns: Int, val colors: Int,
                val bytes: ByteArray){
    fun calculateRowLength(colors: Int, bitsPerComponent: Int, columns: Int): Int {
        val bitsPerPixel = colors * bitsPerComponent
        return (columns * bitsPerPixel + 7) / 8
    }

    // get value from bit interval from a byte
    fun getBitSeq(by: Int, startBit: Int, bitSize: Int): Int {
        val mask = (1 shl bitSize) - 1
        return by.ushr(startBit) and mask
    }

    // set value in a bit interval and return that value
    fun calcSetBitSeq(by: Int, startBit: Int, bitSize: Int, `val`: Int): Int {
        var mask = (1 shl bitSize) - 1
        val truncatedVal = `val` and mask
        mask = (mask shl startBit).inv()
        return by and mask or (truncatedVal shl startBit)
    }

    /**
     * Wraps and `OutputStream` in a predictor decoding stream as necessary.
     * If no predictor is specified by the parameters, the original stream is returned as is.
     *
     * @param out The stream to which decoded data should be written
     * @return An `OutputStream` is returned, which will write decoded data
     * into the given stream. If no predictor is specified, the original stream is returned.
     */
    fun wrapPredictor(out: OutputStream): OutputStream {
        return if (predictor > 1) {
            PredictorOutputStream(out, predictor, colors, bitsPerComponent, columns)
        } else {
            out
        }
    }

    /**
     * Output stream that implements predictor decoding. Data is buffered until a complete
     * row is available, which is then decoded and written to the underlying stream.
     * The previous row is retained for decoding the next row.
     */
    private inner class PredictorOutputStream internal constructor(
        out: OutputStream,
        // current predictor type
        private var predictor: Int,
        // image decode parameters
        private val colors: Int,
        private val bitsPerComponent: Int,
        private val columns: Int
    ) : FilterOutputStream(out) {
        private val rowLength: Int = calculateRowLength(colors, bitsPerComponent, columns)
        // PNG predictor (predictor>=10) means every row has a (potentially different)
        // predictor value
        private val predictorPerRow: Boolean = predictor >= 10

        // data buffers
        private var currentRow = ByteArray(rowLength)
        private var lastRow = ByteArray(rowLength)
        // amount of data in the current row
        private var currentRowData = 0
        // was the per-row predictor value read for the current row being processed
        private var predictorRead = false

        override fun write(b: ByteArray?) {
            write(bytes, 0, bytes.size)
        }

        override fun write(bytes: ByteArray, off: Int, len: Int) {
            var currentOffset = off
            val maxOffset = currentOffset + len
            while (currentOffset < maxOffset) {
                if (predictorPerRow && currentRowData == 0 && !predictorRead) {
                    // PNG predictor; each row starts with predictor type (0, 1, 2, 3, 4)
                    // read per line predictor, add 10 to tread value 0 as 10, 1 as 11, ...
                    predictor = bytes[currentOffset] + 10
                    currentOffset++
                    predictorRead = true
                } else {
                    val toRead = Math.min(rowLength - currentRowData, maxOffset - currentOffset)
                    System.arraycopy(bytes, currentOffset, currentRow, currentRowData, toRead)
                    currentRowData += toRead
                    currentOffset += toRead

                    // current row is filled, decode it, write it to underlying stream,
                    // and reset the state.
                    if (currentRowData == currentRow.size) {
                        decodeAndWriteRow()
                    }
                }
            }
        }

        @Throws(IOException::class)
        private fun decodeAndWriteRow() {
            decodePredictorRow(predictor, colors, bitsPerComponent, columns, currentRow, lastRow)
            out.write(currentRow)
            flipRows()
        }

        /**
         * Flips the row buffers (to avoid copying), and resets the current-row index
         * and predictorRead flag
         */
        private fun flipRows() {
            val temp = lastRow
            lastRow = currentRow
            currentRow = temp
            currentRowData = 0
            predictorRead = false
        }

        @Throws(IOException::class)
        override fun flush() {
            // The last row is allowed to be incomplete, and should be completed with zeros.
            if (currentRowData > 0) {
                Arrays.fill(currentRow, currentRowData, rowLength, 0.toByte())
                decodeAndWriteRow()
            }
            super.flush()
        }

        @Throws(IOException::class)
        override fun write(i: Int) {
            throw UnsupportedOperationException("Not supported")
        }
    }

    /**
     * Decodes a single line of data in-place.
     * @param predictor Predictor value for the current line
     * @param colors Number of color components, from decode parameters.
     * @param bitsPerComponent Number of bits per components, from decode parameters.
     * @param columns Number samples in a row, from decode parameters.
     * @param actline Current (active) line to decode. Data will be decoded in-place,
     * i.e. - the contents of this buffer will be modified.
     * @param lastline The previous decoded line. When decoding the first line, this
     * parameter should be an empty byte array of the same length as
     * `actline`.
     */
    fun decodePredictorRow(
        predictor: Int,
        colors: Int,
        bitsPerComponent: Int,
        columns: Int,
        actline: ByteArray,
        lastline: ByteArray
    ) {
        if (predictor == 1) {
            // no prediction
            return
        }
        val bitsPerPixel = colors * bitsPerComponent
        val bytesPerPixel = (bitsPerPixel + 7) / 8
        val rowlength = actline.size
        when (predictor) {
            2 -> run when2@{
                // PRED TIFF SUB
                if (bitsPerComponent == 8) {
                    // for 8 bits per component it is the same algorithm as PRED SUB of PNG format
                    for (p in bytesPerPixel until rowlength) {
                        val sub = actline[p].toInt() and 0xff
                        val left = actline[p - bytesPerPixel].toInt() and 0xff
                        actline[p] = (sub + left).toByte()
                    }
                    return@when2
                }
                if (bitsPerComponent == 16) {
                    var p = bytesPerPixel
                    while (p < rowlength) {
                        val sub = (actline[p].toInt() and 0xff shl 8) + (actline[p + 1].toInt() and 0xff)
                        val left =
                            (actline[p - bytesPerPixel].toInt() and 0xff shl 8) + (actline[p - bytesPerPixel + 1].toInt()
                                    and 0xff)
                        actline[p] = (sub + left shr 8 and 0xff).toByte()
                        actline[p + 1] = (sub + left and 0xff).toByte()
                        p += 2
                    }
                    return@when2
                }
                if (bitsPerComponent == 1 && colors == 1) {
                    // bytesPerPixel cannot be used:
                    // "A row shall occupy a whole number of bytes, rounded up if necessary.
                    // Samples and their components shall be packed into bytes
                    // from high-order to low-order bits."
                    for (p in 0 until rowlength) {
                        for (bit in 7 downTo 0) {
                            val sub = actline[p].toInt() shr bit and 1
                            if (p == 0 && bit == 7) {
                                continue
                            }
                            val left: Int = if (bit == 7) {
                                // use bit #0 from previous byte
                                actline[p - 1].toInt() and 1
                            } else {
                                // use "previous" bit
                                actline[p].toInt() shr bit + 1 and 1
                            }
                            if (sub + left and 1 == 0) {
                                // reset bit
                                actline[p] = (actline[p].toInt() and (1 shl bit).inv()).toByte()
                            } else {
                                // set bit
                                actline[p] = (actline[p].toInt() or (1 shl bit)).toByte()
                            }
                        }
                    }
                    return@when2
                }
                // everything else, i.e. bpc 2 and 4, but has been tested for bpc 1 and 8 too
                val elements = columns * colors
                for (p in colors until elements) {
                    val bytePosSub = p * bitsPerComponent / 8
                    val bitPosSub = 8 - p * bitsPerComponent % 8 - bitsPerComponent
                    val bytePosLeft = (p - colors) * bitsPerComponent / 8
                    val bitPosLeft = 8 - (p - colors) * bitsPerComponent % 8 - bitsPerComponent

                    val sub = getBitSeq(actline[bytePosSub].toInt(), bitPosSub, bitsPerComponent)
                    val left = getBitSeq(actline[bytePosLeft].toInt(), bitPosLeft, bitsPerComponent)
                    actline[bytePosSub] =
                        calcSetBitSeq(actline[bytePosSub].toInt(), bitPosSub, bitsPerComponent, sub + left).toByte()
                }
            }
            10 -> {
            }
            11 ->
                // PRED SUB
                for (p in bytesPerPixel until rowlength) {
                    val sub = actline[p].toInt()
                    val left = actline[p - bytesPerPixel].toInt()
                    actline[p] = (sub + left).toByte()
                }
            12 ->
                // PRED UP
                for (p in 0 until rowlength) {
                    val up = actline[p].toInt() and 0xff
                    val prior = lastline[p].toInt() and 0xff
                    actline[p] = (up + prior and 0xff).toByte()
                }
            13 ->
                // PRED AVG
                for (p in 0 until rowlength) {
                    val avg = actline[p].toInt() and 0xff
                    val left = if (p - bytesPerPixel >= 0) actline[p - bytesPerPixel].toInt() and 0xff else 0
                    val up = lastline[p].toInt() and 0xff
                    actline[p] = (avg + (left + up) / 2 and 0xff).toByte()
                }
            14 ->
                // PRED PAETH
                for (p in 0 until rowlength) {
                    val paeth = actline[p].toInt() and 0xff
                    val a = if (p - bytesPerPixel >= 0) actline[p - bytesPerPixel].toInt() and 0xff else 0// left
                    val b = lastline[p].toInt() and 0xff// upper
                    val c = if (p - bytesPerPixel >= 0) lastline[p - bytesPerPixel].toInt() and 0xff else 0// upperleft
                    val value = a + b - c
                    val absa = Math.abs(value - a)
                    val absb = Math.abs(value - b)
                    val absc = Math.abs(value - c)

                    if (absa <= absb && absa <= absc) {
                        actline[p] = (paeth + a and 0xff).toByte()
                    } else if (absb <= absc) {
                        actline[p] = (paeth + b and 0xff).toByte()
                    } else {
                        actline[p] = (paeth + c and 0xff).toByte()
                    }
                }
            else -> {
            }
        }// PRED NONE
        // do nothing
    }
}