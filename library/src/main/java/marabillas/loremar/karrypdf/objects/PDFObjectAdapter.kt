package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.utils.exts.containedEqualsWith
import marabillas.loremar.karrypdf.utils.exts.isEnclosedWith
import marabillas.loremar.karrypdf.utils.exts.isNumeric
import marabillas.loremar.karrypdf.utils.exts.trimContainedChars

internal class PDFObjectAdapter {
    companion object {
        private const val PDF_OBJECT_SECONDARY_STRING_BUILDER = "PDFObject Secondary StringBuilder"

        fun getPDFObject(
            context: KarryPDFContext,
            sb: StringBuilder,
            resolveReferences: Boolean = false,
            obj: Int,
            gen: Int
        ): PDFObject? {
            sb.trimContainedChars()

            if (sb == context.getStringBuilder(PDF_OBJECT_SECONDARY_STRING_BUILDER)) {
                context.setStringBuilder(PDF_OBJECT_SECONDARY_STRING_BUILDER, StringBuilder())
            }

            return when {
                sb.containedEqualsWith('t', 'r', 'u', 'e') -> PDFBoolean(true)
                sb.containedEqualsWith('f', 'a', 'l', 's', 'e') -> PDFBoolean(false)
                sb.isNumeric() -> sb.toNumeric()
                sb.isEnclosedWith('(', ')') -> sb.toPDFString()
                sb.isEnclosedWith("<<", ">>") ->
                    context.getStringBuilder(PDF_OBJECT_SECONDARY_STRING_BUILDER).let {
                        sb.toDictionary(
                            context,
                            it, obj, gen, resolveReferences
                        )
                    }
                sb.isEnclosedWith('<', '>') -> sb.toPDFString()
                sb.startsWith("/") -> sb.toName()
                sb.isEnclosedWith('[', ']') -> context
                    .getStringBuilder(PDF_OBJECT_SECONDARY_STRING_BUILDER).let {
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
                        context.getStringBuilder(PDF_OBJECT_SECONDARY_STRING_BUILDER).let {
                            sb.toReference(
                                context,
                                it
                            ).resolve(context)
                        }
                    } else {
                        context.getStringBuilder(PDF_OBJECT_SECONDARY_STRING_BUILDER).let {
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