package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.utils.exts.toDouble

internal class Numeric : PDFObject {
    val value: Double

    constructor(string: String) {
        value = string.toDouble()
    }

    constructor(sb: StringBuilder) {
        value = sb.toDouble()
    }

    override fun equals(other: Any?): Boolean {
        return value.equals(other)
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }
}

internal fun String.toNumeric(): Numeric {
    return Numeric(this)
}

internal fun StringBuilder.toNumeric(): Numeric {
    return Numeric(this)
}