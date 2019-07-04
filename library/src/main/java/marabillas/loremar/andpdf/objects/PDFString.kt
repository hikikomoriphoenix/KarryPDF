package marabillas.loremar.andpdf.objects

import marabillas.loremar.andpdf.filters.DecoderFactory
import marabillas.loremar.andpdf.utils.exts.resolveEscapedSequences
import java.math.BigInteger

internal class PDFString(val original: String, val value: String) : Any(), PDFObject {
    override fun toString(): String {
        return value
    }

    override operator fun equals(other: Any?): Boolean {
        return value == other
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    operator fun plus(other: String): String {
        return value + other
    }
}

internal fun String.toPDFString(): PDFString {
    when {
        this.startsWith("(") && this.endsWith(")") -> {
            var s = this.substringAfter("(").substringBeforeLast(")")

            // Convert octal character codes to their proper character representations
            "\\\\\\d{1,3}"
                .toRegex()
                .findAll(s)
                .associateBy( // Create a map where key=character code and value=character representation of code
                    { it.value },
                    {
                        // Get the bytes equivalent of given octal characters and convert to string.
                        val b = BigInteger(it.value.substringAfter("\\"), 8)
                        String(b.toByteArray())
                    })
                .forEach {
                    // Replace all occurrences of character code with its character representation
                    s = s.replace(it.key, it.value)
                }
            return PDFString(this, s)
        }
        this.startsWith("<") && this.endsWith(">") -> {
            var s = this.substringAfter("<").substringBeforeLast(">")
            if (s.length % 2 != 0) s += "0"

            val decoder = DecoderFactory().getDecoder("ASCIIHexDecode")
            val bytes = decoder.decode(s.toByteArray())
            s = String(bytes, Charsets.UTF_16)
            return PDFString(this, s)
        }
        else -> throw IllegalArgumentException("A PDF string object should either be enclosed in () or <>.")
    }
}

internal fun StringBuilder.toPDFString(): PDFString {
    when {
        this.startsWith('(') && this.endsWith(')') -> {
            val new = this.lastIndex + 1
            this.append(this)

            // Delete enclosing parentheses
            this.delete(new, new + 1)
            this.delete(this.lastIndex, this.length)

            this.resolveEscapedSequences(new)

            // original = substring containing original string
            // value = substring containing modified string with deleted enclosing parentheses
            return PDFString(
                this.substring(0, new),
                this.substring(new, this.length)
            )
        }
        this.startsWith('<') && this.endsWith('>') -> {
            val new = this.lastIndex + 1
            this.append(this)

            // Delete enclosing '<' and '>'
            this.delete(new, new + 1)
            this.delete(this.lastIndex, this.length)

            if ((this.length - new) % 2 != 0) this.append('0')

            var i = new
            while (i <= this.lastIndex) {
                if (this[i] == ' ' || this[i] == '\n' || this[i] == '\r') {
                    i++
                    continue
                }
                val first = Character.getNumericValue(this[i])
                val second = Character.getNumericValue(this[i + 1])
                val value = first * 16 + second
                this.delete(i, i + 2)
                this.insert(i, value.toChar())
                i++
            }

            // original = substring containing original string
            // value = substring containing modified string with deleted enclosing '<' and '>'
            return PDFString(
                this.substring(0, new),
                this.substring(new, this.length)
            )
        }
        else -> throw IllegalArgumentException("A PDF string object should either be enclosed in () or <>.")
    }
}