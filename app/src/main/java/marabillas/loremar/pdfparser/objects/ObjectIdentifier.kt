package marabillas.loremar.pdfparser.objects

class ObjectIdentifier {
    companion object {
        fun processString(string: String?): PDFObject? {
            return when {
                string == "true" || string == "false" -> string.toPDFBoolean()
                string?.isNumeric() ?: false -> string?.toNumeric()
                string?.isEnclosedWith("(", ")") ?: false -> string?.toPDFString()
                string?.isEnclosedWith("<<", ">>") ?: false -> string?.toDictionary()
                string?.isEnclosedWith("<", ">") ?: false -> string?.toPDFString()
                string?.startsWith("/") ?: false -> string?.toName()
                string?.isEnclosedWith("[", "]") ?: false -> string?.toArray()
                (string?.let { "^\\d+ \\d+ R$".toRegex().matches(it) }) ?: false -> string?.toReference()

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

fun String.toPDFObject(): PDFObject? {
    return ObjectIdentifier.processString(this)
}

