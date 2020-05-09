package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.exceptions.IndirectObjectMismatchException
import marabillas.loremar.karrypdf.utils.exts.toInt
import marabillas.loremar.karrypdf.utils.length
import java.io.RandomAccessFile

/**
 * An indirect object in a PDF file.
 *
 * @param file PDF file
 * @param start offset position where the beginning of the indirect object is located
 */
internal open class Indirect(
    private val file: RandomAccessFile,
    private var start: Long,
    reference: Reference? = null
) {
    var obj: Int? = null
        private set
    var gen: Int = 0
        private set

    init {
        synchronized(file) {
            file.seek(start)
            obj = extractNextNumber()
            start = file.filePointer - 1 - (obj as Int).length()
            gen = extractNextNumber()

            validateObj()

            if (reference != null) {
                if (reference.obj != obj) throw IndirectObjectMismatchException()
            }
        }
    }

    private fun extractNextNumber(): Int {
        stringBuilder.clear()
        var c = nextDigit()
        while (c.isDigit()) {
            stringBuilder.append(c)
            c = file.readByte().toChar()
        }

        return stringBuilder.toInt()
    }

    private fun nextDigit(): Char {
        var c = ' '
        while (!c.isDigit()) {
            c = file.readByte().toChar()
        }
        return c
    }

    private fun validateObj() {
        file.seek(file.filePointer - 1)
        while (true) {
            val c = file.readByte().toChar()
            if (c != ' ') {
                if (c != 'o') {
                    throw IndirectObjectMismatchException()
                } else {
                    break
                }
            }
        }
        val b = file.readByte().toChar()
        val j = file.readByte().toChar()
        if (b != 'b' || j != 'j') {
            throw IndirectObjectMismatchException()
        }
    }

    fun extractContent(): StringBuilder {
        synchronized(file) {
            file.seek(start)
            val sb = StringBuilder()
            while (true) {
                val s = " ${file.readLine()}"
                if (s.endsWith("stream", true)) return sb.clear()
                    .append("pdf_stream_content")
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

    companion object {
        val stringBuilder = StringBuilder()
    }
}