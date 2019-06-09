package marabillas.loremar.pdfparser.contents.image

import marabillas.loremar.pdfparser.filters.DecoderFactory
import marabillas.loremar.pdfparser.objects.Dictionary
import marabillas.loremar.pdfparser.objects.Name
import marabillas.loremar.pdfparser.objects.PDFArray
import marabillas.loremar.pdfparser.objects.toDictionary
import marabillas.loremar.pdfparser.utils.exts.trimContainedChars
import java.nio.CharBuffer

internal class ImageObjectParser {
    private val ID = "ID"
    private val DIC_OPENING = "<<"
    private val DIC_CLOSING = ">>"
    private val EI = "EI"
    private val section = StringBuilder()
    private val miniSB = StringBuilder()
    private val charBuffer = CharBuffer.wrap(section)

    companion object {
        private val filters = hashMapOf(
            "AHx" to "ASCIIHexDecode",
            "A85" to "ASCII85Decode",
            "LZW" to "LZWDecode",
            "Fl" to "FlateDecode",
            "RL" to "RunLengthDecode",
            "CCF" to "CCITTFaxDecode",
            "DCT" to "DCTDecode"
        )
    }

    fun parse(s: StringBuilder, imageObject: ImageObject, startIndex: Int): Int {
        val idIndex = s.indexOf(ID, startIndex)
        val imageDic = section
            .clear()
            .append(s, startIndex, idIndex)
            .insert(0, DIC_OPENING)
            .append(DIC_CLOSING)
            .toDictionary(miniSB)
        val eiIndex = s.indexOf(
            EI,
            (idIndex + 2) + (section.length - 4)
        )
        section
            .clear()
            .append(s, idIndex + 2, eiIndex)
            .trimContainedChars()
        val encoded = Charsets.UTF_8.encode(charBuffer).array()
        val encodedDic = Dictionary(
            hashMapOf(
                "Filter" to imageDic["F"],
                "DecodeParms" to imageDic["DP"],
                "Height" to imageDic["H"]
            )
        )
        imageObject.imageData = decodeEncodedImageData(encoded, encodedDic)
        return eiIndex + 1
    }

    private fun decodeEncodedImageData(encoded: ByteArray, encodedDic: Dictionary): ByteArray {
        var decoded = encoded
        val filter = encodedDic["Filter"]
        val decoderFactory = DecoderFactory()
        if (filter is PDFArray) {
            for (f in filter) {
                if (f is Name) {
                    val decoder = filters[f.value]?.let {
                        decoderFactory.getDecoder(it, encodedDic)
                    }
                    decoded = decoder?.decode(decoded) ?: encoded
                }
            }
        } else if (filter is Name) {
            val decoder = filters[filter.value]?.let {
                decoderFactory.getDecoder(it, encodedDic)
            }
            decoded = decoder?.decode(decoded) ?: encoded
        }
        return decoded
    }
}