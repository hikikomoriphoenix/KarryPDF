package marabillas.loremar.andpdf.objects

import marabillas.loremar.andpdf.utils.exts.toInt
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

    private object Inst {
        val stringBuilder = StringBuilder()
    }

    init {
        file.seek(start)
        obj = extractNextNumber()
        gen = extractNextNumber()
    }

    private fun extractNextNumber(): Int {
        Inst.stringBuilder.clear()
        var c = nextDigit()
        while (c != ' ') {
            Inst.stringBuilder.append(c)
            c = file.readByte().toChar()
        }

        return Inst.stringBuilder.toInt()
    }

    private fun nextDigit(): Char {
        var c = ' '
        while (!c.isDigit()) {
            c = file.readByte().toChar()
        }
        return c
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