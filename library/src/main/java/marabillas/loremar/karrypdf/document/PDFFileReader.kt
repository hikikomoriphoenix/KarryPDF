package marabillas.loremar.karrypdf.document

import marabillas.loremar.karrypdf.exceptions.InvalidDocumentException
import marabillas.loremar.karrypdf.objects.*
import marabillas.loremar.karrypdf.utils.exts.*
import marabillas.loremar.karrypdf.utils.logd
import java.io.RandomAccessFile

/**
 * This class facilitates reading in a pdf file.
 */
internal class PDFFileReader(val file: RandomAccessFile) {
    private var startXRefPos: Long? = null
    private var trailerPos: Long? = null
    private var isLinearized: Boolean? = null

    companion object {
        private const val READ_BUFFER_SIZE_DEFAULT = 32000
        private const val READ_BUFFER_SIZE_64 = 64
        private val STRING_BUILDERS: MutableMap<KarryPDFContext.Session, StringBuilder> =
            mutableMapOf()
        private val READ_BUFFERS: MutableMap<Int, MutableMap<KarryPDFContext.Session, ByteArray>> =
            mutableMapOf(
                READ_BUFFER_SIZE_DEFAULT to mutableMapOf(),
                READ_BUFFER_SIZE_64 to mutableMapOf()
            )
        private val BUFFER_POSITIONS: MutableMap<KarryPDFContext.Session, Long> = mutableMapOf()

        fun notifyNewSession(session: KarryPDFContext.Session) {
            STRING_BUILDERS[session] = StringBuilder()
            READ_BUFFERS[READ_BUFFER_SIZE_DEFAULT]?.set(
                session,
                ByteArray(READ_BUFFER_SIZE_DEFAULT)
            )
            READ_BUFFERS[READ_BUFFER_SIZE_64]?.set(session, ByteArray(READ_BUFFER_SIZE_64))
            BUFFER_POSITIONS[session] = -1L
        }
    }

    fun isLinearized(context: KarryPDFContext): Boolean {
        synchronized(file) {
            return if (isLinearized == null) {
                file.seek(0)
                var s = readFileLine(context)
                var beginning = file.filePointer
                while (s.startsWith('%')) {
                    beginning = file.filePointer
                    s = readFileLine(context)
                }
                val indObj = getIndirectObject(beginning)
                val destSb = STRING_BUILDERS[context.session] ?: StringBuilder()
                indObj.extractContent(destSb)
                val firstObj = destSb.toPDFObject(context, indObj.obj ?: -1, indObj.gen)
                if (firstObj is Dictionary) {
                    val linearized = firstObj["Linearized"]
                    if (linearized != null) {
                        isLinearized = true
                        return true
                    }
                }
                isLinearized = false
                false
            } else {
                isLinearized as Boolean
            }
        }
    }

    fun getStartXRefPositionLinearized(context: KarryPDFContext): Long {
        synchronized(file) {
            return if (isLinearized(context)) {
                file.seek(0)
                var s = readFileLine(context)
                while (!s.contains("endobj"))
                    s = readFileLine(context)
                var beginning = file.filePointer
                s = readFileLine(context)
                while (s.isBlank() || s.startsWith('%')) {
                    beginning = file.filePointer
                    s = readFileLine(context)
                }
                beginning
            } else {
                throw IllegalStateException("PDF document is not linearized")
            }
        }
    }

    /**
     * Read the line containing the character in the given offset position. Trailing line feed and carriage return is
     * treated as part of the line but will be discarded in the returned output. The file pointer is also set to the
     * location of the line feed or carriage return preceding the first character of the line. If the line is the first
     * line of the file, then file pointer is set to the beginning of the file.
     *
     * @param position Offset position within the required line
     *
     * @return The required containing line
     *
     * @throws IllegalArgumentException If position is not within beginning and end of file.
     */
    private fun readContainingLine(context: KarryPDFContext, position: Long): StringBuilder {
        if (position < 0 || position > file.length() - 1) throw IllegalArgumentException()

        var nonLineBreakFound = false
        var p = position
        while (true) {
            file.seek(p)
            val c = file.readByte().toChar()
            if (c == '\n' || c == '\r') {
                if (nonLineBreakFound) {
                    val s = readFileLine(context)
                    file.seek(p)
                    return s
                }
            } else {
                nonLineBreakFound = true
            }

            p--
            if (p <= 0) {
                file.seek(0)
                return readFileLine(context)
            }
        }
    }

