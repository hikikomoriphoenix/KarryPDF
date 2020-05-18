package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.exceptions.IndirectObjectMismatchException
import marabillas.loremar.karrypdf.utils.exts.appendBytes
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
        file.seek(start)
        obj = extractNextNumber()
        start = file.filePointer - 1 - (obj as Int).length()
        gen = extractNextNumber()

        validateObj()

        if (reference != null) {
            if (reference.obj != obj) throw IndirectObjectMismatchException()
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

    fun extractContent(destStringBuilder: StringBuilder) {
        file.seek(start)
        destStringBuilder.clear()
        while (true) {
            val s = readFileLine()
            s.insert(0, ' ')
            if (s.endsWith("stream", true)) {
                destStringBuilder.clear().append("pdf_stream_content")
                return
            }
            destStringBuilder.append(s)
            if (s.contains("endobj", true)) break
        }
        val objIndex = destStringBuilder.indexOf("obj")
        val endObjIndex = destStringBuilder.lastIndexOf("endobj")
        destStringBuilder.delete(endObjIndex, destStringBuilder.length)
        destStringBuilder.delete(0, objIndex + 3)
    }

    private fun readFileLine(): StringBuilder {
        stringBuilder.clear()
        while (true) {
            val read = file.read(readBuffer, 0, READ_BUFFER_SIZE)
            if (read <= 0)
                break
            var end = 0
            for (i in 0 until read) {
                end = i + 1
                val c = readBuffer[i].toChar()
                if (c == '\n' || c == '\r') {
                    end = i
                    break
                }
            }
            stringBuilder.appendBytes(readBuffer, 0, end)
            if (readBuffer[end].toChar() == '\n') {
                val numNotAppended = read - end
                file.seek(file.filePointer - numNotAppended + 1)
                break
            } else if (readBuffer[end].toChar() == '\r' && end + 1 < read && readBuffer[end].toChar() == '\n') {
                val numNotAppended = read - end
                file.seek(file.filePointer - numNotAppended + 2)
                break
            } else if (readBuffer[end].toChar() == '\r') {
                val numNotAppended = read - end
                file.seek(file.filePointer - numNotAppended + 1)
                val curr = file.filePointer
                if (file.read().toChar() != '\n')
                    file.seek(curr)
                break
            }
        }
        return stringBuilder
    }

    companion object {
        private val stringBuilder = StringBuilder()
        private const val READ_BUFFER_SIZE = 1024
        private val readBuffer = ByteArray(READ_BUFFER_SIZE)
    }
}