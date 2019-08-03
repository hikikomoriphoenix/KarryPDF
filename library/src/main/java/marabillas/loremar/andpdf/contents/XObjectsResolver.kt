package marabillas.loremar.andpdf.contents

import android.util.Log
import marabillas.loremar.andpdf.contents.image.ImageDataDecoder
import marabillas.loremar.andpdf.contents.image.ImageObject
import marabillas.loremar.andpdf.objects.Name
import marabillas.loremar.andpdf.objects.Stream

internal class XObjectsResolver(
    private val pageObjects: MutableList<PageObject>,
    private val xObjects: HashMap<String, Stream>
) {
    private val imageDataDecoder = ImageDataDecoder()

    fun resolve() {
        val toRemove = mutableListOf<PageObject>()
        pageObjects.forEachIndexed { i, obj ->
            if (obj is XObject) {
                val xObjStm = xObjects[obj.resourceName.value]
                val type = xObjStm?.dictionary?.get("Subtype")
                if (xObjStm is Stream && type is Name && type.value == "Image") {
                    val imgObj = ImageObject(obj.getX(), obj.getY())
                    val encodedImgData = xObjStm.decodeEncodedStream()
                    try {
                        imgObj.bitmap = imageDataDecoder.decodeEncodedImageData(encodedImgData, xObjStm.dictionary)
                        pageObjects[i] = imgObj
                    } catch (e: Exception) {
                        Log.e(javaClass.name, "${e.javaClass.name}: ${e.message}")
                        val elements = e.stackTrace
                        for (element in elements) {
                            Log.e(javaClass.name, "$element")
                            if (element.toString().contains("XObjectsResolver.resolve"))
                                break
                        }
                        toRemove.add(obj)
                    }
                } else {
                    // TODO Other types of XObjects need to be handled. Until then, remove this XObject.
                    toRemove.add(obj)
                }
            }
        }
        toRemove.forEach { obj ->
            pageObjects.remove(obj)
        }
    }
}