package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.objects.Numeric
import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.Stream
import marabillas.loremar.pdfparser.objects.toPDFObject
import java.io.RandomAccessFile

class ObjectStream(file: RandomAccessFile, start: Long) : Stream(file, start) {
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
            val bytes = stream.toByteArray()
            val first = (dictionary["First"] as Numeric).value.toInt()
            val firstArray = bytes.copyOfRange(0, first)
            val indString = firstArray.toString().trim()
            val indArray = indString.split(" ")
            val objPos = indArray[index * 2 + 1].toInt() + first
            return if (index + 1 < indArray.size) {
                val nextObjPos = indArray[(index + 1) * 2 + 1].toInt() + first
                val objBytes = bytes.copyOfRange(objPos, nextObjPos)
                objBytes.toString().toPDFObject()
            } else {
                val objBytes = bytes.copyOfRange(objPos, bytes.size)
                objBytes.toString().toPDFObject()
            }
        } else return null
    }
}