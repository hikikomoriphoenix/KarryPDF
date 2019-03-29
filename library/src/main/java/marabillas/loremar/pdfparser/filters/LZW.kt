package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.objects.Dictionary
import marabillas.loremar.pdfparser.objects.Numeric
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Class for LZWDecode filter.
 * In case this class does not work, try using BitmapFactory.decode() in android to create bitmap from given compressed
 * data.
 */
internal class LZW(decodeParams: Dictionary?) : Decoder {
    private val predictor: Int = (decodeParams?.get("Predictor") as Numeric?)?.value?.toInt() ?: 1
    private val bitsPerComponent: Int = (decodeParams?.get("BitsPerComponent") as Numeric?)?.value?.toInt() ?: 8
    private val columns: Int = (decodeParams?.get("Columns") as Numeric?)?.value?.toInt() ?: 1
    private var earlyChange: Int = (decodeParams?.get("EarlyChange") as Numeric?)?.value?.toInt() ?: 1
    private var colors: Int = Math.min((decodeParams?.get("Colors") as Numeric?)?.value?.toInt() ?: 1, 32)

    private var lzwTable = ArrayList<ByteArray>()

    constructor() : this(null)

    companion object {
        private const val CLEAR_TABLE = 256L
        private const val EOD = 257L
    }

    init {
        if (earlyChange != 0 && earlyChange != 1) {
            earlyChange = 1
        }
    }

    override fun decode(encoded: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val inputStream = encoded.inputStream()
        val input = MemoryCacheImageInputStream(inputStream)

        val predictedOut = Predictor(
            predictor = predictor,
            bitsPerComponent = bitsPerComponent,
            columns = columns,
            colors = colors,
            bytes = encoded
        ).wrapPredictor(out)

        var size = 8
        var next: Long
        var prev = -1L
        createLZWTable()
        while (true) {
            next = input.readBits(size)
            if (next == EOD) break
            when (next) {
                CLEAR_TABLE -> {
                    size = 9
                    createLZWTable()
                    prev = -1L
                }
                else -> {
                    when {
                        next < lzwTable.size -> {
                            var bytes = lzwTable[next.toInt()]
                            val first = bytes[0]
                            predictedOut.write(bytes)
                            if (prev != -1L) {
                                checkIndexBounds(prev, input)
                                bytes = lzwTable[prev.toInt()]
                                val newBytes = bytes.copyOf(bytes.size + 1)
                                newBytes[bytes.size] = first
                                lzwTable.add(newBytes)
                            }
                        }
                        else -> {
                            if (prev != -1L) {
                                checkIndexBounds(prev, input)
                                val bytes = lzwTable[prev.toInt()]
                                val newBytes = bytes.copyOf(bytes.size + 1)
                                newBytes[bytes.size] = bytes[0]
                                predictedOut.write(newBytes)
                                lzwTable.add(newBytes)
                            }
                        }
                    }

                    size = calculateNextCodeSize()
                    prev = next
                }
            }
        }

        return out.toByteArray()
    }

    @Throws(IOException::class)
    private fun checkIndexBounds(index: Long, input: MemoryCacheImageInputStream) {
        if (index < 0) {
            throw IOException(
                "negative array index: " + index + " near offset "
                        + input.streamPosition
            )
        }
        if (index >= lzwTable.size) {
            throw IOException(
                ("array index overflow: " + index +
                        " >= " + lzwTable.size + " near offset "
                        + input.streamPosition)
            )
        }
    }

    private fun createLZWTable() {
        lzwTable = ArrayList(4096)
        for (i in 0 until 256) {
            val byte = (i and 0xFF).toByte()
            val array = arrayOf(byte)
            lzwTable.add(array.toByteArray())
        }

        repeat(2) { lzwTable.add(ByteArray(0)) }
    }

    private fun calculateNextCodeSize(): Int {
        return when {
            lzwTable.size >= 2048 - earlyChange -> 12
            lzwTable.size >= 1024 - earlyChange -> 11
            lzwTable.size >= 512 - earlyChange -> 10
            else -> 8
        }
    }
}