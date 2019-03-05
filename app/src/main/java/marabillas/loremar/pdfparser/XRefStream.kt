package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.objects.StreamObject
import java.io.RandomAccessFile

/**
 * Class for parsing a cross reference stream
 *
 * @param file  PDF file containing the stream.
 * @param start offset position where beginning of the cross reference stream's object is located.
 */
class XRefStream(private val file: RandomAccessFile, private val start: Long) : StreamObject(file, start) {
}