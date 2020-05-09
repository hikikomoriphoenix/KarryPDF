package marabillas.loremar.karrypdf.contents.text

import marabillas.loremar.karrypdf.contents.PageContent
import marabillas.loremar.karrypdf.objects.PDFObject
import marabillas.loremar.karrypdf.objects.toPDFString

internal class TextElement internal constructor(
    val tj: PDFObject = "()".toPDFString(),
    val tf: String = "",
    val td: FloatArray = FloatArray(2),
    val ts: Float = 0f,
    val rgb: FloatArray = floatArrayOf(-1f, -1f, -1f)
) : PageContent {
    var width = 0f
    val fontResource = tf.substring(0, tf.indexOf(' '))
}