package marabillas.loremar.pdfparser.objects

class PDFBoolean(string: String) {
    var value = false
        private set

    init {
        value = when (string) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Give string ")
        }
    }

    override fun equals(other: Any?): Boolean {
        return value.equals(other)
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }
}

fun String.toPDFBoolean(): PDFBoolean {
    return PDFBoolean(this)
}