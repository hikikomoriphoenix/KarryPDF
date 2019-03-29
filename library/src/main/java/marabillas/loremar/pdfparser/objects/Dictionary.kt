package marabillas.loremar.pdfparser.objects

internal class Dictionary(private val string: String) : PDFObject {
    private val entries = HashMap<String, PDFObject?>()

    fun parse(resolveReferences: Boolean = true): Dictionary {
        var s = string
        while (true) {
            s = s.substringAfter("/", "")
            if (s == "") break
            val key = s.split(regex = "[()<>\\[\\]{}/% ]".toRegex())[0]
            s = s.substringAfter(key).trim()

            val value = when {
                s.startsEnclosed() -> s.extractEnclosedObject()
                s.startsWith("/") -> "/${s.substringAfter("/").substringBefore("/")
                    .substringBefore(">>").trim()}"
                else -> s.substringBefore("/").substringBefore(">>").trim()
            }
            s = s.substringAfter(value)
            entries[key] = value.toPDFObject(resolveReferences)
        }
        return this
    }

    operator fun get(entry: String): PDFObject? {
        return entries[entry]
    }

    fun resolveReferences(): Dictionary {
        entries.asSequence()
            .filter { it.value is Reference }
            .forEach {
                val resolved = (it.value as Reference).resolve()
                entries[it.key] = resolved
            }
        return this
    }
}

internal fun String.toDictionary(resolveReferences: Boolean = false): Dictionary {
    return Dictionary(this).parse(resolveReferences)
}