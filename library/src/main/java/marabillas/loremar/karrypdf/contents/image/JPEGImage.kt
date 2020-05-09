package marabillas.loremar.karrypdf.contents.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.ByteBuffer

internal class JPEGImage(private val data: ByteArray, colorTransform: Int? = null) {
    private var colorTransformInEncoded: Int? = null
    private var startOfScan: Int? = null
    private var startOfData: Int? = null
    private var endOfImage: Int? = null

    var bitmap: Bitmap? = null; private set

    init {
        /*
        Reference for JPEG file structure: http://vip.sugovica.hu/Sardi/kepnezo/JPEG%20File%20Layout%20and%20Format.htm
         */

        bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

        val numComponents = numComponents()
        if (numComponents == 4 && bitmap != null) {
            invertColors()
        }
    }

    private fun invertColors() {
        // Inverting colors as per this solution: https://stackoverflow.com/questions/4624531/invert-bitmap-colors/4625618#4625618
        val bmp = bitmap as Bitmap
        val length = bmp.width * bmp.height
        val pixels = IntArray(length)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        val buffer = ByteBuffer.allocate(4)

        for (i in 0 until pixels.size) {
            buffer.putInt(pixels[i])
            buffer.flip()
            buffer.array()[1] = (255 - buffer.array()[1].toInt() and 0xff).toByte()
            buffer.array()[2] = (255 - buffer.array()[2].toInt() and 0xff).toByte()
            buffer.array()[3] = (255 - buffer.array()[3].toInt() and 0xff).toByte()
            pixels[i] = buffer.int
            buffer.clear()
        }
        // Create mutable Bitmap. The bitmap above was immutable(Check with bmp.isMutable())
        bitmap = Bitmap.createBitmap(bmp.width, bmp.height, bmp.config)
        bitmap?.setPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
    }

    private fun getColorTransform() {
        var i = 0
        while (i + 4 < data.size) {
            if (
                data[i].toChar() == 'A'
                && data[i + 1].toChar() == 'd'
                && data[i + 2].toChar() == 'o'
                && data[i + 3].toChar() == 'b'
                && data[i + 4].toChar() == 'e'
            ) {
                colorTransformInEncoded = data[11].toInt() and 0xff
                break
            }
            i++
        }
    }

    /**
     * Locates the first byte of the SOS(Start of Scan) marker segment which
     */
    private fun findStartOfScan() {
        if (startOfScan == null) {
            var i = 0
            while (i + 1 < data.size) {
                val ff = data[i].toInt() and 0xff
                val xx = data[i + 1].toInt() and 0xff
                if (ff == 255 && xx == 218) {
                    // SOS = DA(Hex) or 218(Dec)
                    startOfScan = i + 2
                }
                i++
            }
        }
    }

    private fun numComponents(): Int {
        findStartOfScan()
        if (startOfScan != null) {
            val nPos = (startOfScan as Int) + 2
            return data[nPos].toInt() and 0xff
        }
        return 0
    }

    private fun findStartOfData() {
        findStartOfScan()
        if (startOfScan != null) {
            // Get length which is the value in the first two bytes of the SOS marker
            var length = 0 or (data[startOfScan as Int].toInt() and 0xff)
            length = length shl 8
            length = length or (data[(startOfScan as Int) + 1].toInt() and 0xff)
            // Locate the start of compressed data which is right after the marker
            startOfData = (startOfScan as Int) + length
        }
    }

    private fun findEndOfImage() {
        var i = startOfData ?: return
        while (i + 1 < data.size) {
            val ff = data[i].toInt() and 0xff
            val xx = data[i + 1].toInt() and 0xff
            if (ff == 255 && xx == 217) {
                // EOI = D9(Hex) or 217(Dec)
                endOfImage = i
            }
            i++
        }
    }

    private fun identifyComponents() {
        findStartOfScan()
        if (startOfScan != null) {
            val c1Pos = (startOfScan as Int) + 3
            val c1 = data[c1Pos].toInt() and 0xff
            val c2Pos = (startOfScan as Int) + 5
            val c2 = data[c2Pos].toInt() and 0xff
            val c3Pos = (startOfScan as Int) + 7
            val c3 = data[c3Pos].toInt() and 0xff
            val c4Pos = (startOfScan as Int) + 9
            val c4 = data[c4Pos].toInt() and 0xff
        }
    }

    private fun yccToR(y: Float, cb: Float, cr: Float): Float {
        return y + (1.402f * (cr - 128))
    }

    private fun yccToG(y: Float, cb: Float, cr: Float): Float {
        return y - (0.34414f * (cb - 128)) - (0.71414f * (cr - 128))
    }

    private fun yccToB(y: Float, cb: Float, cr: Float): Float {
        return y + (1.772f * (cb - 128))
    }

    private fun rgbToY(r: Float, g: Float, b: Float): Float {
        return (0.299f * r) + (0.587f * g) + (0.114f * b)
    }

    private fun rgbToCb(r: Float, g: Float, b: Float): Float {
        return (-0.1687f * r) - (0.3313f * g) + (0.5f * b) + 128
    }

    private fun rgbToCr(r: Float, g: Float, b: Float): Float {
        return (0.5f * r) - (0.4187f * g) - (0.0813f * b) + 128
    }
}