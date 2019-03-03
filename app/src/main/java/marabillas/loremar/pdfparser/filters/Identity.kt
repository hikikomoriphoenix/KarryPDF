package marabillas.loremar.pdfparser.filters

class Identity : Decoder {
    override fun decode(encoded: String): ByteArray {
        return encoded.toByteArray()
    }
}