    /**
     * Get the offset position of the cross reference section.
     */
    private fun getStartXRefPosition(context: KarryPDFContext): Long {
        if (!isLinearized(context)) {
            if (startXRefPos == null) {
                var p = file.length() - 1
                while (true) {
                    var s = readContainingLine(context, p)
                    if (s.startsWith("startxref")) {
                        file.seek(file.filePointer + 1)
                        readFileLine(context)
                        while (true) {
                            s = readFileLine(context)
                            if (!s.startsWith("%")) {
                                startXRefPos = s.toNumeric().value.toLong()
                                return getValidXRefPos(context, startXRefPos as Long)
                            }
                        }
                    }
                    p = file.filePointer
                }
            } else return getValidXRefPos(context, startXRefPos as Long)
        } else {
            return getValidXRefPos(context, getStartXRefPositionLinearized(context))
        }
    }

    private fun getValidXRefPos(context: KarryPDFContext, pos: Long): Long {
        file.seek(pos)
        var isXrefStream = false
        var foundXref = false
        var s: StringBuilder? = null
        var xi: Int = -1
        while (!isEndOfLine()) {
            s = readFileLine(context)
            xi = s.indexOf("xref", 0, true)
            if (xi != -1) {
                isXrefStream = s[xi] == 'X'
                foundXref = true
                break
            }
        }

        return if (!foundXref)
            throw InvalidDocumentException(
                "Can not find valid cross reference table"
            )
        else {
            file.seek(file.filePointer - 2)
            if (file.readByte().toChar() == '\r' && s != null) {
                val shift = s.length - (xi + 4) + 1
                file.seek(file.filePointer - shift)
            } else if (s != null) {
                val shift = s.length - (xi + 4)
                file.seek(file.filePointer - shift)
            }

            if (isXrefStream) {
                findStartOfXRefStream(file.filePointer)
            } else {
                file.filePointer - 4
            }
        }
    }

    private fun findStartOfXRefStream(start: Long): Long {
        file.seek(start)
        while (file.filePointer >= 0) {
            file.seek(file.filePointer - 2)
            var c = file.readByte().toChar()
            if (c == 'o') {
                c = file.readByte().toChar()
                if (c == 'b') {
                    c = file.readByte().toChar()
                    if (c == 'j') {
                        file.seek(file.filePointer - 4)
                        c = file.readByte().toChar()
                        while (!c.isDigit()) {
                            file.seek(file.filePointer - 2)
                            c = file.readByte().toChar()
                        }
                        while (c.isDigit()) {
                            file.seek(file.filePointer - 2)
                            c = file.readByte().toChar()
                        }
                        while (!c.isDigit()) {
                            file.seek(file.filePointer - 2)
                            c = file.readByte().toChar()
                        }
                        while (c.isDigit()) {
                            file.seek(file.filePointer - 2)
                            c = file.readByte().toChar()
                        }
                        return file.filePointer
                    } else {
                        file.seek(file.filePointer - 4)
                    }
                } else {
                    file.seek(file.filePointer - 3)
                }
            }
        }

        throw InvalidDocumentException("Cant find start of cross reference stream")
    }

    /**
     * Locate the last cross reference section in the file and parse all of its entries into a collection
     *
     * @return a map of cross reference entries
     */
    fun getLastXRefData(context: KarryPDFContext): HashMap<String, XRefEntry> {
        synchronized(file) {
            val startXRef = getStartXRefPosition(context)
            return getXRefData(context, startXRef)
        }
    }

