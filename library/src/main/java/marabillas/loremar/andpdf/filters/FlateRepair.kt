package marabillas.loremar.andpdf.filters

import java.util.zip.DataFormatException
import java.util.zip.Inflater

class FlateRepair(private val corruptedData: ByteArray) {
    private var pointer = 2

    private val dstBuffer = ByteArray(1024)
    private var repairedData = byteArrayOf()
    private val inflater = Inflater(true)

    fun repair(): ByteArray {
        while (pointer < corruptedData.size - 4) {
            val len = dstLength()
            setInputToInflater(len)

            val success = inflate()
            if (success) {
                repairedData = corruptedData.copyOfRange(pointer, corruptedData.size)
                break
            }

            inflater.reset()
            pointer++
        }
        inflater.end()

        return repairedData
    }

    private fun dstLength(): Int {
        var len = 2048
        if (corruptedData.size - pointer < len)
            len = corruptedData.size - pointer

        return len
    }

    private fun setInputToInflater(len: Int) {
        inflater.setInput(corruptedData, pointer, len)
    }

    private fun inflate(): Boolean {
        return try {
            val read = inflater.inflate(dstBuffer)
            read > 0
        } catch (e: DataFormatException) {
            false
        }
    }
}