package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.exceptions.IndirectObjectMismatchException
import marabillas.loremar.karrypdf.utils.exts.contains
import marabillas.loremar.karrypdf.utils.exts.toInt
import marabillas.loremar.karrypdf.utils.length
import java.io.RandomAccessFile
import java.nio.ByteBuffer

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

    private val fileChannel = file.channel

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

    fun extractContent(destStringBuilder: StringBuilder) {
        synchronized(file) {
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
    }

    private fun readFileLine(): StringBuilder {
        stringBuilder.clear()
        while (true) {
            if (bufferPos == -1L
                || file.filePointer < bufferPos
                || file.filePointer >= bufferPos + READ_BUFFER_SIZE
            ) {
                bufferPos = file.filePointer
                readBuffer.rewind()
                fileChannel.position(file.filePointer)
                fileChannel.read(readBuffer)
                readBuffer.rewind()
                file.seek(fileChannel.position())
            } else
                readBuffer.position((file.filePointer - bufferPos).toInt())

            val read = readBuffer.remaining()

            if (read <= 0)
                break

            var lastChar: Char? = null
            while (readBuffer.hasRemaining()) {
                val c = readBuffer.get().toChar()
                stringBuilder.append(c)
                if (c == '\n' || c == '\r') {
                    stringBuilder.deleteCharAt(stringBuilder.lastIndex)
                    lastChar = c
                    break
                }
            }

            if (!readBuffer.hasRemaining()) {
                file.seek(bufferPos + readBuffer.position())
                if (lastChar == '\r') {
                    val curr = file.filePointer
                    if (file.read().toChar() != '\n')
                        file.seek(curr)
                    break
                }
            } else if (lastChar == '\n') {
                file.seek(bufferPos + readBuffer.position())
                break
            } else if (lastChar == '\r') {
                val prevPosition = readBuffer.position()
                if (readBuffer.get().toChar() == '\n')
                    file.seek(bufferPos + readBuffer.position())
                else
                    file.seek(bufferPos + prevPosition)
                break
            }
        }
        return stringBuilder
    }

    companion object {
        private val stringBuilder = StringBuilder()
        private const val READ_BUFFER_SIZE = 32000
        private val readBuffer = ByteBuffer.allocateDirect(READ_BUFFER_SIZE)
        private var bufferPos = -1L
    }
}