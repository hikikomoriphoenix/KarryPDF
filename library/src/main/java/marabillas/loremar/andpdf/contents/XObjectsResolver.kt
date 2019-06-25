package marabillas.loremar.andpdf.contents

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import marabillas.loremar.andpdf.contents.image.ImageObject
import marabillas.loremar.andpdf.contents.image.JPEGImage
import marabillas.loremar.andpdf.filters.DecoderFactory
import marabillas.loremar.andpdf.objects.*

internal class XObjectsResolver(
    private val pageObjects: MutableList<PageObject>,
    private val xObjects: HashMap<String, Stream>
) {
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
                        imgObj.bitmap = decodeEncodedImageData(encodedImgData, xObjStm.dictionary)
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

    private fun decodeEncodedImageData(encoded: ByteArray, encodedDic: Dictionary): Bitmap? {
        encodedDic.resolveReferences()
        var decoded = encoded
        val filter = encodedDic["Filter"]

        val colorSpace = encodedDic["ColorSpace"]
        val numColorComponents = numColorComponents(colorSpace)

        val decodeParms = encodedDic["DecodeParms"]
        if (filter is PDFArray) {
            for (f in filter) {
                if (f is Name) {
                    val decoder = DecoderFactory().getDecoder(f.value, encodedDic)
                    decoded = decoder.decode(decoded)
                    return if (f.value == "DCTDecode") {
                        handleDCTDecode(decoded, decodeParms)
                    } else {
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    }
                }
            }
        } else if (filter is Name) {
            val decoder = DecoderFactory().getDecoder(filter.value, encodedDic)
            decoded = decoder.decode(decoded)
            decoded = decoder.decode(decoded)
            return if (filter.value == "DCTDecode") {
                handleDCTDecode(decoded, decodeParms)
            } else {
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            }
        }

        return null
    }

    private fun handleDCTDecode(decoded: ByteArray, decodeParms: PDFObject?): Bitmap? {
        println("Handling DCTDecode")
        var colorTransformValue: Int? = null
        if (decodeParms is Dictionary) {
            decodeParms.resolveReferences()
            val colorTransform = decodeParms["ColorTransform"] as Numeric?
            colorTransformValue = colorTransform?.value?.toInt()
        }
        val jpegImage = JPEGImage(decoded, colorTransformValue)
        return jpegImage.bitmap
    }

    private fun numColorComponents(colorSpace: PDFObject?): Int {
        if (colorSpace is PDFArray && (colorSpace[0] as Name).value == "ICCBased") {
            val iccStream = colorSpace[1]
            if (iccStream is Reference) {
                val iccStreamResolved = iccStream.resolveToStream()
                if (iccStreamResolved is Stream) {
                    iccStreamResolved.dictionary.resolveReferences()
                    val n = iccStreamResolved.dictionary["N"] as Numeric
                    return n.value.toInt()
                }
            }
        } else if (colorSpace is Name) {
            when (colorSpace.value) {
                "DeviceRGB" -> return 3
                "DeviceCMYK" -> return 4
                "DeviceGray" -> return 1
            }
        }

        return 3 // Default to RGB
    }
}