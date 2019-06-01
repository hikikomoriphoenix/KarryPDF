package marabillas.loremar.pdfparser.objects

/**
 * Extracts enclosed objects from a string. An enclosed object is an object with enclosing delimiters i.e. arrays,
 * dictionaries, and string.
 *
 * @param stringWithEnclosed String that contains the enclosed object.
 * @param startIndex Index of the enclosed object's first character.
 */
internal class EnclosedObjectExtractor(private val stringWithEnclosed: String, private val startIndex: Int = 0) {
    init {
        stringBuilder.clear().append(stringWithEnclosed)
        start = startIndex
    }

    fun extract(): String {
        return EnclosedObjectExtractor.extract()
    }

    internal companion object {
        var stringBuilder = StringBuilder()
        var start = 0

        fun extract(): String {
            val close = indexOfClosingChar(stringBuilder, start)
            return if (close > start) {
                stringBuilder.substring(start, close + 1)
            } else {
                ""
            }
        }

        fun indexOfClosingChar(string: StringBuilder, start: Int): Int {
            var unb = 0
            var prev = string[start]

            val open = string[start]
            val close = getClosingChar(open)

            var dictionary = false
            if (open == '<' && string[start + 1] == '<') dictionary = true

            var i = start
            while (i < string.length) {
                val c = string[i]
                when {
                    c == open -> {
                        if (dictionary) {
                            if (prev != '\\' && string[i + 1] == '<') {
                                unb++
                                i++
                            }
                        } else if (prev != '\\') {
                            unb++
                        }
                        prev = c
                    }
                    c == close -> {
                        if (dictionary) {
                            if (prev != '\\' && string[i + 1] == '>') {
                                unb--
                                i++
                            }
                        } else if (prev != '\\') {
                            unb--
                        }
                        prev = c
                    }
                    open != '(' && isEnclosing(c) -> {
                        if (!(dictionary && prev == '<')) {
                            i = indexOfClosingChar(string, i)
                            prev = string[i]
                        }
                    }
                }
                if (unb == 0) {
                    return i
                }
                i++
            }

            return start
        }

        private fun getClosingChar(c: Char): Char? {
            return when (c) {
                '(' -> ')'
                '[' -> ']'
                '<' -> '>'
                '{' -> '}'
                else -> null
            }
        }

        private fun isEnclosing(c: Char): Boolean {
            return (c == '(' || c == '[' || c == '<' || c == '{')
        }
    }
}

/**
 * Check if string starts with an enclosing delimiter i.e. '(', '[', '<', '{'.
 *
 * @return true or false.
 */
internal fun String.startsEnclosed(): Boolean {
    return when (this.first()) {
        '(' -> true
        '[' -> true
        '<' -> true
        '{' -> true
        else -> false
    }
}

internal fun String.extractEnclosedObject(startIndex: Int = 0): String {
    EnclosedObjectExtractor.start = startIndex
    EnclosedObjectExtractor.stringBuilder.clear().append(this)
    return EnclosedObjectExtractor.extract()
}