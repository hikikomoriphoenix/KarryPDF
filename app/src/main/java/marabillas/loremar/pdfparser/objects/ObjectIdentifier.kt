package marabillas.loremar.pdfparser.objects

import marabillas.loremar.pdfparser.exceptions.NoReferenceResolverException

class ObjectIdentifier {
    companion object {
        var referenceResolver: ReferenceResolver? = null

        fun processString(string: String?, resolverReferences: Boolean = false): PDFObject? {
            return when {
                string == "true" || string == "false" -> string.toPDFBoolean()
                string?.isNumeric() ?: false -> string?.toNumeric()
                string?.isEnclosedWith("(", ")") ?: false -> string?.toPDFString()
                string?.isEnclosedWith("<<", ">>") ?: false -> string?.toDictionary(resolverReferences)
                string?.isEnclosedWith("<", ">") ?: false -> string?.toPDFString()
                string?.startsWith("/") ?: false -> string?.toName()
                string?.isEnclosedWith("[", "]") ?: false -> string?.toArray(resolverReferences)
                (string?.let { "^\\d+ \\d+ R$".toRegex().matches(it) }) ?: false -> {
                    if (resolverReferences) {
                        referenceResolver?.let {
                            string?.toReference()?.resolve(
                                it
                            ) ?: throw NoReferenceResolverException()
                        }
                    } else {
                        string?.toReference()
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

fun String.toPDFObject(resolveReferences: Boolean = false): PDFObject? {
    return ObjectIdentifier.processString(this, resolveReferences)
}

