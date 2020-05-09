package marabillas.loremar.karrypdf.contents.image

import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.objects.Dictionary
import marabillas.loremar.karrypdf.objects.toDictionary
import marabillas.loremar.karrypdf.utils.exts.trimContainedChars
import java.nio.CharBuffer

internal class ImageObjectParser(
    private val context: KarryPDFContext,
    private val obj: Int,
    private val gen: Int
) {
    private val ID = "ID"
    private val DIC_OPENING = "<<"
    private val DIC_CLOSING = ">>"
    private val EI = "EI"
    private val section = StringBuilder()
    private val miniSB = StringBuilder()
    private val charBuffer = CharBuffer.wrap(section)
    private val imageDataDecoder =
        ImageDataDecoder()

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
            .toDictionary(context, miniSB, obj, gen)

        val eiIndex = s.indexOf(
            EI,
            idIndex + 2
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
        imageObject.bitmap = imageDataDecoder.decodeEncodedImageData(encoded, encodedDic)
        return eiIndex + 2
    }
}