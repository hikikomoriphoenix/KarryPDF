package marabillas.loremar.pdfparser.objects

internal class Array(private val arrayString: String) : PDFObject, Iterable<PDFObject?> {
    private val array = ArrayList<PDFObject?>()

    fun parse(resolveReferences: Boolean = true): Array {
        if (!arrayString.startsWith("[") || !arrayString.endsWith("]")) throw IllegalArgumentException(
            "An array object needs to be enclosed in []"
        )

        var content = arrayString.substringAfter("[").substringBeforeLast("]")

        while (true) {
            content = content.trim()
            if (content == "") break

            var entry: String
            when {
                content.startsEnclosed() -> entry = content.extractEnclosedObject()
                content.startsWith("/") -> {
                    entry = content.substringAfter("/")
                    entry = entry.split(regex = "[()<\\[{/ ]".toRegex())[0]
                    entry = "/$entry"
                }
                else -> {
                    entry = content.split(regex = "[()<\\[{/ ]".toRegex())[0]
                    if (entry == "R") {
                        val a = (array[array.lastIndex - 1] as Numeric).value.toInt()
                        val b = (array[array.lastIndex] as Numeric).value.toInt()
                        val ref = "$a $b R"
                        if ("^\\d+ \\d+ R$".toRegex().matches(ref)) {
                            repeat(2) { array.removeAt(array.lastIndex) }
                            entry = ref
                            content = "$a $b $content"
                        }
                    }
                }
            }

            content = content.substringAfter(entry)
            if (entry == "") break
            array.add(entry.toPDFObject(resolveReferences))
        }

        return this
    }

    operator fun get(i: Int): PDFObject? {
        return array[i]
    }

    override fun iterator(): Iterator<PDFObject?> {
        return array.iterator()
    }

    override fun toString(): String {
        val sb = StringBuilder("[")
        array.forEach {
            sb.append(" ${it.toString()} ")
        }
        sb.append("]")
        return sb.toString()
    }

    fun resolveReferences(): Array {
        array.asSequence()
            .filter { it is Reference }
            .forEachIndexed { i, it ->
                val resolved = (it as Reference).resolve()
                array[i] = resolved
            }
        return this
    }
}

internal fun String.toArray(resolveReferences: Boolean = false): Array {
    return Array(this).parse(resolveReferences)
}