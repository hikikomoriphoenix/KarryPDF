package marabillas.loremar.karrypdf.document

class CharBuffer(private val size: Int) {
    private val array = CharArray(size)
    private var limit = 0

    val length: Int
        get() {
            return limit
        }

    fun put(c: Char) {
        if (limit < size) {
            array[limit] = c
            limit++
        }
    }

    operator fun get(i: Int): Char {
        return array[i]
    }

    fun rewind() {
        limit = 0
    }

    fun startsWith(c: Char): Boolean {
        return !(limit == 0 || array[0] != c)
    }

    fun startsWith(s: String): Boolean {
        for (i in s.indices) {
            if (i >= limit || array[i] != s[i])
                return false
        }
        return true
    }

    fun endsWith(c: Char): Boolean {
        return !(limit == 0 || array[limit - 1] != c)
    }

    fun endsWith(s: String): Boolean {
        for (i in s.indices) {
            if (limit - 1 - i < 0 || array[limit - 1 - i] != s[s.lastIndex - i])
                return false
        }
        return true
    }

    fun indexOf(c: Char, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
        var i = startIndex
        while (i < limit) {
            val c1 = if (ignoreCase) array[i].toLowerCase() else array[i]
            val c2 = if (ignoreCase) c.toLowerCase() else c
            if (c1 == c2)
                return i
            i++
        }
        return -1
    }

    fun indexOf(s: String, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
        var i = startIndex
        while (i < limit) {
            var c1 = if (ignoreCase) array[i].toLowerCase() else array[i]
            var c2 = if (ignoreCase) s[0].toLowerCase() else s[0]
            if (c1 == c2) {
                var found = true
                for (j in s.indices) {
                    c1 = if (ignoreCase) array[i + j].toLowerCase() else array[i + j]
                    c2 = if (ignoreCase) s[j].toLowerCase() else s[j]
                    if (i + j >= limit || c1 != c2) {
                        found = false
                        break
                    }
                }
                if (found) return i
            }
            i++
        }
        return -1
    }

    fun trimLast() {
        limit--
    }

    fun trimContainedChars() {
        while (startsWith(' ') || startsWith('\n') || startsWith('\r')) {
            delete(0, 1)
        }
        while (endsWith(' ') || endsWith('\n') || endsWith('\r'))
            delete(length - 1, length)
    }

    fun contains(s: String, ignoreCase: Boolean = true): Boolean {
        var i = 0
        while (i < limit) {
            var c1 = if (ignoreCase) array[i].toLowerCase() else array[i]
            var c2 = if (ignoreCase) s[0].toLowerCase() else s[0]
            if (c1 == c2) {
                var found = true
                for (j in s.indices) {
                    if (i + j >= limit) {
                        found = false
                        break
                    }
                    c1 = if (ignoreCase) array[i + j].toLowerCase() else array[i + j]
                    c2 = if (ignoreCase) s[j].toLowerCase() else s[j]
                    if (c1 != c2) {
                        found = false
                        break
                    }
                }
                if (found) return true
            }
            i++
        }
        return false
    }

    fun isBlank(): Boolean {
        for (i in 0 until limit) {
            if (array[i] != ' ' || array[i] != '\n' || array[i] != '\r')
                return false
        }
        return true
    }

    fun toInt(startIndex: Int = 0, endIndex: Int = -1): Int {
        val end = if (endIndex == -1) limit else endIndex
        var factor = 1
        var int = 0
        for (i in (end - 1) downTo startIndex) {
            val num = Character.getNumericValue(array[i])

            if ((num == -2 || num == -1) && (array[i] != '-')) throw NumberFormatException()

            if (array[i] == '-') {
                if (i == 0) {
                    int *= (-1)
                } else {
                    throw  NumberFormatException()
                }
                break
            }

            int += (num * factor)
            factor *= 10
        }
        return int
    }

    fun toLong(startIndex: Int = 0, endIndex: Int = -1): Long {
        val end = if (endIndex == -1) limit else endIndex
        var factor = 1
        var long = 0L
        for (i in (end - 1) downTo startIndex) {
            val num = Character.getNumericValue(array[i])

            if ((num == -2 || num == -1) && (array[i] != '-')) throw NumberFormatException()

            if (array[i] == '-') {
                if (i == 0) {
                    long *= (-1)
                } else {
                    throw  NumberFormatException()
                }
                break
            }

            long += (num * factor)
            factor *= 10
        }
        return long
    }

    fun matches(regex: Regex): Boolean {
        return array.joinToString(separator = "", limit = limit, truncated = "")
            .matches(regex)
    }

    fun delete(startIndex: Int, endIndex: Int) {
        if (startIndex < 0 || startIndex >= limit || endIndex <= 0 || endIndex > limit)
            throw IndexOutOfBoundsException()
        if (endIndex <= startIndex)
            throw IllegalArgumentException()
        for (i in endIndex until limit) {
            val j = i - endIndex
            array[startIndex + j] = array[i]
        }
        limit = startIndex + limit - endIndex
    }
}