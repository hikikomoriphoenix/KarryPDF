package marabillas.loremar.pdfparser.objects

import marabillas.loremar.pdfparser.utils.exts.indexOfClosingChar
import marabillas.loremar.pdfparser.utils.exts.isEnclosingAt
import marabillas.loremar.pdfparser.utils.exts.isWhiteSpaceAt

internal class Dictionary(private val entries: HashMap<String, PDFObject?>) : PDFObject {
    operator fun get(entry: String): PDFObject? {
        return entries[entry]
    }

    fun getKeys(): Set<String> {
        return entries.keys
    }

    fun resolveReferences(): Dictionary {
        entries.asSequence()
            .filter { it.value is Reference }
            .forEach {
                val resolved = (it.value as Reference).resolve()
                entries[it.key] = resolved
            }
        return this
    }
}

internal fun String.toDictionary(resolveReferences: Boolean = false): Dictionary {
    val entries = HashMap<String, PDFObject?>()
    var s = this
    while (true) {
        s = s.substringAfter("/", "")
        if (s == "") break
        val key = s.split(regex = "[()<>\\[\\]{}/% ]".toRegex())[0]
        s = s.substringAfter(key).trim()

        val value = when {
            s.startsEnclosed() -> s.extractEnclosedObject()
            s.startsWith("/") -> "/${s.substringAfter("/").substringBefore("/")
                .substringBefore(">>").trim()}"
            else -> s.substringBefore("/").substringBefore(">>").trim()
        }
        s = s.substringAfter(value)
        entries[key] = value.toPDFObject(resolveReferences)
    }
    return Dictionary(entries)
}

internal fun StringBuilder.toDictionary(secondary: StringBuilder, resolveReferences: Boolean = false): Dictionary {
    val entries = HashMap<String, PDFObject?>()
    var i = this.indexOf('/')
    i++
    while (i < this.length) {
        // Locate next key position and length
        val keyIndex = i
        var keyLength = 1
        while (i < this.length) {
            val c = this[i]
            if (c == ' ' || c == '/' || c == '(' || c == '<' || c == '[' || c == '{' || c == '\n' || c == '\r') {
                keyLength = i - keyIndex
                break
            }
            i++
        }

        if (i >= this.length) break

        // Skip whitespaces
        while (this.isWhiteSpaceAt(i) && i < this.length)
            i++

        if (i >= this.length) break

        // Locate next value position and length
        val valueIndex = i
        if (this.isEnclosingAt(i))
            i = this.indexOfClosingChar(i)
        i = this.indexOf('/', i)
        var valueLength: Int
        if (i == -1) {
            valueLength = this.length - valueIndex
            i = this.length
        } else {
            valueLength = i - valueIndex
        }

        // Store value in a StringBuilder and remove whitespaces and closing '>> for dictionary.
        secondary.clear()
        secondary.append(this, valueIndex, valueIndex + valueLength)
        var hasClosing = true
        while (true) {
            if (secondary.last() == ' ' || secondary.last() == '\n' || secondary.last() == '\r')
                secondary.deleteCharAt(secondary.lastIndex)
            else if (hasClosing && secondary.last() == '>' && secondary[secondary.lastIndex - 1] == '>') {
                secondary.delete(secondary.lastIndex - 1, secondary.length)
                hasClosing = false
            } else break
        }

        // Add entry
        entries[this.substring(keyIndex, keyIndex + keyLength)] = secondary.toPDFObject(resolveReferences)

        i++
    }
    secondary.clear()
    return Dictionary(entries)
}