    /**
     * Given the byte offset position of a cross reference section, parse all of its entries.
     *
     * @param pos Byte offset position of the cross reference section.
     *
     * @return a map of cross reference entries
     */
    fun getXRefData(context: KarryPDFContext, pos: Long): HashMap<String, XRefEntry> {
        synchronized(file) {
            file.seek(pos)
            val s = readFileLine(context)
            return if (s.contains("xref")) {
                var data = parseXRefSection(context)
                data = parseOtherXRefInTrailer(context, file.filePointer, data)
                data
            } else {
                XRefStream(
                    context,
                    file,
                    pos
                ).parse()
            }
        }
    }

    /**
     * Parse through each line of the cross reference section to get all of its entries. The offset position must
     * currently be in the beginning of the first subsection.
     */
    private fun parseXRefSection(context: KarryPDFContext): HashMap<String, XRefEntry> {
        val entries = HashMap<String, XRefEntry>()

        val nextLineData = NextLineData(byteArrayOf(), 0, 0)

        logd("Parsing XRef section start")
        val subSectionRegex = Regex("^\\s*(\\d+) (\\d+)\\s*$")
        while (!isEndOfLine()) {
            val p = file.filePointer
            // Find next subsection
            val s = readFileLine(context)
            if (s.isBlank() && !isEndOfLine()) continue
            if (!s.matches(subSectionRegex)) {
                // File pointer should be reset to right after the last entry
                file.seek(p)
                break
            }

            s.trimContainedChars()
            val spi = s.indexOf(' ')
            val obj = s.toInt(0, spi)
            val count = s.toInt(spi + 1)

            // Iterate through every entry and add to entries
            for (i in obj until obj + count) {
                //logd("Parsing XRef entry for obj $i ")
                getNextLine(context, nextLineData)

                nextLineData.apply {
                    val sp1 = buffer.indexOfChar(' ', start, end)
                    val sp2 = buffer.indexOfChar(' ', sp1 + 1, end)
                    val pos = buffer.toLong(start, sp1)
                    val gen = buffer.toInt(sp1 + 1, sp2)

                    if (buffer[sp2 + 1].toChar() == 'f') {
                        entries["$i $gen"] =
                            XRefEntry(
                                i,
                                pos,
                                gen,
                                false
                            )
                    } else {
                        entries["$i $gen"] =
                            XRefEntry(
                                i,
                                pos,
                                gen
                            )
                    }
                }
            }
        }
        logd("Parsing XRef section end")
        return entries
    }

    private fun parseOtherXRefInTrailer(
        context: KarryPDFContext,
        endXRefPos: Long,
        xRefEntries: HashMap<String, XRefEntry>
    ): HashMap<String, XRefEntry> {
        var entries = xRefEntries
        var p: Long
        var s: StringBuilder
        file.seek(endXRefPos)
        do {
            p = file.filePointer
            s = readFileLine(context)
        } while (!s.startsWith("trailer"))

        val trailer = getDictionary(context, p, -1, 0, false)

        // Parse any existing cross reference stream
        val xRefStm = trailer["XRefStm"] as Numeric?
        if (xRefStm != null) {
            logd("XRefStm = ${xRefStm.value.toLong()}")
            val data = getXRefData(context, (xRefStm.value.toLong()))
            data.putAll(entries)
            entries = data
        }

        // Parse any existing previous cross reference table
        val prev = trailer["Prev"] as Numeric?
        if (prev != null) {
            logd("Prev = ${prev.value.toLong()}")
            val data = getXRefData(context, prev.value.toLong())
            data.putAll(entries)
            entries = data
        }
        return entries
    }

    /**
     * Gets the byte offset position of the trailer.
     *
     * @return position or null if PDF document does not have a trailer and that trailer entries are merged into a
     * cross reference stream instead.
     */
    fun getTrailerPosition(context: KarryPDFContext): Long? {
        synchronized(file) {
            return if (trailerPos == null) {
                val startXRef = getStartXRefPosition(context)
                file.seek(startXRef)
                var s = readFileLine(context)
                if (s.contains("xref")) {
                    parseXRefSection(context)
                    var p: Long
                    do {
                        p = file.filePointer
                        s = readFileLine(context)
                    } while (!s.startsWith("trailer"))
                    p
                } else {
                    null
                }
            } else {
                trailerPos
            }
        }
    }

