package marabillas.loremar.karrypdf.objects

internal class Name(val string: String) : Any(), PDFObject {
    val value: String get() = string

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

internal fun String.toName(): Name {
    if (!this.startsWith("/")) throw IllegalArgumentException("Name object should start with '/'")
    return Name(this.substringAfter("/"))
}

internal fun StringBuilder.toName(): Name {
    if (!this.startsWith("/")) throw IllegalArgumentException("Name object should start with '/'")
    return Name(this.substring(1, this.length))
}