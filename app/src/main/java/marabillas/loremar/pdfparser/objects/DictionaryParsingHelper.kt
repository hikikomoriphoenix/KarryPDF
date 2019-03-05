package marabillas.loremar.pdfparser.objects

internal class DictionaryParsingHelper {
    /**
     * Locates the closing delimiter of the first opening delimiter in a string and returns its index. This helps in
     * parsing objects enclosed with enclosing delimiters i.e. '()','[]','<>','{}' in a PDF file.
     *
     * @param string String enclosed by enclosing delimiters.
     *
     * @return the index of the closing delimiter or 0 if no closing delimiter for the first opening delimiter exists.
     */
    fun findIndexOfClosingDelimiter(string: String): Int {
        var unb = 1
        var closeIndex = 0
        var prev = ""

        val open = string.first()
        val close = getClosingChar(string.first())

        var dictionary = false
        if (open == '<' && string[1] == '<') dictionary = true

        string.substringAfter(open).forEachIndexed { i, c ->
            when (c) {
                open -> {
                    if (dictionary) {
                        if (prev != "\\" && string[i + 1] == '<')
                            unb++
                    } else if (prev != "\\") {
                        unb++
                    }
                }
                close -> {
                    if (dictionary) {
                        if (prev != "\\" && string[i + 1] == '>')
                            unb--
                    } else if (prev != "\\") {
                        unb--
                    }
                }
            }
            prev = c.toString()
            closeIndex++
            if (unb == 0) {
                return closeIndex
            }
        }

        return 0
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
}