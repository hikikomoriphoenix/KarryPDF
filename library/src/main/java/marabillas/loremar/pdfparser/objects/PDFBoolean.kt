package marabillas.loremar.pdfparser.objects

internal class PDFBoolean : PDFObject {
    var value = false
        private set

    constructor(string: String) {
        value = when (string) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Give string ")
        }
    }

    constructor(bool: Boolean) {
        value = bool
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

internal fun String.toPDFBoolean(): PDFBoolean {
    return PDFBoolean(this)
}