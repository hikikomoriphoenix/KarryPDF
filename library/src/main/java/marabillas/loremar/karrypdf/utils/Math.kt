package marabillas.loremar.karrypdf.utils

import kotlin.math.abs
import kotlin.math.log10

internal fun octalToDecimal(octal: Int): Int {
    var num = octal
    var result = 0

    // Count digits
    var n = octal.length()

    while (true) {
        // Divisor allows handling of first digit
        val divisor = Math.pow(10.0, n.toDouble() - 1).toInt()

        // Get first digit
        val first = num / divisor

        // Add first digit to decimal result
        result += first

        // Remove first digit
        num -= (first * divisor)
        n--

        // If no more first digit to add next, then done.
        if (n == 0) break

        // If not done, multiply decimal result by 8
        result *= 8
    }

    return result
}

internal fun decimalToOctal(decimal: Int): Int {
    var octal = 0
    var q = decimal
    var r: Int
    var factor = 1
    while (true) {
        r = q % 8
        q /= 8
        octal += r * factor
        factor *= 10

        if (q == 0) break
    }
    return octal
}

internal fun wholeNumToFractional(num: Int): Float {
    if (num == 0) return 0f

    // Count digits
    val n = num.length()

    // Get the divisor required to convert the number to the nth place
    val divisor = Math.pow(10.0, n.toDouble()).toFloat()

    return num.toFloat() / divisor
}

internal fun Int.length(): Int {
    return when (this) {
        0 -> 1
        else -> log10(abs(toDouble())).toInt() + 1
    }
}

internal fun multiplyTransformMatrices(m1: FloatArray, m2: FloatArray): FloatArray {
    if (m1.count() != 6 || m2.count() != 6)
        throw IllegalArgumentException("Each matrix must have a size of 6")

    val a = m1[0] * m2[0] + m1[1] * m2[2]
    val b = m1[0] * m2[1] + m1[1] * m2[3]
    val c = m1[2] * m2[0] + m1[3] * m2[2]
    val d = m1[2] * m2[1] + m1[3] * m2[3]
    val e = m1[4] * m2[0] + m1[5] * m2[2] + m2[4]
    val f = m1[4] * m2[1] + m1[5] * m2[3] + m2[5]

    return floatArrayOf(a, b, c, d, e, f)
}