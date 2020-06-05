package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.utils.exts.containedEqualsWith
import marabillas.loremar.karrypdf.utils.exts.isEnclosedWith
import marabillas.loremar.karrypdf.utils.exts.isNumeric
import marabillas.loremar.karrypdf.utils.exts.trimContainedChars

internal class PDFObjectAdapter {
    companion object {
        private val AUXILIARY_STRING_BUILDERS: MutableMap<KarryPDFContext.Session, StringBuilder> =
            mutableMapOf()

        fun notifyNewSession(session: KarryPDFContext.Session) {
            AUXILIARY_STRING_BUILDERS[session] = StringBuilder()
        }

        fun getPDFObject(
            context: KarryPDFContext,
            sb: StringBuilder,
            resolveReferences: Boolean = false,
            obj: Int,
            gen: Int
        ): PDFObject? {
            sb.trimContainedChars()

            if (sb == AUXILIARY_STRING_BUILDERS[context.session]) {
                AUXILIARY_STRING_BUILDERS[context.session] = StringBuilder()
            }

            return when {
                sb.containedEqualsWith('t', 'r', 'u', 'e') -> PDFBoolean(true)
                sb.containedEqualsWith('f', 'a', 'l', 's', 'e') -> PDFBoolean(false)
                sb.isNumeric() -> sb.toNumeric()
                sb.isEnclosedWith('(', ')') -> sb.toPDFString()
                sb.isEnclosedWith("<<", ">>") ->
                    AUXILIARY_STRING_BUILDERS[context.session]?.let {
                        sb.toDictionary(
                            context,
                            it, obj, gen, resolveReferences
                        )
                    }
                sb.isEnclosedWith('<', '>') -> sb.toPDFString()
                sb.startsWith("/") -> sb.toName()
                sb.isEnclosedWith('[', ']') -> AUXILIARY_STRING_BUILDERS[context.session]?.let {
                    sb.toPDFArray(
                        context,
                        it,
                        obj,
                        gen,
                        resolveReferences
                    )
                }
                Reference.isReference(sb) -> {
                    if (resolveReferences) {
                        AUXILIARY_STRING_BUILDERS[context.session]?.let {
                            sb.toReference(
                                context,
                                it
                            ).resolve(context)
                        }
                    } else {
                        AUXILIARY_STRING_BUILDERS[context.session]?.let {
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
    context: KarryPDFContext,
    obj: Int,
    gen: Int,
    resolveReferences: Boolean = false
): PDFObject? {
    return PDFObjectAdapter.getPDFObject(context, this, resolveReferences, obj, gen)
}