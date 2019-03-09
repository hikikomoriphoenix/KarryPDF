package marabillas.loremar.pdfparser.filters

class Identity : Decoder {
    override fun decode(encoded: ByteArray): ByteArray {
        return encoded
    }
}