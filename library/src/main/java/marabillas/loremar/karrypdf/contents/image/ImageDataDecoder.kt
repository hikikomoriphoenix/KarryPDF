package marabillas.loremar.karrypdf.contents.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import marabillas.loremar.karrypdf.filters.DecoderFactory
import marabillas.loremar.karrypdf.objects.*
import marabillas.loremar.karrypdf.utils.logd

internal class ImageDataDecoder {
    fun decodeEncodedImageData(encoded: ByteArray, encodedDic: Dictionary): Bitmap? {
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
            return if (filter.value == "DCTDecode") {
                handleDCTDecode(decoded, decodeParms)
            } else {
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            }
        }

        return null
    }

    private fun handleDCTDecode(decoded: ByteArray, decodeParms: PDFObject?): Bitmap? {
        logd("Handling DCTDecode")
        var colorTransformValue: Int? = null
        if (decodeParms is Dictionary) {
            decodeParms.resolveReferences()
            val colorTransform = decodeParms["ColorTransform"] as Numeric?
            colorTransformValue = colorTransform?.value?.toInt()
        }
        val jpegImage = JPEGImage(
            decoded,
            colorTransformValue
        )
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