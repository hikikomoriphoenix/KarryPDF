package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.document.ReferenceResolver
import marabillas.loremar.karrypdf.utils.exts.toInt


internal class Reference(var context: KarryPDFContext, val obj: Int, val gen: Int) : PDFObject {
    var value: PDFObject? = null

    fun resolve(referenceResolver: ReferenceResolver? = context): PDFObject? {
        if (value == null)
            value = referenceResolver?.resolveReference(this)
        return value
    }

    fun resolveToStream(referenceResolver: ReferenceResolver? = context): Stream? {
        return referenceResolver?.resolveReferenceToStream(this)
    }

    override fun toString(): String {
        return "$obj $gen R"
    }

    companion object {
        fun isReference(sb: StringBuilder): Boolean {
            var i = 0
            // Check first number
            var hasFirstNumber = false
            while (true) {
                if (i >= sb.length) return false
                if (sb[i] == ' ') {
                    if (hasFirstNumber) break else return false
                }
                if (!Character.isDigit(sb[i])) return false else hasFirstNumber = true
                i++
            }
            i++
            // Check second number
            var hasSecondNumber = false
            while (true) {
                if (i >= sb.length) return false
                if (sb[i] == ' ') {
                    if (hasSecondNumber) break else return false
                }
                if (!Character.isDigit(sb[i])) return false else hasSecondNumber = true
                i++
            }
            // Check for R
            if (sb[++i] != 'R') return false
            if (i != sb.lastIndex) return false

            return true
        }
    }
}

@Deprecated("Use StringBuilder.toReference instead")
internal fun String.toReference(context: KarryPDFContext): Reference {
    if (!Reference.isReference(StringBuilder())) throw IllegalArgumentException(
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
    if (!Reference.isReference(this)) throw IllegalArgumentException(
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