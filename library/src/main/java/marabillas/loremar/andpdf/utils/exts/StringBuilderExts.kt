package marabillas.loremar.andpdf.utils.exts

internal fun StringBuilder.trimContainedChars() {
    while (this.startsWith(' ') || this.startsWith('\n') || this.startsWith('\r')) {
        this.delete(0, 1)
    }
    while (this.endsWith(' ') || this.endsWith('\n') || this.endsWith('\r'))
        this.delete(this.length - 1, this.length)
}

internal fun StringBuilder.trimStartOfContainedChars() {
    while (this.startsWith(' ') || this.startsWith('\n') || this.startsWith('\r')) {
        this.delete(0, 1)
    }
}

internal fun StringBuilder.trimEndOfContainedChars() {
    while (this.endsWith(' ') || this.endsWith('\n') || this.endsWith('\r'))
        this.delete(this.length - 1, this.length)
}

internal fun StringBuilder.containedEqualsWith(vararg chars: Char): Boolean {
    if (chars.size != this.length)
        return false

    for (i in 0 until chars.size) {
        if (this[i] != chars[i])
            return false
    }
    return true
}

internal fun StringBuilder.toInt(): Int {
    var factor = 1
    var int = 0
    for (i in (this.length - 1) downTo 0) {
        val num = Character.getNumericValue(this[i])

        if ((num == -2 || num == -1) && (this[i] != '-')) throw NumberFormatException()

        if (this[i] == '-') {
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

internal fun StringBuilder.toDouble(): Double {
    var factor = 1
    var double = 0.0
    for (i in (this.length - 1) downTo 0) {
        val num = Character.getNumericValue(this[i])

        if ((num == -2 || num == -1) && this[i] != '-' && this[i] != '.') throw NumberFormatException()

        if (this[i] == '-') {
            if (i == 0) {
                double *= (-1)
            } else {
                throw NumberFormatException()
            }
            break
        }

        if (this[i] == '.') {
            double /= factor
            factor = 1
        } else {
            double += (num * factor)
            factor *= 10
        }
    }
    return double
}

internal fun StringBuilder.isEnclosedWith(open: Char, close: Char): Boolean {
    return this.startsWith(open) && this.endsWith(close)
}

internal fun StringBuilder.isEnclosedWith(open: Array<Char>, close: Array<Char>): Boolean {
    for (i in 0 until open.size) {
        if (this[i] != open[i])
            return false
    }
    for (i in (close.size - 1) downTo 0) {
        if (this[this.length - (close.size - i)] != close[i])
            return false
    }
    return true
}

internal fun StringBuilder.isEnclosingAt(i: Int): Boolean {
    return (this[i] == '(' || this[i] == '[' || this[i] == '<' || this[i] == '{')
}

internal fun StringBuilder.isUnEnclosingAt(i: Int): Boolean {
    return (this[i] == ')' || this[i] == ']' || this[i] == '>' || this[i] == '}')
}

internal fun StringBuilder.indexOfClosingChar(start: Int): Int {
    if (!this.isEnclosingAt(start))
        return start

    var unb = 0
    var prev: Char? = null

    val open = this[start]
    val close = when (open) {
        '(' -> ')'
        '[' -> ']'
        '<' -> '>'
        '{' -> '}'
        else -> null
    }

    var dictionary = false
    if (open == '<' && this[start + 1] == '<') dictionary = true

    var i = start
    while (i < this.length) {
        val c = this[i]
        when {
            c == open -> {
                if (dictionary) {
                    if (prev != '\\' && this[i + 1] == '<') {
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
                    if (prev != '\\' && this[i + 1] == '>') {
                        unb--
                        i++
                    }
                } else if (prev != '\\') {
                    unb--
                }
                prev = c
            }
            (open != '(') && (this.isEnclosingAt(i)) -> {
                if (!(dictionary && prev == '<')) {
                    i = this.indexOfClosingChar(i)
                    prev = this[i]
                }
            }
            c == '\\' && prev == '\\' -> {
                prev = null
            }
            else -> {
                prev = c
            }
        }
        if (unb == 0) {
            return i
        }
        i++
    }

    return start
}

internal fun StringBuilder.isWhiteSpaceAt(i: Int): Boolean {
    return (this[i] == ' ' || this[i] == '\n' || this[i] == '\r')
}

internal fun StringBuilder.resolveEscapedSequences(startIndex: Int = 0) {
    var i = startIndex
    while (i < this.length) {
        if (this[i] == '\\' && (i + 1 < this.length)) {
            when (this[i + 1]) {
                'n' -> {
                    this.delete(i, i + 2)
                    this.insert(i, '\n')
                }
                'r' -> {
                    this.delete(i, i + 2)
                    this.insert(i, '\r')
                }
                't' -> {
                    this.delete(i, i + 2)
                    this.insert(i, '\t')
                }
                'b' -> {
                    this.delete(i, i + 2)
                    this.insert(i, '\b')
                }
                'f' -> {
                    this.delete(i, i + 2)
                    this.insert(i, '\u000C')
                }
                '(' -> {
                    this.delete(i, i + 2)
                    this.insert(i, '(')
                }
                ')' -> {
                    this.delete(i, i + 2)
                    this.insert(i, ')')
                }
                '\\' -> {
                    this.delete(i, i + 2)
                    this.insert(i, '\\')
                }
                else -> {
                    if (i + 3 < this.length) {
                        val digits = CharArray(3)
                        var dCounts = 0
                        for (j in 1..3) {
                            if (this[i + j].isDigit()) {
                                digits[j - 1] = this[i + j]
                                dCounts++
                            } else break
                        }
                        if (dCounts > 0) {
                            val int = Integer.parseInt(String(digits, 0, dCounts), 8)
                            this.delete(i, i + dCounts + 1)
                            this.insert(i, int.toChar())
                        }
                    }
                }
            }
        }
        i++
    }
}

private val HEX = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

internal fun StringBuilder.convertContentsToHex() {
    var int: Int
    var r1: Int
    var r2: Int
    var i = 0
    while (i < this.length) {
        int = this[i].toInt()

        // Convert decimal to hex
        r1 = int % 16
        int /= 16
        r2 = int % 16

        // Replace the current character in StringBuilder with two Hex characters
        this.deleteCharAt(i)
        this.insert(i, HEX[r2])
        this.insert(i + 1, HEX[r1])

        i += 2
    }
}

internal fun StringBuilder.hexToInt(start: Int = 0, end: Int = this.length): Int {
    if (this.length % 2 != 0)
        this.append('0')

    var int: Int
    var sum = 0
    for (i in start until end) {
        int = Character.getNumericValue(this[i])
        if (int < 0 || int > 15) throw NumberFormatException()
        sum += (int * Math.pow(16.0, (end - 1 - i).toDouble()).toInt())
    }
    return sum
}

internal fun StringBuilder.hexFromInt(int: Int): StringBuilder {
    this.clear()
    var result = int
    val remainders = mutableListOf<Int>()
    while (true) {
        remainders.add(
            result % 16
        )
        result /= 16

        if (result == 0) break
    }
    for (i in remainders.lastIndex downTo 0) {
        this.append(HEX[remainders[i]])
    }
    remainders.clear()
    return this
}

internal fun StringBuilder.appendBytes(bytes: ByteArray, start: Int = 0, end: Int = bytes.size): StringBuilder {
    for (i in start until end) {
        val c = bytes[i].toChar()
        this.append(c)
    }
    return this
}
