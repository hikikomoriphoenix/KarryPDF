package marabillas.loremar.andpdf.filters

import marabillas.loremar.andpdf.exceptions.InvalidStreamException
import marabillas.loremar.andpdf.objects.Dictionary
import marabillas.loremar.andpdf.objects.Numeric
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.min

/**
 * Class for LZWDecode filter.
 */
internal class LZW(decodeParams: Dictionary?) : Decoder {
    private val predictor: Int = (decodeParams?.get("Predictor") as Numeric?)?.value?.toInt() ?: 1
    private val bitsPerComponent: Int = (decodeParams?.get("BitsPerComponent") as Numeric?)?.value?.toInt() ?: 8
    private val columns: Int = (decodeParams?.get("Columns") as Numeric?)?.value?.toInt() ?: 1
    private var earlyChange: Int = (decodeParams?.get("EarlyChange") as Numeric?)?.value?.toInt() ?: 1
    private var colors: Int = min((decodeParams?.get("Colors") as Numeric?)?.value?.toInt() ?: 1, 32)

    private val codeTable = HashMap<BitArray, ByteArray>()
    private val clearTableMarker = bitArrayOf(true, false, false, false, false, false, false, false, false)
    private val endOfDataMarker = bitArrayOf(true, false, false, false, false, false, false, false, true)
    private var ptr = 0
    private var maxSize = 9
    private var chunkSize = 9
    private var codeValue: ByteArray? = null
    private var prevValue: ByteArray? = null
    private var prevCode = endOfDataMarker

    init {
        if (earlyChange != 0 && earlyChange != 1) {
            earlyChange = 1
        }
    }

    override fun decode(encoded: ByteArray): ByteArray {
        val bitArray = encoded.toBitArray()
        val out = ByteArrayOutputStream()

        val predictedOut = Predictor(
            predictor = predictor,
            bitsPerComponent = bitsPerComponent,
            columns = columns,
            colors = colors,
            bytes = encoded
        ).wrapPredictor(out)

        resetCodeTable()
        ptr = 0
        maxSize = 9
        prevCode = endOfDataMarker
        codeValue = null
        prevValue = null
        while (true) {
            evaluateForCodeLengthChange()
            if (ptr + chunkSize > bitArray.size()) break

            val code = bitArray.subArray(ptr, ptr + chunkSize)

            if (code == endOfDataMarker) {
                break
            } else if (code == clearTableMarker) {
                resetCodeTable()
                maxSize = 9
                chunkSize = 9
                ptr += chunkSize
                prevCode = endOfDataMarker
                prevValue = null
            } else {
                decode(code, predictedOut)
            }
        }

        return out.toByteArray()
    }

    private fun decode(code: BitArray, predictedOut: OutputStream) {
        codeValue = codeTable[code]
        if (codeValue != null) {
            prevValue?.let { pv ->
                val newValue = ByteArray(pv.size + 1)
                pv.forEachIndexed { i, byte ->
                    if (byte.toChar() != '\r') {
                        newValue[i] = byte
                    } else {
                        newValue[i] = '\n'.toByte()
                    }
                }
                if (codeValue!!.first().toChar() == '\r') {
                    codeValue!![0] = '\n'.toByte()
                }
                newValue[newValue.lastIndex] = codeValue!!.first()
                codeTable[prevCode.next()] = newValue
                prevCode = prevCode.next()
            }
        } else if (prevCode.next() == code && prevValue != null) {
            val newValue = ByteArray(prevValue!!.size + 1)
            prevValue!!.forEachIndexed { i, byte ->
                newValue[i] = byte
            }
            newValue[newValue.lastIndex] = prevValue!!.first()
            codeTable[code] = newValue
            codeValue = newValue
            prevCode = code
        } else if (chunkSize > 9 && !code[0]) {
            ptr++
            chunkSize--
            return
        } else {
            throw InvalidStreamException("Exception on LZW decoding: Cant find code in table", null)
        }

        codeValue?.forEach { byte ->
            var c = byte.toChar()
            if (c == '\r') {
                c = '\n'
            }
            predictedOut.write(c.toInt() and 0xff)
        }

        ptr += chunkSize
        prevValue = codeValue
        chunkSize = maxSize
    }

    private fun resetCodeTable() {
        codeTable.clear()
        for (i in 0..255) {
            val byte = i.toByte()
            val bitArray = byte.toBitArray()
            codeTable[bitArray] = byteArrayOf(byte)
        }

        codeTable[clearTableMarker] = byteArrayOf()
        codeTable[endOfDataMarker] = byteArrayOf()
    }

    private fun evaluateForCodeLengthChange() {
        if (earlyChange == 1) {
            if (prevCode.next().next().size() > maxSize) {
                maxSize++
                chunkSize = maxSize
            }
        } else {
            if (prevCode.next().size() > maxSize) {
                maxSize++
                chunkSize = maxSize
            }
        }
    }
}