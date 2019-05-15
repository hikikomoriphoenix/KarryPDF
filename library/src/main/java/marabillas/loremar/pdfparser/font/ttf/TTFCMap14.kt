package marabillas.loremar.pdfparser.font.ttf

import android.support.v4.util.SparseArrayCompat

internal class TTFCMap14(val data: ByteArray, val pos: Long) : TTFCMap {
    override fun getCharacterWidths(glyphWidths: IntArray): SparseArrayCompat<Float> {
        val characterWidths = SparseArrayCompat<Float>()
        characterWidths.put(-1, glyphWidths[0].toFloat())

        val numVarSelectorRecords = TTFParser.getUInt32At(data, pos.toInt() + 6)
        var start = pos + 10
        for (i in 0 until numVarSelectorRecords) {
            // NOTE: Default UVS table wil be ignored for now since it requires to use another CMap table of 4 or 12 format.
            // For now all characters not mapped using non-default UVS table are treated as missing characters.

            val nonDefaultUVSOffset = TTFParser.getUInt32At(data, start.toInt() + 7)
            val numUVSMappings = TTFParser.getUInt32At(data, nonDefaultUVSOffset.toInt())
            var mappingStart = nonDefaultUVSOffset + 4
            for (j in 0 until numUVSMappings) {
                val c = getUInt24At(data, mappingStart.toInt())
                val glyphIndex = TTFParser.getUInt16At(data, mappingStart.toInt() + 3)
                if (
                    glyphIndex in 0..glyphWidths.lastIndex
                    && glyphWidths[glyphIndex] > 0
                ) {
                    characterWidths.put(c.toInt(), glyphWidths[glyphIndex].toFloat())
                }
                mappingStart += 5
            }
            start += 11
        }


        return characterWidths
    }

    private fun getUInt24At(data: ByteArray, start: Int): Long {
        var num = 0L
        num = num or (data[start].toInt() and 0xff).toLong()
        num = num shl 8
        num = num or (data[start + 1].toInt() and 0xff).toLong()
        num = num shl 8
        num = num or (data[start + 2].toInt() and 0xff).toLong()
        return num
    }
}