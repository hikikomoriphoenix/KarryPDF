package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.document.ReferenceResolver
import marabillas.loremar.karrypdf.utils.exts.toInt


internal class Reference(var context: KarryPDFContext, val obj: Int, val gen: Int) : PDFObject {
    companion object {
        val REGEX = "^\\d+ \\d+ R\$".toRegex()
    }

    fun resolve(referenceResolver: ReferenceResolver? = context): PDFObject? {
        return referenceResolver?.resolveReference(this)
    }

    fun resolveToStream(referenceResolver: ReferenceResolver? = context): Stream? {
        return referenceResolver?.resolveReferenceToStream(this)
    }

    override fun toString(): String {
        return "$obj $gen R"
    }
}

internal fun String.toReference(context: KarryPDFContext): Reference {
    if (!(Reference.REGEX.matches(this))) throw IllegalArgumentException(
        "Given string does not match format for an indirect object reference"
    )

    val obj = this.substringBefore(' ').toInt()
    val gen = this.substringAfter(' ').substringBefore(' ').toInt()
    return Reference(context, obj, gen)
}

internal fun StringBuilder.toReference(
    context: KarryPDFContext,
    secondary: StringBuilder
): Reference {
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

    return Reference(context, obj, gen)
}