package marabillas.loremar.pdfparser.contents

data class TextContent internal constructor(
    val tj: String = "",
    val tf: String = "",
    val td: String = "",
    val tm: String = ""
) : PageContent