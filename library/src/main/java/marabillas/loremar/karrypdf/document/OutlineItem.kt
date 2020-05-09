package marabillas.loremar.karrypdf.document

data class OutlineItem(
    val title: String,
    val pageIndex: Int,
    val subItems: List<OutlineItem>
)