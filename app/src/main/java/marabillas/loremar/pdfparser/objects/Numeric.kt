package marabillas.loremar.pdfparser.objects

import java.math.BigDecimal

class Numeric(string: String) : PDFObject {
    val value = BigDecimal(string)

    override fun equals(other: Any?): Boolean {
        return value.equals(other)
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }

    operator fun plus(other: BigDecimal): BigDecimal {
        return value.plus(other)
    }

    operator fun minus(other: BigDecimal): BigDecimal {
        return value.minus(other)
    }

    operator fun times(other: BigDecimal): BigDecimal {
        return value.times(other)
    }

    operator fun div(other: BigDecimal): BigDecimal {
        return value.div(other)
    }

    operator fun rem(other: BigDecimal): BigDecimal {
        return value.rem(other)
    }

    operator fun rangeTo(other: BigDecimal): ClosedRange<BigDecimal> {
        return value..other
    }

    operator fun compareTo(other: BigDecimal): Int {
        return value.compareTo(other)
    }
}

fun String.toNumeric(): Numeric {
    return Numeric(this)
}