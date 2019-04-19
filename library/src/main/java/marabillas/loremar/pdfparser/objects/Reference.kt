package marabillas.loremar.pdfparser.objects

import marabillas.loremar.pdfparser.toInt


internal class Reference(val obj: Int, val gen: Int) : PDFObject {
    companion object {
        val REGEX = "^\\d+ \\d+ R\$".toRegex()
    }

    fun resolve(referenceResolver: ReferenceResolver? = ObjectIdentifier.referenceResolver): PDFObject? {
        return referenceResolver?.resolveReference(this)
    }
}

internal fun String.toReference(): Reference {
    if (!(Reference.REGEX.matches(this))) throw IllegalArgumentException(
        "Given string does not match format for an indirect object reference"
    )

    val obj = this.substringBefore(' ').toInt()
    val gen = this.substringAfter(' ').substringBefore(' ').toInt()
    return Reference(obj, gen)
}

internal fun StringBuilder.toReference(secondary: StringBuilder): Reference {
    if (!(Reference.REGEX.matches(this))) throw IllegalArgumentException(
        "Given string does not match format for an indirect object reference"
    )

    val firstSpace = this.indexOf(' ')
    val secondSpace = this.indexOf(' ', firstSpace + 1)

    secondary.clear()
    secondary.append(this, 0, firstSpace)
    val obj = secondary.toInt()
    secondary.clear()
    secondary.append(this, firstSpace + 1, secondSpace)
    val gen = secondary.toInt()
    secondary.clear()

    return Reference(obj, gen)
}