package marabillas.loremar.pdfparser.utils

internal fun octalToDecimal(octal: Int): Int {
    var num = octal
    var result = 0

    // Count digits
    var n = (Math.log10(octal.toDouble()) + 1).toInt()

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
    val n = (Math.log10(num.toDouble()) + 1).toInt()

    // Get the divisor required to convert the number to the nth place
    val divisor = Math.pow(10.0, n.toDouble()).toFloat()

    return num.toFloat() / divisor
}