    fun getTrailerEntries(
        context: KarryPDFContext,
        resolveReferences: Boolean = true
    ): HashMap<String, PDFObject?> {
        synchronized(file) {
            val trailerPos = getTrailerPosition(context)
            return if (trailerPos != null) {
                file.seek(trailerPos)
                val dictionary = getDictionary(context, file.filePointer, -1, 0, resolveReferences)
                if (resolveReferences) dictionary.resolveReferences()
                createTrailerHashMap(dictionary)
            } else {
                // Get trailer entries from XRefStream dictionary
                val xrefStm = XRefStream(
                    context,
                    file,
                    getStartXRefPosition(context)
                )
                if (resolveReferences) xrefStm.dictionary.resolveReferences()
                createTrailerHashMap(xrefStm.dictionary)
            }
        }
    }

    private fun createTrailerHashMap(dictionary: Dictionary): HashMap<String, PDFObject?> {
        return hashMapOf(
            "Size" to dictionary["Size"],
            "Prev" to dictionary["Prev"],
            "Root" to dictionary["Root"],
            "Encrypt" to dictionary["Encrypt"],
            "Info" to dictionary["Info"],
            "ID" to dictionary["ID"],
            "XRefStm" to dictionary["XRefStm"]
        )
    }

    fun getIndirectObject(
        pos: Long,
        reference: Reference? = null
    ): Indirect {
        synchronized(file) {
            return Indirect(file, pos, reference)
        }
    }

    fun getDictionary(
        context: KarryPDFContext,
        pos: Long,
        obj: Int,
        gen: Int,
        resolveReferences: Boolean = false
    ): Dictionary {
        synchronized(file) {
            file.seek(pos)
            goToDictionaryStart()
            val stringBuilder = STRING_BUILDERS[context.session] ?: StringBuilder()
            extractDictionary(stringBuilder)
            return stringBuilder.toPDFObject(context, obj, gen, resolveReferences) as Dictionary
        }
    }

    private fun goToDictionaryStart() {
        var isComment = false
        var isLineStart = true
        while (true) {
            val c = file.readByte().toChar()
            if (isLineStart) {
                isComment = c == '%'
                isLineStart = false
            }
            if (isComment)
                continue
            if (c == '\n' || c == '\r')
                isLineStart = true
            else if (c == '<' && !isComment) {
                val c2 = file.readByte().toChar()
                if (c2 == '<')
                    break
            }
        }
    }

    private fun extractDictionary(stringBuilder: StringBuilder) {
        stringBuilder.clear().append("<<")
        var unbalance = 1
        while (unbalance != 0) {
            var c = file.readByte().toChar()
            if (c == '<') {
                stringBuilder.append(c)
                c = file.readByte().toChar()
                if (c == '<') unbalance++
            } else if (c == '>') {
                stringBuilder.append(c)
                c = file.readByte().toChar()
                if (c == '>') unbalance--
            }
            stringBuilder.append(c)
        }
    }

    fun getObjectStream(
        context: KarryPDFContext,
        pos: Long,
        reference: Reference? = null
    ): ObjectStream {
        synchronized(file) {
            return ObjectStream(
                context,
                file,
                pos,
                reference
            )
        }
    }

    fun getStream(context: KarryPDFContext, pos: Long, reference: Reference? = null): Stream {
        synchronized(file) {
            return Stream(context, file, pos, reference)
        }
    }

