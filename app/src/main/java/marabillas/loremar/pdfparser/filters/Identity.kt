package marabillas.loremar.pdfparser.filters

internal class Identity : Decoder {
    override fun decode(encoded: ByteArray): ByteArray {
        return encoded
    }
}