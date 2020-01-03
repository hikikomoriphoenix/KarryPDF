package marabillas.loremar.andpdf.objects

import marabillas.loremar.andpdf.document.AndPDFContext
import marabillas.loremar.andpdf.utils.exts.containedEqualsWith
import marabillas.loremar.andpdf.utils.exts.isEnclosedWith
import marabillas.loremar.andpdf.utils.exts.trimContainedChars

internal class PDFObjectAdapter {
    companion object {
        private val NUMERIC_PATTERN = "-?\\d*.?\\d+".toRegex()
        private val auxiliaryStringBuilders: MutableMap<AndPDFContext.Session, StringBuilder> =
            mutableMapOf()

        fun notifyNewSession(session: AndPDFContext.Session) {
            auxiliaryStringBuilders[session] = StringBuilder()
        }

        fun getPDFObject(
            context: AndPDFContext,
            sb: StringBuilder,
            resolveReferences: Boolean = false,
            obj: Int,
            gen: Int
        ): PDFObject? {
            sb.trimContainedChars()

            if (sb == auxiliaryStringBuilders[context.session]) {
                auxiliaryStringBuilders[context.session] = StringBuilder()
            }

            return when {
                sb.containedEqualsWith('t', 'r', 'u', 'e') -> PDFBoolean(true)
                sb.containedEqualsWith('f', 'a', 'l', 's', 'e') -> PDFBoolean(false)
                NUMERIC_PATTERN.matches(sb) -> sb.toNumeric()
                sb.isEnclosedWith('(', ')') -> sb.toPDFString()
                sb.isEnclosedWith(arrayOf('<', '<'), arrayOf('>', '>')) ->
                    auxiliaryStringBuilders[context.session]?.let {
                        sb.toDictionary(
                            context,
                            it, obj, gen, resolveReferences
                        )
                    }
                sb.isEnclosedWith('<', '>') -> sb.toPDFString()
                sb.startsWith("/") -> sb.toName()
                sb.isEnclosedWith('[', ']') -> auxiliaryStringBuilders[context.session]?.let {
                    sb.toPDFArray(
                        context,
                        it,
                        obj,
                        gen,
                        resolveReferences
                    )
                }
                Reference.REGEX.matches(sb) -> {
                    if (resolveReferences) {
                        auxiliaryStringBuilders[context.session]?.let {
                            sb.toReference(
                                context,
                                it
                            ).resolve(context)
                        }
                    } else {
                        auxiliaryStringBuilders[context.session]?.let {
                            sb.toReference(
                                context,
                                it
                            )
                        }
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