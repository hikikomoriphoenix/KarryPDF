package marabillas.loremar.pdfparser.objects

import java.io.RandomAccessFile

/**
 * An indirect object in a PDF file.
 *
 * @param file PDF file
 * @param start offset position where the beginning of the indirect object is located
 */
open class IndirectObject(private val file: RandomAccessFile, private val start: Long) {
    var obj: Int? = null
        private set
    var gen: Int = 0
        private set

    init {
        val s = file.readLine().trim()
        obj = s.substringBefore(' ').toInt()
        gen = s.substringAfter(' ').substringBefore(' ').toInt()
    }
}