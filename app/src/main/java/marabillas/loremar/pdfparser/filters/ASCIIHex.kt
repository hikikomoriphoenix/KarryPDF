package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.CaseInsensitiveMap

class ASCIIHex {
    companion object {
        var hexMap = CaseInsensitiveMap()
            private set

        init {
            val keys = arrayOfNulls<String>(256)
            var index = 0

            for (i in '0'..'9') {
                index = iter2ndDigit(i, keys, index)
            }

            for (i in 'A'..'F') {
                index = iter2ndDigit(i, keys, index)
            }

            val values = ByteArray(256) { it.toByte() }
            index = 0
            keys.map { hexMap.put(it.toString(), values[index++]) }
        }

        private fun iter2ndDigit(i: Char, keys: Array<String?>, start: Int): Int {
            var m = start
            for (j in '0'..'9') {
                keys[m++] = "$i$j"
            }
            for (j in 'A'..'F') {
                keys[m++] = "$i$j"
            }
            return m
        }
    }
}