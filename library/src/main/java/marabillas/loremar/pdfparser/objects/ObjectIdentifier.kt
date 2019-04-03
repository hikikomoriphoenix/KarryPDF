package marabillas.loremar.pdfparser.objects

import marabillas.loremar.pdfparser.exceptions.NoReferenceResolverException

internal class ObjectIdentifier {
    companion object {
        var referenceResolver: ReferenceResolver? = null

        fun processString(string: String?, resolverReferences: Boolean = false): PDFObject? {
            val s = string?.trim() ?: ""
            return when {
                s == "true" || s == "false" -> s.toPDFBoolean()
                s.isNumeric() -> s.toNumeric()
                s.isEnclosedWith("(", ")") -> s.toPDFString()
                s.isEnclosedWith("<<", ">>") -> s.toDictionary(resolverReferences)
                s.isEnclosedWith("<", ">") -> s.toPDFString()
                s.startsWith("/") -> s.toName()
                s.isEnclosedWith("[", "]") -> s.toPDFArray(resolverReferences)
                (s.let { "^\\d+ \\d+ R$".toRegex().matches(it) }) -> {
                    if (resolverReferences) {
                        if (referenceResolver == null)
                            throw NoReferenceResolverException()
                        else
                            s.toReference().resolve(referenceResolver)
                    } else {
                        s.toReference()
                    }
                }

                else -> null
            }
        }

        private fun String.isNumeric(): Boolean {
            return try {
                this.toNumeric()
                true
            } catch (e: NumberFormatException) {
                false
            }
        }

        private fun String.isEnclosedWith(open: String, close: String): Boolean {
            return this.startsWith(open) && this.endsWith(close)
        }
    }

}

internal fun String.toPDFObject(resolveReferences: Boolean = false): PDFObject? {
    return ObjectIdentifier.processString(this, resolveReferences)
}

