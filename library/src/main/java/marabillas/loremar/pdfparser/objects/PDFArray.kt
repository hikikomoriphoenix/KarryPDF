package marabillas.loremar.pdfparser.objects

import marabillas.loremar.pdfparser.utils.exts.*
import marabillas.loremar.pdfparser.utils.exts.indexOfClosingChar
import marabillas.loremar.pdfparser.utils.exts.isEnclosingAt
import marabillas.loremar.pdfparser.utils.exts.trimContainedChars
import marabillas.loremar.pdfparser.utils.exts.trimEndOfContainedChars

internal class PDFArray(val array: ArrayList<PDFObject?>) : PDFObject, Iterable<PDFObject?> {
    operator fun get(i: Int): PDFObject? {
        return array[i]
    }

    override fun iterator(): Iterator<PDFObject?> {
        return array.iterator()
    }

    override fun toString(): String {
        val sb = StringBuilder("[")
        array.forEach {
            sb.append(" ${it.toString()} ")
        }
        sb.append("]")
        return sb.toString()
    }

    fun resolveReferences(): PDFArray {
        array.asSequence()
            .filter { it is Reference }
            .forEachIndexed { i, it ->
                val resolved = (it as Reference).resolve()
                array[i] = resolved
            }
        return this
    }
}

internal fun String.toPDFArray(resolveReferences: Boolean = false): PDFArray {
    val array = ArrayList<PDFObject?>()
    if (!this.startsWith("[") || !this.endsWith("]")) throw IllegalArgumentException(
        "An array object needs to be enclosed in []"
    )

    var content = this.substringAfter("[").substringBeforeLast("]")

    while (true) {
        content = content.trim()
        if (content == "") break

        var entry: String
        when {
            content.startsEnclosed() -> entry = content.extractEnclosedObject()
            content.startsWith("/") -> {
                entry = content.substringAfter("/")
                entry = entry.split(regex = "[()<\\[{/ ]".toRegex())[0]
                entry = "/$entry"
            }
            else -> {
                entry = content.split(regex = "[()<\\[{/ ]".toRegex())[0]
                if (entry == "R") {
                    val a = (array[array.lastIndex - 1] as Numeric).value.toInt()
                    val b = (array[array.lastIndex] as Numeric).value.toInt()
                    val ref = "$a $b R"
                    if ("^\\d+ \\d+ R$".toRegex().matches(ref)) {
                        repeat(2) { array.removeAt(array.lastIndex) }
                        entry = ref
                        content = "$a $b $content"
                    }
                }
            }
        }

        content = content.substringAfter(entry)
        if (entry == "") break
        array.add(entry.toPDFObject(resolveReferences))
    }
    return PDFArray(array)
}

internal fun StringBuilder.toPDFArray(secondary: StringBuilder, resolveReferences: Boolean = false): PDFArray {
    val array = ArrayList<PDFObject?>()

    // Remove enclosing '[' and ']'
    val open = this.indexOf('[')
    this.deleteCharAt(open)
    val close = this.lastIndexOf(']')
    this.deleteCharAt(close)

    this.trimContainedChars()

    var i = 0
    while (i < this.length) {
        // Skip whitespaces
        while (this.isWhiteSpaceAt(i) && i < this.length)
            i++

        if (i >= this.length) break

        // Locate next entry position and length
        val entryIndex = i
        if (this.isEnclosingAt(i)) {
            i = this.indexOfClosingChar(i)
            i++
        } else {
            var isReference = false
            // Check for a Reference Object.
            if (this[i].isDigit()) {
                val firstSpace = this.indexOf(' ', i)
                val secondSpace = this.indexOf(' ', firstSpace + 1)
                if (this[secondSpace + 1] == 'R') {
                    isReference = true
                    i = secondSpace + 2
                }
            }

            if (!isReference) {
                if (this[i] == '/') {
                    // Skip '/'
                    i++
                }
                // Locate next delimiter.
                while (i <= this.length) {
                    if (i == this.length || this.isWhiteSpaceAt(i) || this.isEnclosingAt(i) || this[i] == '/') {
                        break
                    } else {
                        i++
                    }
                }
            }
        }
        secondary.clear()
        secondary.append(this, entryIndex, i)
        secondary.trimEndOfContainedChars()
        array.add(secondary.toPDFObject(resolveReferences))
        secondary.clear()
    }

    return PDFArray(array)
}