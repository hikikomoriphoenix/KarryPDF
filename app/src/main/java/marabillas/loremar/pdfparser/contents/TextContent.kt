package marabillas.loremar.pdfparser.contents

class TextContent internal constructor(
    val tj: String = "",
    val tf: String = "",
    val td: FloatArray = FloatArray(2),
    val ts: Float = 0f
) : PageContent