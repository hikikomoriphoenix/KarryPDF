package marabillas.loremar.pdfparser.objects

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.channels.Channels

class Dictionary : PDFObject {
    private val entries = HashMap<String, PDFObject?>()
    private var reader: BufferedReader = BufferedReader("".reader())

    constructor(file: RandomAccessFile, start: Long) {
        file.seek(start)
        val stream = Channels.newInputStream(file.channel)
        reader = BufferedReader(InputStreamReader(stream!!))
    }

    constructor(string: String) {
        reader = BufferedReader(string.reader())
    }

    fun parse(): Dictionary {
        var s: String = reader.readLine()

        while (!s.contains("<<") || s.startsWith('%'))
            s = reader.readLine()
        s = s.substringAfter("<<")

        while (true) {
            val sWithName = s.substringAfter('/', "")

            if (sWithName == "") {
                // Check if end of dictionary. If it is, then stop parsing.
                if (s.endsWith(">>")) break

                s = reader.readLine()
                while (s.startsWith('%'))
                    s = reader.readLine()
                continue
            } else s = sWithName

            val key = s.split(regex = "[()<>\\[\\]{}/% ]".toRegex())[0]
            s = s.substringAfter(key, "").trim()

            // Get the entry's value.
            var value: String
            while (true) {
                if (s != "" && s.startsEnclosed()) {
                    value = s.extractEnclosedObject()
                    s = s.substringAfter(value)
                } else {
                    if (s.trim().startsWith('/')) {
                        value = "/" + s.substringAfter('/').substringBefore('/')
                        value = value.substringBefore(">>").trim()
                        s = s.substringAfter('/')
                    } else {
                        value = s.trimStart().substringBefore('/')
                        value = value.substringBefore(">>").trim()

                        // Check if value ends at the end of line then maybe value continues on the next line
                        if (s.trimStart() == value) value = ""
                    }
                }

                if (value != "") {
                    entries[key] = value.toPDFObject()
                    break
                } else {
                    // Get the value in the next line.
                    s += reader.readLine()
                }
            }
        }

        return this
    }

    operator fun get(entry: String): PDFObject? {
        return entries[entry]
    }
}

fun String.toDictionary(): Dictionary {
    return Dictionary(this).parse()
}