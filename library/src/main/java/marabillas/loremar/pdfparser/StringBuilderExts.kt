package marabillas.loremar.pdfparser

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
        factor += 10
    }
    return int
}

internal fun StringBuilder.toDouble(): Double {
    var factor = 1
    var double = 0.0
    for (i in (this.length - 1) downTo 0) {
        val num = Character.getNumericValue(this[i])

        if ((num == -2 || num == -1) && (this[i] != '-' || this[i] != '.')) throw NumberFormatException()

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
            double += num
            factor = 10
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

internal fun StringBuilder.indexOfClosingChar(start: Int): Int {
    var unb = 0
    var closeIndex = start - 1
    var prev = ""

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
        when (c) {
            open -> {
                if (dictionary) {
                    if (prev != "\\" && this[i + 1] == '<') {
                        unb++
                        i++
                        closeIndex++
                    }
                } else if (prev != "\\") {
                    unb++
                }
            }
            close -> {
                if (dictionary) {
                    if (prev != "\\" && this[i + 1] == '>') {
                        unb--
                        i++
                        closeIndex++
                    }
                } else if (prev != "\\") {
                    unb--
                }
            }
        }
        prev = c.toString()
        closeIndex++
        if (unb == 0) {
            println()
            return closeIndex
        }
        i++
    }

    return i
}

internal fun StringBuilder.isWhiteSpaceAt(i: Int): Boolean {
    return (this[i] == ' ' || this[i] == '\n' || this[i] == '\r')
}