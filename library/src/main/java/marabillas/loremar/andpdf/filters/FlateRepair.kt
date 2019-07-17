package marabillas.loremar.andpdf.filters

import marabillas.loremar.andpdf.objects.Dictionary

internal class FlateRepair(private val corruptedData: ByteArray, decodeParams: Dictionary?) {
    private val headerOffsets = mutableListOf<Int>()
    private val uncompressedResults = mutableListOf<ByteArray>()
    private val decoder = Flate(decodeParams, true)

    fun repair(): ByteArray {
        locateHeaders()

        tryDecompressWithEachHeader()

        return getLargestUncompressedResult()
    }

    private fun locateHeaders() {
        corruptedData.forEachIndexed { i, byte ->
            if (byte == 0x78.toByte() && i + 1 < corruptedData.size) {
                val second = corruptedData[i + 1]
                if (second == 0x01.toByte()
                    || second == 0x9c.toByte()
                    || second == 0xda.toByte()
                    || second == 0x5e.toByte()
                )
                    headerOffsets.add(i)
            }
        }
    }

    private fun tryDecompressWithEachHeader() {
        headerOffsets.forEach { offset ->
            val compressed = corruptedData.copyOfRange(offset, corruptedData.size)
            val uncompressed = decoder.decode(compressed)
            uncompressedResults.add(uncompressed)
        }
    }

    private fun getLargestUncompressedResult(): ByteArray {
        var largest = byteArrayOf()
        uncompressedResults.forEach { uncompressed ->
            if (uncompressed.size > largest.size)
                largest = uncompressed
        }
        return largest
    }
}