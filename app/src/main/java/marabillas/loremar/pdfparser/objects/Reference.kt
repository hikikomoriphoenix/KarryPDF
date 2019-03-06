package marabillas.loremar.pdfparser.objects

class Reference(string: String) {
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
}

fun String.toReference(): Reference {
    return Reference(this)
}