package marabillas.loremar.karrypdf.document

import java.io.RandomAccessFile
import java.nio.ByteBuffer

internal class FileLineReader(private val file: RandomAccessFile) :
    PDFFileReader.Companion.NewSessionListener, PDFFileReader.Companion.EndSessionListener {
    private val fileChannel = file.channel
    private val readBuffers: MutableMap<Int, MutableMap<KarryPDFContext.Session, ByteBuffer>> =
        mutableMapOf(
            READ_BUFFER_SIZE_DEFAULT to mutableMapOf(),
            READ_BUFFER_SIZE_64 to mutableMapOf()
        )
    private val bufferPositions: MutableMap<KarryPDFContext.Session, Long> = mutableMapOf()

    private val charBuffers: MutableMap<KarryPDFContext.Session, CharBuffer> = mutableMapOf()

    private lateinit var currentReadBuffer: ByteBuffer
    private var currentBufferPosition: Long = 0L
    private lateinit var currentCharBuffer: CharBuffer

    private var read = 0
    private var lastChar: Char? = null

    private var lineStart = 0
    private var lineEnd = -1

    fun read(context: KarryPDFContext, readBufferSize: Int = READ_BUFFER_SIZE_DEFAULT) {
        currentReadBuffer =
            readBuffers[readBufferSize]?.get(context.session) ?: ByteBuffer.allocateDirect(
                readBufferSize
            )
                .apply {
                    readBuffers[readBufferSize]?.set(context.session, this)
                }
        currentBufferPosition = bufferPositions[context.session] ?: (-1L).also {
            bufferPositions[context.session] = it
        }
        currentCharBuffer = charBuffers[context.session] ?: CharBuffer(32000).also {
            charBuffers[context.session] = it
        }


        currentCharBuffer.rewind()

        read = 0
        lastChar = null
        while (true) {
            if (currentBufferPosition == -1L
                || file.filePointer < currentBufferPosition
                || file.filePointer >= currentBufferPosition + readBufferSize
            ) {

                bufferPositions[context.session] = file.filePointer
                currentBufferPosition = file.filePointer
                currentReadBuffer.rewind()
                fileChannel.position(file.filePointer)
                fileChannel.read(currentReadBuffer)
                currentReadBuffer.rewind()
                file.seek(fileChannel.position())
            } else {
                currentReadBuffer.position((file.filePointer - currentBufferPosition).toInt())
            }
            read = currentReadBuffer.remaining()

            if (read <= 0)
                break

            while (currentReadBuffer.hasRemaining()) {
                val c = (currentReadBuffer.get().toInt() and 0xff).toChar()
                currentCharBuffer.put(c)
                if (c == '\n' || c == '\r') {
                    currentCharBuffer.trimLast()
                    lastChar = c
                    break
                }
            }

            if (!currentReadBuffer.hasRemaining()) {
                file.seek(currentBufferPosition + currentReadBuffer.position())
                if (lastChar == '\r') {
                    val curr = file.filePointer
                    if (file.read().toChar() != '\n')
                        file.seek(curr)
                    break
                }
            } else if (lastChar == '\n') {
                file.seek(currentBufferPosition + currentReadBuffer.position())
                break
            } else if (lastChar == '\r') {
                val prevPosition = currentReadBuffer.position()
                if (currentReadBuffer.get().toChar() == '\n')
                    file.seek(currentBufferPosition + currentReadBuffer.position())
                else
                    file.seek(currentBufferPosition + prevPosition)
                break
            }
        }
    }

    fun getNextLine(
        context: KarryPDFContext,
        nextLineData: NextLineData,
        readBufferSize: Int = READ_BUFFER_SIZE_DEFAULT
    ) {
        currentReadBuffer =
            readBuffers[readBufferSize]?.get(context.session) ?: ByteBuffer.allocateDirect(
                readBufferSize
            )
                .apply {
                    readBuffers[readBufferSize]?.set(context.session, this)
                }
        currentBufferPosition = bufferPositions[context.session] ?: (-1L).also {
            bufferPositions[context.session] = it
        }

        lineStart = if (currentBufferPosition == -1L
            || file.filePointer < currentBufferPosition
            || file.filePointer >= currentBufferPosition + readBufferSize
        ) {

            bufferPositions[context.session] = file.filePointer
            currentBufferPosition = file.filePointer
            currentReadBuffer.rewind()
            fileChannel.position(file.filePointer)
            fileChannel.read(currentReadBuffer)
            file.seek(fileChannel.position())
            0
        } else
            (file.filePointer - currentBufferPosition).toInt()

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
                file.seek(currentBufferPosition + read)
                if (lastChar == '\r') {
                    val curr = file.filePointer
                    if (file.read().toChar() != '\n')
                        file.seek(curr)
                    break
                }
            } else if (lastChar == '\n') {
                file.seek(currentBufferPosition + currentReadBuffer.position())
                break
            } else if (lastChar == '\r' && currentReadBuffer.get().toChar() == '\n') {
                file.seek(currentBufferPosition + currentReadBuffer.position())
                break
            }

            if (!isEndOfLine()) {
                file.seek(currentBufferPosition + lineStart)
                lineStart = 0
                lineEnd = 0
                currentBufferPosition = file.filePointer
                bufferPositions[context.session] = file.filePointer
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

    override fun onNewSession(session: KarryPDFContext.Session) {
        readBuffers[READ_BUFFER_SIZE_DEFAULT]?.set(
            session,
            ByteBuffer.allocateDirect(READ_BUFFER_SIZE_DEFAULT)
        )
        readBuffers[READ_BUFFER_SIZE_64]?.set(
            session, ByteBuffer.allocateDirect(READ_BUFFER_SIZE_64)
        )
        bufferPositions[session] = -1L

        charBuffers[session] = CharBuffer(32000)
    }

    override fun onEndSession(session: KarryPDFContext.Session) {
        readBuffers[READ_BUFFER_SIZE_DEFAULT]?.remove(session)
        readBuffers[READ_BUFFER_SIZE_64]?.remove(session)
        bufferPositions.remove(session)
        charBuffers.remove(session)
    }

    fun getCharBuffer(session: KarryPDFContext.Session): CharBuffer {
        return charBuffers[session] ?: CharBuffer(32000).also {
            charBuffers[session] = it
        }
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