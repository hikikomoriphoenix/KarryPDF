package marabillas.loremar.andpdf.document

import java.io.RandomAccessFile

internal class TopDownParser(private val file: RandomAccessFile) {
    fun parseObjects(): HashMap<String, XRefEntry> {
        val objects = HashMap<String, XRefEntry>()
        file.seek(0)
        while (file.filePointer + 2 < file.length()) {
            var c = file.readByte().toChar()
            if (c == 'o') {
                c = file.readByte().toChar()
                if (c == 'b') {
                    c = file.readByte().toChar()
                    if (c == 'j') {
                        extractObj(objects)
                    }
                }
            }
        }
        return objects
    }

    private fun extractObj(objects: HashMap<String, XRefEntry>) {
        val continuePosition = file.filePointer
        file.seek(file.filePointer - 4)

        var skipSpace = skipSpace(continuePosition)
        if (!skipSpace) return

        val gen = extractNumber()
        file.seek(file.filePointer - 1)

        skipSpace = skipSpace(continuePosition)
        if (!skipSpace) return

        val obj = extractNumber()

        objects["$obj $gen"] = XRefEntry(obj = obj, gen = gen, pos = file.filePointer)
        file.seek(continuePosition)
    }

    private fun skipSpace(continuePosition: Long): Boolean {
        var haveSpace = false
        while (true) {
            val c = file.readByte().toChar()
            if (c != ' ') {
                return if (haveSpace && c.isDigit()) {
                    file.seek(file.filePointer - 1)
                    true
                } else {
                    file.seek(continuePosition)
                    false
                }
            } else {
                haveSpace = true
            }
            file.seek(file.filePointer - 2)
        }
    }

    private fun extractNumber(): Int {
        var number = 0
        var factor = 1
        while (true) {
            val c = file.readByte().toChar()
            if (c.isDigit()) {
                val num = Character.getNumericValue(c)
                number += (num * factor)
                factor *= 10
            } else {
                return number
            }
            file.seek(file.filePointer - 2)
        }
    }
}