    private fun readFileLine(
        context: KarryPDFContext,
        readBufferSize: Int = READ_BUFFER_SIZE_DEFAULT
    ): StringBuilder {
        val sb = STRING_BUILDERS[context.session] ?: StringBuilder().apply {
            STRING_BUILDERS[context.session] = this
        }
        val buffer = READ_BUFFERS[readBufferSize]?.get(context.session) ?: ByteArray(readBufferSize)
            .apply {
                READ_BUFFERS[readBufferSize]?.set(context.session, this)
            }
        var bufferPos = BUFFER_POSITIONS[context.session] ?: (-1L).also {
            BUFFER_POSITIONS[context.session] = it
        }

        sb.clear()
        while (true) {
            val start = if (bufferPos == -1L
                || file.filePointer < bufferPos
                || file.filePointer >= bufferPos + readBufferSize
            ) {

                BUFFER_POSITIONS[context.session] = file.filePointer
                bufferPos = file.filePointer
                file.read(buffer, 0, readBufferSize)
                0
            } else
                (file.filePointer - bufferPos).toInt()

            val read = if (file.length() - bufferPos >= readBufferSize.toLong())
                readBufferSize else (file.length() - bufferPos).toInt()

            if (read <= 0)
                break

            var end = -1
            for (i in start until read) {
                end = i + 1
                val c = buffer[i].toChar()
                if (c == '\n' || c == '\r') {
                    end = i
                    break
                }
            }

            sb.appendBytes(buffer, start, end)

            if (end >= read) {
                file.seek(bufferPos + end)
            } else if (buffer[end].toChar() == '\n') {
                file.seek(bufferPos + end + 1)
                break
            } else if (buffer[end].toChar() == '\r' && end + 1 < read && buffer[end].toChar() == '\n') {
                file.seek(bufferPos + end + 2)
                break
            } else if (buffer[end].toChar() == '\r') {
                file.seek(bufferPos + end + 1)
                val curr = file.filePointer
                if (file.read().toChar() != '\n')
                    file.seek(curr)
                break
            }
        }
        return sb
    }

    private fun getNextLine(
        context: KarryPDFContext,
        nextLineData: NextLineData,
        readBufferSize: Int = READ_BUFFER_SIZE_DEFAULT
    ) {

        val buffer = READ_BUFFERS[readBufferSize]?.get(context.session) ?: ByteArray(readBufferSize)
            .apply {
                READ_BUFFERS[readBufferSize]?.set(context.session, this)
            }
        var bufferPos = BUFFER_POSITIONS[context.session] ?: (-1L).also {
            BUFFER_POSITIONS[context.session] = it
        }

        var start = if (bufferPos == -1L
            || file.filePointer < bufferPos
            || file.filePointer >= bufferPos + readBufferSize
        ) {

            BUFFER_POSITIONS[context.session] = file.filePointer
            bufferPos = file.filePointer
            file.read(buffer, 0, readBufferSize)
            0
        } else
            (file.filePointer - bufferPos).toInt()

        var end = -1

        while (true) {
            val read = if (file.length() - bufferPos >= readBufferSize.toLong())
                readBufferSize else (file.length() - bufferPos).toInt()

            if (read <= 0)
                break

            for (i in start until read) {
                end = i + 1
                val c = buffer[i].toChar()
                if (c == '\n' || c == '\r') {
                    end = i
                    break
                }
            }

            if (end >= read) {
                file.seek(bufferPos + end)
            } else if (buffer[end].toChar() == '\n') {
                file.seek(bufferPos + end + 1)
                break
            } else if (buffer[end].toChar() == '\r' && end + 1 < read && buffer[end].toChar() == '\n') {
                file.seek(bufferPos + end + 2)
                break
            } else if (buffer[end].toChar() == '\r') {
                file.seek(bufferPos + end + 1)
                val curr = file.filePointer
                if (file.read().toChar() != '\n')
                    file.seek(curr)
                break
            }

            if (!isEndOfLine()) {
                file.seek(bufferPos + start)
                start = 0
                end = 0
                bufferPos = file.filePointer
                BUFFER_POSITIONS[context.session] = file.filePointer
                file.read(buffer, 0, readBufferSize)
            }
        }

        nextLineData.buffer = buffer
        nextLineData.start = start
        nextLineData.end = end
    }

    private fun isEndOfLine(): Boolean {
        val curr = file.filePointer
        val read = file.read()
        file.seek(curr)
        return read == -1
    }

    data class NextLineData(var buffer: ByteArray, var start: Int, var end: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NextLineData

            if (!buffer.contentEquals(other.buffer)) return false
            if (start != other.start) return false
            if (end != other.end) return false

            return true
        }

        override fun hashCode(): Int {
            var result = buffer.contentHashCode()
            result = 31 * result + start
            result = 31 * result + end
            return result
        }
    }
}