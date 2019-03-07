package marabillas.loremar.pdfparser.objects

class Name(string: String) : Any(), PDFObject {
    val value = string.substringAfter("/")

    init {
        if (!string.startsWith("/")) throw IllegalArgumentException("Name object should start with '/'")
    }

    override fun toString(): String {
        return value
    }

    override operator fun equals(other: Any?): Boolean {
        return value == other
    }

    operator fun plus(other: String): String {
        return value + other
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

fun String.toName(): Name {
    return Name(this)
}