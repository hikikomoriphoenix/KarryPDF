package marabillas.loremar.pdfparser.utils

internal fun octalToDecimal(octal: Int): Int {
    var num = octal
    var result = 0

    while (true) {
        var factor = 1

        // Get first digit
        var first = num
        while (first >= 10) {
            first /= 10
            factor *= 10
        }

        // Add first digit to decimal result
        result += first

        // Remove first digit
        num -= (first * factor)

        // If no more first digit to add next, then done.
        if (num == 0) break

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