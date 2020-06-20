package marabillas.loremar.karrypdf.document

import java.io.RandomAccessFile
import java.nio.ByteBuffer

internal class FileLineReader(private val file: RandomAccessFile) {
    val charBuffer = CharBuffer(32000)

    private val fileChannel = file.channel
    private val readBuffers: MutableMap<Int, ByteBuffer> = mutableMapOf()
    private var bufferPosition: Long = -1L

    private lateinit var currentReadBuffer: ByteBuffer

    private var read = 0
    private var lastChar: Char? = null

    private var lineStart = 0
    private var lineEnd = -1

    fun read(readBufferSize: Int = READ_BUFFER_SIZE_DEFAULT) {
        currentReadBuffer = readBuffers[readBufferSize] ?: ByteBuffer
            .allocateDirect(readBufferSize)
            .apply {
                readBuffers[readBufferSize] = this
            }
        charBuffer.rewind()
        read = 0
        lastChar = null
        while (true) {
            if (bufferPosition == -1L
                || file.filePointer < bufferPosition
                || file.filePointer >= bufferPosition + readBufferSize
            ) {

                bufferPosition = file.filePointer
                bufferPosition = file.filePointer
                currentReadBuffer.rewind()
                fileChannel.position(file.filePointer)
                fileChannel.read(currentReadBuffer)
                currentReadBuffer.rewind()
                file.seek(fileChannel.position())
            } else {
                currentReadBuffer.position((file.filePointer - bufferPosition).toInt())
            }
            read = currentReadBuffer.remaining()

            if (read <= 0)
                break

            while (currentReadBuffer.hasRemaining()) {
                val c = (currentReadBuffer.get().toInt() and 0xff).toChar()
                charBuffer.put(c)
                if (c == '\n' || c == '\r') {
                    charBuffer.trimLast()
                    lastChar = c
                    break
                }
            }

            if (!currentReadBuffer.hasRemaining()) {
                file.seek(bufferPosition + currentReadBuffer.position())
                if (lastChar == '\r') {
                    val curr = file.filePointer
                    if (file.read().toChar() != '\n')
                        file.seek(curr)
                    break
                }
            } else if (lastChar == '\n') {
                file.seek(bufferPosition + currentReadBuffer.position())
                break
            } else if (lastChar == '\r') {
                val prevPosition = currentReadBuffer.position()
                if (currentReadBuffer.get().toChar() == '\n')
                    file.seek(bufferPosition + currentReadBuffer.position())
                else
                    file.seek(bufferPosition + prevPosition)
                break
            }
        }
    }

    fun getNextLine(
        nextLineData: NextLineData,
        readBufferSize: Int = READ_BUFFER_SIZE_DEFAULT
    ) {
        currentReadBuffer = readBuffers[readBufferSize] ?: ByteBuffer
            .allocateDirect(readBufferSize)
            .apply {
                readBuffers[readBufferSize] = this
            }

        lineStart = if (bufferPosition == -1L
            || file.filePointer < bufferPosition
            || file.filePointer >= bufferPosition + readBufferSize
        ) {

            bufferPosition = file.filePointer
            bufferPosition = file.filePointer
            currentReadBuffer.rewind()
            fileChannel.position(file.filePointer)
            fileChannel.read(currentReadBuffer)
            file.seek(fileChannel.position())
            0
        } else
            (file.filePointer - bufferPosition).toInt()

        lineEnd = -1

        while (true) {
            currentReadBuffer.position(lineStart)
            read = currentReadBuffer.remaining()

            if (read <= 0)
                break

            lastChar = null
            while (currentReadBuffer.hasRemaining()) {
                val c = currentReadBuffer.get().toChar()
                lineEnd = currentReadBuffer.position()
                if (c == '\n' || c == '\r') {
                    lastChar = c
                    lineEnd = currentReadBuffer.position() - 1
                    break
                }
            }

            if (!currentReadBuffer.hasRemaining()) {
                file.seek(bufferPosition + read)
                if (lastChar == '\r') {
                    val curr = file.filePointer
                    if (file.read().toChar() != '\n')
                        file.seek(curr)
                    break
                }
            } else if (lastChar == '\n') {
                file.seek(bufferPosition + currentReadBuffer.position())
                break
            } else if (lastChar == '\r' && currentReadBuffer.get().toChar() == '\n') {
                file.seek(bufferPosition + currentReadBuffer.position())
                break
            }

            if (!isEndOfLine()) {
                file.seek(bufferPosition + lineStart)
                lineStart = 0
                lineEnd = 0
                bufferPosition = file.filePointer
                bufferPosition = file.filePointer
                currentReadBuffer.rewind()
                fileChannel.position(file.filePointer)
                fileChannel.read(currentReadBuffer)
                file.seek(fileChannel.position())
            }
        }

        nextLineData.buffer = currentReadBuffer
        nextLineData.start = lineStart
        nextLineData.end = lineEnd

    }

    private fun isEndOfLine(): Boolean {
        val curr = file.filePointer
        val read = file.read()
        file.seek(curr)
        return read == -1
    }

    companion object {
        const val READ_BUFFER_SIZE_DEFAULT = 32000
        const val READ_BUFFER_SIZE_64 = 64
    }
}

data class NextLineData(var buffer: ByteBuffer?, var start: Int, var end: Int)