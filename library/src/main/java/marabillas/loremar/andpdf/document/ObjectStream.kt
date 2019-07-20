package marabillas.loremar.andpdf.document

import marabillas.loremar.andpdf.objects.Numeric
import marabillas.loremar.andpdf.objects.Reference
import marabillas.loremar.andpdf.objects.Stream
import java.io.RandomAccessFile

internal class ObjectStream(context: AndPDFContext, file: RandomAccessFile, start: Long, reference: Reference? = null) :
    Stream(context, file, start, reference) {
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

    fun extractObjectBytesGivenObjectNum(objNum: Int): ByteArray? {
        val stream = decodeEncodedStream()
        val first = (dictionary["First"] as Numeric).value.toInt()
        val firstArray = stream.copyOfRange(0, first)
        val indString = String(firstArray, Charsets.US_ASCII).trim()
        val indArray = indString.split(" ")
        for (i in 0 until indArray.size step 2) {
            if (indArray[i].toInt() == objNum) {
                val objPos = indArray[i + 1].toInt() + first
                return if (i + 2 < indArray.size) {
                    val nextObjPos = indArray[i + 3].toInt() + first
                    stream.copyOfRange(objPos, nextObjPos)
                } else {
                    stream.copyOfRange(objPos, stream.size)
                }
            }
        }
        return null
    }
}