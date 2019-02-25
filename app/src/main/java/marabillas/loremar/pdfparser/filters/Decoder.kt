package marabillas.loremar.pdfparser.filters

interface Decoder {
    fun decode(encoded: String): ByteArray
}