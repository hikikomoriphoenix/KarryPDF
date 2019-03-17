package marabillas.loremar.pdfparser.objects

import java.io.RandomAccessFile

/**
 * An indirect object in a PDF file.
 *
 * @param file PDF file
 * @param start offset position where the beginning of the indirect object is located
 */
internal open class Indirect(private val file: RandomAccessFile, private val start: Long) {
    var obj: Int? = null
        private set
    var gen: Int = 0
        private set

    init {
        file.seek(start)
        var s = ""
        while (s == "")
            s = file.readLine().trim()
        obj = s.substringBefore(' ').toInt()
        gen = s.substringAfter(' ').substringBefore(' ').toInt()
    }

    fun extractContent(): String {
        file.seek(start)
        val sb = StringBuilder()
        while (true) {
            val s = file.readLine()
            sb.append(s)
            if (s.endsWith("endobj")) break
        }
        val s = sb.toString()
        return s.substringAfter("obj").substringBeforeLast("endobj").trim()
    }
}