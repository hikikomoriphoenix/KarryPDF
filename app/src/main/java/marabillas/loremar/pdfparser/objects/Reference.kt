package marabillas.loremar.pdfparser.objects

internal class Reference(string: String) : PDFObject {
    var obj: Int? = null
        private set
    var gen: Int = 0
        private set

    init {
        if (!("^\\d+ \\d+ R$".toRegex().matches(string))) throw IllegalArgumentException(
            "Given " +
                    "string does not match format for an indirect object reference"
        )

        obj = string.substringBefore(' ').toInt()
        gen = string.substringAfter(' ').substringBefore(' ').toInt()
    }

    fun resolve(referenceResolver: ReferenceResolver? = ObjectIdentifier.referenceResolver): PDFObject? {
        return referenceResolver?.resolveReference(this)
    }
}

internal fun String.toReference(): Reference {
    return Reference(this)
}