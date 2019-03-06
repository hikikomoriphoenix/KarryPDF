package marabillas.loremar.pdfparser.objects

class Array(private val arrayString: String) {
    private val array = ArrayList<String>()
    fun parse(): Array {
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
                else -> entry = content.split(regex = "[()<\\[{/ ]".toRegex())[0]
            }

            content = content.substringAfter(entry)
            if (entry == "") break
            array.add(entry)
        }

        return this
    }

    operator fun get(i: Int): String {
        return array[i]
    }
}

fun String.toArray(): Array {
    return Array(this)
}