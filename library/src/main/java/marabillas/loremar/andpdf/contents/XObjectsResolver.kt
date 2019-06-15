package marabillas.loremar.andpdf.contents

import marabillas.loremar.andpdf.contents.image.ImageObject
import marabillas.loremar.andpdf.filters.DecoderFactory
import marabillas.loremar.andpdf.objects.Dictionary
import marabillas.loremar.andpdf.objects.Name
import marabillas.loremar.andpdf.objects.PDFArray
import marabillas.loremar.andpdf.objects.Stream

internal class XObjectsResolver(
    private val pageObjects: MutableList<PageObject>,
    private val xObjects: HashMap<String, Stream>
) {
    fun resolve() {
        pageObjects.forEachIndexed { i, obj ->
            if (obj is XObject) {
                val xObjStm = xObjects[obj.resourceName.value]
                if (xObjStm is Stream) {
                    val imgObj = ImageObject(obj.getX(), obj.getY())
                    val encodedImgData = xObjStm.decodeEncodedStream()
                    imgObj.imageData = decodeEncodedImageData(encodedImgData, xObjStm.dictionary)
                    pageObjects[i] = imgObj
                }
            }
        }
    }

    private fun decodeEncodedImageData(encoded: ByteArray, encodedDic: Dictionary): ByteArray {
        var decoded = encoded
        val filter = encodedDic["Filter"]
        if (filter is PDFArray) {
            for (f in filter) {
                if (f is Name) {
                    val decoder = DecoderFactory().getDecoder(f.value, encodedDic)
                    decoded = decoder.decode(decoded)
                }
            }
        } else if (filter is Name) {
            val decoder = DecoderFactory().getDecoder(filter.value, encodedDic)
            decoded = decoder.decode(decoded)
        }
        return decoded
    }
}