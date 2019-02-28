package marabillas.loremar.pdfparser

import java.io.RandomAccessFile

class Dictionary(file: RandomAccessFile, start: Long) {
    val entries = HashMap<String, String>()
    private val dictionaryParsingHelper = DictionaryParsingHelper()

    init {
        file.seek(start)
        var s = file.readLine()
        while (!s.contains("<<") || s.startsWith('%'))
            s = readLine()
        s = s.substringAfter("<<")

        while (true) {
            val sWithName = s.substringAfter('/', "")

            if (sWithName == "") {
                // Check if end of dictionary. If it is, then stop parsing.
                if (s.endsWith(">>")) break

                s = file.readLine()
                while (s.startsWith('%'))
                    s = readLine()
                continue
            } else s = sWithName

            val key = s.split(regex = "[()<>\\[\\]{}/% ]".toRegex())[0]
            s = s.substringAfter(key, "").trim()

            // Get the entry's value.
            var value: String
            while (true) {
                if (s != "" && startsEnclosed(s)) {
                    // Parse string object
                    val closeIndex = dictionaryParsingHelper.findIndexOfClosingDelimiter(s)
                    if (closeIndex != 0) {
                        value = s.substring(0, closeIndex + 1)
                        s = s.substring(closeIndex + 1, s.length)
                    } else value = ""
                } else {
                    if (s.trim().startsWith('/')) {
                        value = "/" + s.substringAfter('/').substringBefore('/')
                        value = value.substringBefore(' ')
                        value = value.substringBefore(">>")
                        s = s.substringAfter('/')
                    } else {
                        value = s.trimStart().substringBefore('/')
                        value = value.substringBefore(' ')
                        value = value.substringBefore(">>")

                        // Check if value ends at the end of line then maybe value continues on the next line
                        if (s.trimStart() == value) value = ""
                    }
                }

                if (value != "") {
                    entries[key] = value
                    break
                } else {
                    // Get the value in the next line.
                    s += file.readLine()
                }
            }
        }
    }

    private fun startsEnclosed(s: String): Boolean {
        return when (s.first()) {
            '(' -> true
            '[' -> true
            '<' -> true
            '{' -> true
            else -> false
        }
    }
}