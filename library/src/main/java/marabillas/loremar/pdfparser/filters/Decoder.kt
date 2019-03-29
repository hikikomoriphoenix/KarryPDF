package marabillas.loremar.pdfparser.filters

/**
 * Decodes a data that was encoded with a particular filter into its original data. Each filter have to implement this
 * interface, each having a different algorithm for the decode method.
 */
interface Decoder {
    /**
     * Executes a filter's decoding algorithm.
     *
     * @param encoded bytes of encoded data.
     *
     * @return a byte array containing the original un-encoded data.
     */
    fun decode(encoded: ByteArray): ByteArray
}