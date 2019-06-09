package marabillas.loremar.pdfparser.objects

import marabillas.loremar.pdfparser.utils.exts.containedEqualsWith
import marabillas.loremar.pdfparser.exceptions.NoReferenceResolverException
import marabillas.loremar.pdfparser.utils.exts.isEnclosedWith
import marabillas.loremar.pdfparser.utils.exts.trimContainedChars

internal class PDFObjectAdapter {
    companion object {
        var referenceResolver: ReferenceResolver? = null
        private val NUMERIC_PATTERN = "-?\\d*.?\\d+".toRegex()
        private val auxiliaryStringBuilder = StringBuilder()

        fun getPDFObject(sb: StringBuilder, resolverReferences: Boolean = false): PDFObject? {
            sb.trimContainedChars()

            return when {
                sb.containedEqualsWith('t', 'r', 'u', 'e') -> PDFBoolean(true)
                sb.containedEqualsWith('f', 'a', 'l', 's', 'e') -> PDFBoolean(false)
                NUMERIC_PATTERN.matches(sb) -> sb.toNumeric()
                sb.isEnclosedWith('(', ')') -> sb.toPDFString()
                sb.isEnclosedWith(arrayOf('<', '<'), arrayOf('>', '>')) ->
                    sb.toDictionary(auxiliaryStringBuilder, resolverReferences)
                sb.isEnclosedWith('<', '>') -> sb.toPDFString()
                sb.startsWith("/") -> sb.toName()
                sb.isEnclosedWith('[', ']') -> sb.toPDFArray(auxiliaryStringBuilder, resolverReferences)
                Reference.REGEX.matches(sb) -> {
                    if (resolverReferences) {
                        if (referenceResolver == null)
                            throw NoReferenceResolverException()
                        else
                            sb.toReference(auxiliaryStringBuilder).resolve(referenceResolver)
                    } else {
                        sb.toReference(auxiliaryStringBuilder)
                    }
                }

                else -> null
            }
        }
    }
}

internal fun StringBuilder.toPDFObject(resolveReferences: Boolean = false): PDFObject? {
    return PDFObjectAdapter.getPDFObject(this, resolveReferences)
}