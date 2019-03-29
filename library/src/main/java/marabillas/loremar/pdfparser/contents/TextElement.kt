package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.toPDFString

internal class TextElement internal constructor(
    val tj: PDFObject = "()".toPDFString(),
    val tf: String = "",
    val td: FloatArray = FloatArray(2),
    val ts: Float = 0f
) : PageContent