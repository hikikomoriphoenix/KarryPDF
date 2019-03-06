package marabillas.loremar.pdfparser.objects

import marabillas.loremar.pdfparser.filters.DecoderFactory

class PDFString(private var string: String) {
    var value = ""
        private set

    init {
        when {
            string.startsWith("(") && string.endsWith(")") -> {
                value = string.substringAfter("(").substringBeforeLast(")")
            }
            string.startsWith("<") && string.endsWith(">") -> {
                string = string.substringAfter("<").substringBeforeLast(">")
                if (string.length % 2 != 0) string += "0"

                val decoder = DecoderFactory().getDecoder("ASCIIHex")
                value = decoder.decode(string).toString()
            }
            else -> throw IllegalArgumentException("A PDF string object should either be enclosed in () or <>.")
        }
    }

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

fun String.toPDFString(): PDFString {
    return PDFString(this)
}