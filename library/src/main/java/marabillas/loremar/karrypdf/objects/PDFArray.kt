package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.utils.exts.*

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

internal fun StringBuilder.toPDFArray(
    context: KarryPDFContext,
    secondary: StringBuilder,
    obj: Int,
    gen: Int,
    resolveReferences: Boolean = false
): PDFArray {
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
                val firstSpace = this.indexOfSpaceAfterNumberStartingAt(i)
                if (firstSpace != -1) {
                    val secondSpace = this.indexOfSpaceAfterNumberStartingAt(firstSpace + 1)
                    if (secondSpace != -1 && this[secondSpace + 1] == 'R') {
                        isReference = true
                        i = secondSpace + 2
                    }
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
        array.add(secondary.toPDFObject(context, obj, gen, resolveReferences))
        secondary.clear()
    }

    return PDFArray(array)
}

private fun StringBuilder.indexOfSpaceAfterNumberStartingAt(startIndex: Int): Int {
    var i = startIndex
    while (true) {
        if (this.outOfBoundsAt(i))
            return -1
        else if (i != startIndex && this.foundSpaceAt(i))
            return i
        else if (this[i].isDigit())
            i++
        else return -1
    }
}

private fun StringBuilder.outOfBoundsAt(i: Int): Boolean {
    return i >= this.length
}

private fun StringBuilder.foundSpaceAt(i: Int): Boolean {
    return this[i] == ' '
}