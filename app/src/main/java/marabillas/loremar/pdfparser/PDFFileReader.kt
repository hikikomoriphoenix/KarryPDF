package marabillas.loremar.pdfparser

import java.io.*

/**
 * This class facilitates reading in a pdf file.
 */
class PDFFileReader(private val file: RandomAccessFile) {
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
        var p = file.length() - 1
        while (true) {
            var s = readContainingLine(p)
            if (s.startsWith("startxref")) {
                file.seek(file.filePointer + 1)
                file.readLine()
                while (true) {
                    s = file.readLine()
                    if (!s.startsWith("%")) {
                        return s.toLong()
                    }
                }
            }
            p = file.filePointer
        }
    }

    /**
     * Locate the last cross reference section in the file and parse all of its entries into a collection
     *
     * @return a map of cross reference entries
     */
    fun getLastXRefData(): Map<String, XRefEntry> {
        val startXRef = getStartXRefPosition()
        file.seek(startXRef)
        val s = file.readLine()
        return if (s.startsWith("xref")) {
            parseXRefSection()
        } else {
            // TODO Locate cross reference stream and parse xref section.
            HashMap()
        }
    }

    /**
     * Parse through each line of the cross reference section to get all of its entries. The offset position must
     * currently be in the beginning of the first subsection.
     */
    private fun parseXRefSection(): Map<String, XRefEntry> {
        val entries = HashMap<String, XRefEntry>()

        while (true) {
            // Find next subsection
            val s = file.readLine()
            if (s == "") continue
            if (!s.matches(Regex("^(\\d+) (\\d+)$"))) break
            val subs = s.split(" ")
            val obj = subs.component1().toInt()
            val count = subs.component2().toInt()

            // Iterate through every entry and add to entries
            for (i in obj..(obj + count - 1)) {
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
            }
        }

        return entries
    }
}