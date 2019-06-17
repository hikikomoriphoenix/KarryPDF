package marabillas.loremar.andpdf.contents

import android.util.Log
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
        val toRemove = mutableListOf<Int>()
        pageObjects.forEachIndexed { i, obj ->
            if (obj is XObject) {
                val xObjStm = xObjects[obj.resourceName.value]
                val type = xObjStm?.dictionary?.get("Subtype")
                if (xObjStm is Stream && type is Name && type.value == "Image") {
                    val imgObj = ImageObject(obj.getX(), obj.getY())
                    val encodedImgData = xObjStm.decodeEncodedStream()
                    try {
                        imgObj.imageData = decodeEncodedImageData(encodedImgData, xObjStm.dictionary)
                        pageObjects[i] = imgObj
                    } catch (e: Exception) {
                        Log.e(javaClass.name, "${e.javaClass.name}: ${e.message}")
                        val elements = e.stackTrace
                        for (element in elements) {
                            Log.e(javaClass.name, "$element")
                            if (element.toString().contains("XObjectsResolver.resolve"))
                                break
                        }
                        toRemove.add(i)
                    }
                } else {
                    // TODO Other types of XObjects need to be handled. Until then, remove this XObject.
                    toRemove.add(i)
                }
            }
        }
        toRemove.forEach { objIndex ->
            pageObjects.removeAt(objIndex)
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