package marabillas.loremar.karrypdf.utils.exts

/**
 * Returns the index of the first occurrence of the given character starting from startIndex(INCLUSIVE)
 * until endIndex(EXCLUSIVE). Returns -1 if no such occurrence.
 */
fun ByteArray.indexOfChar(c: Char, startIndex: Int, endIndex: Int): Int {
    for (i in startIndex until endIndex) {
        if (get(i).toChar() == c)
            return i
    }
    return -1
}

fun ByteArray.toInt(startIndex: Int, endIndex: Int): Int {
    var factor = 1
    var int = 0
    for (i in (endIndex - 1) downTo startIndex) {
        val num = Character.getNumericValue(this[i].toChar())

        if ((num == -2 || num == -1) && (this[i].toChar() != '-')) throw NumberFormatException()

        if (this[i].toChar() == '-') {
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

internal fun ByteArray.toLong(startIndex: Int, endIndex: Int): Long {
    var factor = 1
    var long = 0L
    for (i in (endIndex - 1) downTo startIndex) {
        val num = Character.getNumericValue(this[i].toChar())

        if ((num == -2 || num == -1) && (this[i].toChar() != '-')) throw NumberFormatException()

        if (this[i].toChar() == '-') {
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