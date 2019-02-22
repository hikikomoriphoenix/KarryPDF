package marabillas.loremar.pdfparser

internal class DictionaryParsingHelper {
    /**
     * Locates the close parentheses of the first open parentheses in a string and returns its index. This helps in
     * parsing a string object in a PDF file.
     *
     * @param string String with a presumably balanced parentheses.
     *
     * @return the index of the close parentheses or 0 if no close parentheses for the first open parentheses exists.
     */
    fun findIndexOfClosingParentheses(string: String): Int {
        var unbParent = 1
        var closeParentIndex = 0
        var closeParentFound = false
        var prev = ""
        string.substringAfter('(').forEach {
            when (it) {
                '(' -> {
                    if (prev != "\\") {
                        unbParent++
                    }
                }
                ')' -> {
                    if (prev != "\\") {
                        unbParent--
                    }
                }
            }
            prev = it.toString()
            if (!closeParentFound) closeParentIndex++
            if (unbParent == 0) {
                closeParentFound = true
                return@forEach
            }
        }

        if (unbParent != 0) return 0
        return closeParentIndex
    }
}