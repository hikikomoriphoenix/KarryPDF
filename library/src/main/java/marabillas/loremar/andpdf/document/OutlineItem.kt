package marabillas.loremar.andpdf.document

data class OutlineItem(
    val title: String,
    val pageIndex: Int,
    val subItems: List<OutlineItem>
)