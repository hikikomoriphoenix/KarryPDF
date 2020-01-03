package marabillas.loremar.andpdf.objects

import marabillas.loremar.andpdf.document.AndPDFContext
import marabillas.loremar.andpdf.utils.exts.containedEqualsWith
import marabillas.loremar.andpdf.utils.exts.isEnclosedWith
import marabillas.loremar.andpdf.utils.exts.trimContainedChars

internal class PDFObjectAdapter {
    companion object {
        private val NUMERIC_PATTERN = "-?\\d*.?\\d+".toRegex()
        private var auxiliaryStringBuilder = StringBuilder()

        fun getPDFObject(
            context: AndPDFContext,
            sb: StringBuilder,
            resolveReferences: Boolean = false,
            obj: Int,
            gen: Int
        ): PDFObject? {
            sb.trimContainedChars()

            if (sb == auxiliaryStringBuilder) {
                auxiliaryStringBuilder = StringBuilder()
            }

            return when {
                sb.containedEqualsWith('t', 'r', 'u', 'e') -> PDFBoolean(true)
                sb.containedEqualsWith('f', 'a', 'l', 's', 'e') -> PDFBoolean(false)
                NUMERIC_PATTERN.matches(sb) -> sb.toNumeric()
                sb.isEnclosedWith('(', ')') -> sb.toPDFString()
                sb.isEnclosedWith(arrayOf('<', '<'), arrayOf('>', '>')) ->
                    sb.toDictionary(context, auxiliaryStringBuilder, obj, gen, resolveReferences)
                sb.isEnclosedWith('<', '>') -> sb.toPDFString()
                sb.startsWith("/") -> sb.toName()
                sb.isEnclosedWith('[', ']') -> sb.toPDFArray(
                    context,
                    auxiliaryStringBuilder,
                    obj,
                    gen,
                    resolveReferences
                )
                Reference.REGEX.matches(sb) -> {
                    if (resolveReferences) {
                        sb.toReference(context, auxiliaryStringBuilder).resolve(context)
                    } else {
                        sb.toReference(context, auxiliaryStringBuilder)
                    }
                }

                else -> null
            }
        }
    }
}

internal fun StringBuilder.toPDFObject(
    context: AndPDFContext,
    obj: Int,
    gen: Int,
    resolveReferences: Boolean = false
): PDFObject? {
    return PDFObjectAdapter.getPDFObject(context, this, resolveReferences, obj, gen)
}