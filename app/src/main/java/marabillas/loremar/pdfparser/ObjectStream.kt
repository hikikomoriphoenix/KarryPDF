package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.objects.Numeric
import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.Stream
import marabillas.loremar.pdfparser.objects.toPDFObject
import java.io.RandomAccessFile

internal class ObjectStream(file: RandomAccessFile, start: Long) : Stream(file, start) {
    /**
     * Get the contents of a compressed object from an object stream given its index in the stream.
     *
     * @param index Index of object in the stream.
     *
     * @return a PDFObject or a null if index is out of bounds.
     */
    fun getObject(index: Int): PDFObject? {
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
                val objBytes = stream.copyOfRange(objPos, nextObjPos)
                String(objBytes, Charsets.US_ASCII).toPDFObject()
            } else {
                val objBytes = stream.copyOfRange(objPos, stream.size)
                String(objBytes, Charsets.US_ASCII).toPDFObject()
            }
        } else return null
    }

    fun getObjectWithObjectNum(obj: Int): PDFObject? {
        val stream = decodeEncodedStream()
        val first = (dictionary["First"] as Numeric).value.toInt()
        val firstArray = stream.copyOfRange(0, first)
        val indString = String(firstArray, Charsets.US_ASCII).trim()
        val indArray = indString.split(" ")
        for (i in 0 until indArray.size step 2) {
            if (indArray[i].toInt() == obj) {
                val objPos = indArray[i + 1].toInt() + first
                return if (i + 2 < indArray.size) {
                    val nextObjPos = indArray[i + 3].toInt() + first
                    val objBytes = stream.copyOfRange(objPos, nextObjPos)
                    String(objBytes, Charsets.US_ASCII).toPDFObject()
                } else {
                    val objBytes = stream.copyOfRange(objPos, stream.size)
                    String(objBytes, Charsets.US_ASCII).toPDFObject()
                }
            }
        }
        return null
    }
}