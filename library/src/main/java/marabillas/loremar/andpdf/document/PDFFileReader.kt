package marabillas.loremar.andpdf.document

import marabillas.loremar.andpdf.exceptions.InvalidDocumentException
import marabillas.loremar.andpdf.objects.*
import marabillas.loremar.andpdf.utils.logd
import java.io.RandomAccessFile

/**
 * This class facilitates reading in a pdf file.
 */
internal class PDFFileReader(private val context: AndPDFContext, private val file: RandomAccessFile) {
    private var startXRefPos: Long? = null
    private var trailerPos: Long? = null
    private var isLinearized: Boolean? = null

    private val stringBuilder = StringBuilder()

    fun isLinearized(): Boolean {
        if (isLinearized == null) {
            file.seek(0)
            var s = file.readLine()
            var beginning = file.filePointer
            while (s.startsWith('%')) {
                beginning = file.filePointer
                s = file.readLine()
            }
            val indObj = getIndirectObject(beginning)
            val firstObj = indObj.extractContent().toPDFObject(context, indObj.obj ?: -1, indObj.gen)
            if (firstObj is Dictionary) {
                val linearized = firstObj["Linearized"]
                if (linearized != null) {
                    isLinearized = true
                    return true
                }
            }
            isLinearized = false
            return false
        } else {
            return isLinearized as Boolean
        }
    }

