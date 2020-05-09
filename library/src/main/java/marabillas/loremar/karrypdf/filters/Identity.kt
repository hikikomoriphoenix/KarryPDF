package marabillas.loremar.karrypdf.filters

internal class Identity : Decoder {
    override fun decode(encoded: ByteArray): ByteArray {
        return encoded
    }
}