package marabillas.loremar.pdfparser.contents

import android.graphics.Typeface

internal class PageContentAdapter(
    private val pageObjects: ArrayList<PageObject>,
    private val pageFonts: HashMap<String, Typeface>
) {

    fun getPageContents(): ArrayList<PageContent> {
        // Arrange objects to correct vertical order.
        pageObjects.sortWith(compareByDescending { it.getY() })

        val contents = ArrayList<PageContent>()

        var i = 0
        while (i < pageObjects.size) {
            val next = pageObjects[i]
            var skip = 0
            when (next) {
                is TextObject -> {
                    val array = pageObjects
                        .subList(i, pageObjects.size)
                        .takeWhile { it is TextObject }
                        .map { it as TextObject }
                        .toTypedArray()
                        .copyOf()
                    val textObjs = array.toCollection(ArrayList())
                    skip = textObjs.size
                    val textContentGroups = TextContentAnalyzer(textObjs)
                        .analyze()
                    val textContents = TextContentAdapter(textContentGroups, pageFonts)
                        .getContents()
                    contents.addAll(textContents)
                }
                // TODO Process other types of objects and add results to contents.
            }
            i += skip
        }
        return contents
    }
}