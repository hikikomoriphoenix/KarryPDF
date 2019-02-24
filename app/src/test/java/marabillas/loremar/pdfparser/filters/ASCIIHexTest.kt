package marabillas.loremar.pdfparser.filters

import org.junit.Test

class ASCIIHexTest {
    @Test
    fun printHexMap() {
        for ((key, value) in ASCIIHex.hexMap.toSortedMap()) {
            println("$key = $value")
        }
    }
}