    fun getStartXRefPositionLinearized(): Long {
        if (isLinearized()) {
            file.seek(0)
            var s = file.readLine()
            while (!s.contains("endobj"))
                s = file.readLine()
            var beginning = file.filePointer
            s = file.readLine()
            while (s.isBlank() || s.startsWith('%')) {
                beginning = file.filePointer
                s = file.readLine()
            }
            return beginning
        } else {
            throw IllegalStateException("PDF document is not linearized")
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
    fun readContainingLine(position: Long): String {
        if (position < 0 || position > file.length() - 1) throw IllegalArgumentException()

        var nonLineBreakFound = false
        var p = position
        while (true) {
            file.seek(p)
            val c = file.readByte().toChar()
            if (c == '\n' || c == '\r') {
                if (nonLineBreakFound) {
                    val s = file.readLine()
                    file.seek(p)
                    return s
                }
            } else {
                nonLineBreakFound = true
            }

            p--
            if (p <= 0) {
                file.seek(0)
                return file.readLine()
            }
        }
    }

    /**
     * Get the offset position of the cross reference section.
     */
    fun getStartXRefPosition(): Long {
        if (!isLinearized()) {
            if (startXRefPos == null) {
                var p = file.length() - 1
                while (true) {
                    var s = readContainingLine(p)
                    if (s.startsWith("startxref")) {
                        file.seek(file.filePointer + 1)
                        file.readLine()
                        while (true) {
                            s = file.readLine()
                            if (!s.startsWith("%")) {
                                startXRefPos = s.toLong()
                                return getValidXRefPos(startXRefPos as Long)
                            }
                        }
                    }
                    p = file.filePointer
                }
            } else return getValidXRefPos(startXRefPos as Long)
        } else {
            return getValidXRefPos(getStartXRefPositionLinearized())
        }
    }

    private fun getValidXRefPos(pos: Long): Long {
        file.seek(pos)
        var isXrefStream = false
        var foundXref = false
        while (file.filePointer + 3 < file.length()) {
            var c = file.readByte().toChar()
            if (c == 'X' || c == 'x') {
                isXrefStream = c == 'X'
                c = file.readByte().toChar()
                if (c == 'R' || c == 'r') {
                    c = file.readByte().toChar()
                    if (c == 'e') {
                        c = file.readByte().toChar()
                        if (c == 'f') {
                            foundXref = true
                            break
                        }
                    }
                }
            }
        }

        return if (!foundXref)
            throw InvalidDocumentException("Can not find valid cross reference table")
        else {
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
                    }
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
    fun getLastXRefData(): HashMap<String, XRefEntry> {
        val startXRef = getStartXRefPosition()
        return getXRefData(startXRef)
    }

    /**
     * Given the byte offset position of a cross reference section, parse all of its entries.
     *
     * @param pos Byte offset position of the cross reference section.
     *
     * @return a map of cross reference entries
     */
    fun getXRefData(pos: Long): HashMap<String, XRefEntry> {
        file.seek(pos)
        val s = file.readLine()
        return if (s.contains("xref")) {
            var data = parseXRefSection()
            data = parseOtherXRefInTrailer(file.filePointer, data)
            data
        } else {
            XRefStream(context, file, pos).parse()
        }
    }

    /**
     * Parse through each line of the cross reference section to get all of its entries. The offset position must
     * currently be in the beginning of the first subsection.
     */
    private fun parseXRefSection(): HashMap<String, XRefEntry> {
        val entries = HashMap<String, XRefEntry>()

        logd("Parsing XRef section start")
        val subSectionRegex = Regex("^\\s*(\\d+) (\\d+)\\s*$")
        while (true) {
            val p = file.filePointer
            // Find next subsection
            val s = file.readLine()
            if (s == "") continue
            if (!s.matches(subSectionRegex)) {
                // File pointer should be reset to right after the last entry
                file.seek(p)
                break
            }
            val subs = s.split(" ")
            val obj = subs.component1().toInt()
            val count = subs.component2().toInt()

            // Iterate through every entry and add to entries
            for (i in obj..(obj + count - 1)) {
                //logd("Parsing XRef entry for obj $i ")
                val e = file.readLine()
                val eFields = e.split(" ")
                val pos = eFields.component1().toLong()
                val gen = eFields.component2().toInt()
                val n = eFields.component3()

                if (n == "f") {
                    entries["$i $gen"] = XRefEntry(i, pos, gen, false)
                } else {
                    entries["$i $gen"] = XRefEntry(i, pos, gen)
                }
                //logd("${entries["$i $gen"]}")
            }
        }
        logd("Parsing XRef section end")
        return entries
    }

    private fun parseOtherXRefInTrailer(
        endXRefPos: Long,
        xRefEntries: HashMap<String, XRefEntry>
    ): HashMap<String, XRefEntry> {
        var entries = xRefEntries
        var p: Long
        var s: String
        file.seek(endXRefPos)
        do {
            p = file.filePointer
            s = file.readLine()
        } while (!s.startsWith("trailer"))

        val trailer = getDictionary(p, -1, 0, false)

        // Parse any existing cross reference stream
        val xRefStm = trailer["XRefStm"] as Numeric?
        if (xRefStm != null) {
            logd("XRefStm = ${xRefStm.value.toLong()}")
            val data = getXRefData((xRefStm.value.toLong()))
            data.putAll(entries)
            entries = data
        }

        // Parse any existing previous cross reference table
        val prev = trailer["Prev"] as Numeric?
        if (prev != null) {
            logd("Prev = ${prev.value.toLong()}")
            val data = getXRefData(prev.value.toLong())
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
    fun getTrailerPosition(): Long? {
        return if (trailerPos == null) {
            val startXRef = getStartXRefPosition()
            file.seek(startXRef)
            var s = file.readLine()
            if (s.contains("xref")) {
                parseXRefSection()
                var p: Long
                do {
                    p = file.filePointer
                    s = file.readLine()
                } while (!s.startsWith("trailer"))
                p
            } else {
                null
            }
        } else {
            trailerPos
        }
    }

    fun getTrailerEntries(resolveReferences: Boolean = true): HashMap<String, PDFObject?> {
        val trailerPos = getTrailerPosition()
        return if (trailerPos != null) {
            file.seek(trailerPos)
            val dictionary = getDictionary(file.filePointer, -1, 0, resolveReferences)
            if (resolveReferences) dictionary.resolveReferences()
            createTrailerHashMap(dictionary)
        } else {
            // Get trailer entries from XRefStream dictionary
            val xrefStm = XRefStream(context, file, getStartXRefPosition())
            if (resolveReferences) xrefStm.dictionary.resolveReferences()
            createTrailerHashMap(xrefStm.dictionary)
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

    fun getIndirectObject(pos: Long): Indirect {
        return Indirect(file, pos)
    }

    fun getDictionary(pos: Long, obj: Int, gen: Int, resolveReferences: Boolean = false): Dictionary {
        file.seek(pos)
        goToDictionaryStart()
        extractDictionary()
        return stringBuilder.toPDFObject(context, obj, gen, resolveReferences) as Dictionary
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

    private fun extractDictionary() {
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

    fun getObjectStream(pos: Long): ObjectStream {
        return ObjectStream(context, file, pos)
    }

    fun getStream(pos: Long): Stream {
        return Stream(context, file, pos)
    }
}