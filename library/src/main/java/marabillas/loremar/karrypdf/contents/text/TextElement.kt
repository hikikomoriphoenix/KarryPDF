package marabillas.loremar.karrypdf.contents.text

import marabillas.loremar.karrypdf.contents.PageContent
import marabillas.loremar.karrypdf.objects.PDFObject
import marabillas.loremar.karrypdf.objects.toPDFString

internal class TextElement internal constructor(
    var tj: PDFObject = "()".toPDFString(),
    val tf: String = "",
    val ts: Float = 0f,
    val rgb: FloatArray = floatArrayOf(-1f, -1f, -1f)
) : PageContent {
    var tx = 0f
    var ty = 0f
    var scaleX = 1f
    var scaleY = 1f
    var width = 0f
    val fontResource = tf.substring(0, tf.indexOf(' '))
    var textMatrix = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)
    var textParamsMatrix = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)
    val fontName = tf.trim().substringBefore(' ')
    var fontSize = tf.trim().substringAfterLast(' ').toFloat()

    fun setTextParamsMatrix(fontSize: Float, horizontalScaling: Float, textRise: Float) {
        textParamsMatrix.apply {
            set(0, fontSize * horizontalScaling)
            set(1, 0f)
            set(2, 0f)
            set(3, fontSize)
            set(4, 0f)
            set(5, textRise)
        }
    }
}