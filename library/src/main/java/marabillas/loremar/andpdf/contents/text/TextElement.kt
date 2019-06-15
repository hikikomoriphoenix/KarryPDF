package marabillas.loremar.andpdf.contents.text

import marabillas.loremar.andpdf.contents.PageContent
import marabillas.loremar.andpdf.objects.PDFObject
import marabillas.loremar.andpdf.objects.toPDFString

internal class TextElement internal constructor(
    val tj: PDFObject = "()".toPDFString(),
    val tf: String = "",
    val td: FloatArray = FloatArray(2),
    val ts: Float = 0f,
    val rgb: FloatArray = floatArrayOf(-1f, -1f, -1f)
) : PageContent {
    var width = 0f
}