package marabillas.loremar.andpdf.document

import marabillas.loremar.andpdf.objects.Numeric
import marabillas.loremar.andpdf.objects.Stream
import java.io.RandomAccessFile

internal class ObjectStream(file: RandomAccessFile, start: Long) : Stream(file, start) {
    fun extractObjectBytes(index: Int): ByteArray? {
        val n = dictionary["N"] as Numeric
        if (index < n.value.toInt()) {
            val stream = decodeEncodedStream()
            val first = (dictionary["First"] as Numeric).value.toInt()
            val firstArray = stream.copyOfRange(0, first)
            val indString = String(firstArray, Charsets.US_ASCII).trim()
            val indArray = indString.split(" ")
            val objPos = indArray[index * 2 + 1].toInt() + first
            return if (index + 1 < indArray.size / 2) {
                val nextObjPos = indArray[(index + 1) * 2 + 1].toInt() + first
                stream.copyOfRange(objPos, nextObjPos)
            } else {
                stream.copyOfRange(objPos, stream.size)
            }
        } else return null
    }
}