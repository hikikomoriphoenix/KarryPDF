package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.objects.Dictionary
import marabillas.loremar.pdfparser.objects.Indirect
import marabillas.loremar.pdfparser.objects.Numeric
import marabillas.loremar.pdfparser.objects.PDFObject
import java.io.RandomAccessFile

/**
 * This class facilitates reading in a pdf file.
 */
class PDFFileReader(private val file: RandomAccessFile) {
    private var startXRefPos: Long? = null
    private var trailerPos: Long? = null

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
            if (c =='\n' || c == '\r') {
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
                            return startXRefPos as Long
                        }
                    }
                }
                p = file.filePointer
            }
        } else return startXRefPos as Long
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
        return if (s.startsWith("xref")) {
            var data = parseXRefSection()
            data = parseOtherXRefInTrailer(file.filePointer, data)
            data
        } else {
            XRefStream(file, pos).parse()
        }
    }

    /**
     * Parse through each line of the cross reference section to get all of its entries. The offset position must
     * currently be in the beginning of the first subsection.
     */
    private fun parseXRefSection(): HashMap<String, XRefEntry> {
        val entries = HashMap<String, XRefEntry>()

        println("Parsing XRef section start")
        while (true) {
            val p = file.filePointer
            // Find next subsection
            val s = file.readLine()
            if (s == "") continue
            if (!s.matches(Regex("^(\\d+) (\\d+)$"))) {
                // File pointer should be reset to right after the last entry
                file.seek(p)
                break
            }
            val subs = s.split(" ")
            val obj = subs.component1().toInt()
            val count = subs.component2().toInt()

            // Iterate through every entry and add to entries
            for (i in obj..(obj + count - 1)) {
                print("Parsing XRef entry for obj $i ")
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
                println("${entries["$i $gen"]}")
            }
        }
        println("Parsing XRef section end")
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

        // Store the main trailer position.
        if (trailerPos == null) {
            trailerPos = p
        }

        val trailer = Dictionary(file, p)

        // Parse any existing cross reference stream
        val xRefStm = trailer["XRefStm"] as Numeric?
        if (xRefStm != null) {
            println("XRefStm = ${xRefStm.value.toLong()}")
            val data = getXRefData((xRefStm.value.toLong()))
            data.putAll(entries)
            entries = data
        }

        // Parse any existing previous cross reference table
        val prev = trailer["Prev"] as Numeric?
        if (prev != null) {
            println("Prev = ${prev.value.toLong()}")
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
            if (s.startsWith("xref")) {
                parseXRefSection()
                var p: Long
                do {
                    p = file.filePointer
                    s = file.readLine()
                } while (!s.startsWith("trailer"))
                p
            } else null
        } else trailerPos
    }

    fun getTrailerEntries(): HashMap<String, PDFObject?> {
        val trailerPos = getTrailerPosition()
        return if (trailerPos != null) {
            file.seek(trailerPos)
            val dictionary = Dictionary(file, file.filePointer).parse()
            createTrailerHashMap(dictionary)
        } else {
            // Get trailer entries from XRefStream dictionary
            val xrefStm = XRefStream(file, getStartXRefPosition())
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
}