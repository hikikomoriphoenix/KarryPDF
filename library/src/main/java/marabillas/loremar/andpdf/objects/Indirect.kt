package marabillas.loremar.andpdf.objects

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

    fun extractContent(): StringBuilder {
        file.seek(start)
        val sb = StringBuilder()
        while (true) {
            val s = " ${file.readLine()}"
            if (s.endsWith("stream", true)) return sb.clear().append("pdf_stream_content")
            sb.append(s)
            if (s.contains("endobj", true)) break
        }
        val objIndex = sb.indexOf("obj")
        val endobjIndex = sb.lastIndexOf("endobj")
        sb.delete(endobjIndex, sb.length)
        sb.delete(0, objIndex + 3)
        return sb
    }
}