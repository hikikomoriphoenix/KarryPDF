package marabillas.loremar.pdfparser.contents

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.TimeCounter
import marabillas.loremar.pdfparser.contents.image.ImageContent
import marabillas.loremar.pdfparser.contents.image.ImageObject
import marabillas.loremar.pdfparser.contents.text.TextContentAdapter
import marabillas.loremar.pdfparser.contents.text.TextContentAnalyzer
import marabillas.loremar.pdfparser.contents.text.TextObject
import marabillas.loremar.pdfparser.font.Font

internal class PageContentAdapter(
    private val pageObjects: ArrayList<PageObject>,
    private val fonts: SparseArrayCompat<Font>
) {
    private val textContentAnalyzer = TextContentAnalyzer()
    private val textContentAdapter = TextContentAdapter()
    private val textObjects = mutableListOf<TextObject>()

    fun getPageContents(): ArrayList<PageContent> {
        // Arrange objects to correct vertical order then to horizontal order.
        pageObjects.sortWith(
            compareBy(
                { -it.getY() },
                { it.getX() })
        )

        val contents = ArrayList<PageContent>()

        var i = 0
        while (i < pageObjects.size) {
            when (val next = pageObjects[i]) {
                is TextObject -> {
                    /* val array = pageObjects
                         .subList(i, pageObjects.size)
                         .takeWhile { it is TextObject }
                         .map { it as TextObject }
                         .toTypedArray()
                         .copyOf()
                     val textObjs = array.toCollection(ArrayList())
                     skip = textObjs.size
                     val textContentGroups = textContentAnalyzer.analyze(textObjs)
                     val textContents = textContentAdapter.getContents(textContentGroups, pageFonts)
                     contents.addAll(textContents)*/

                    TimeCounter.reset()
                    textObjects.clear()
                    textObjects.add(next)

                    i++
                    var nextTextObject: PageObject
                    while (i < pageObjects.size) {
                        nextTextObject = pageObjects[i]
                        if (nextTextObject is TextObject) {
                            textObjects.add(nextTextObject)
                        } else {
                            break
                        }
                        i++
                    }
                    println("Collecting successive TextObjects -> ${TimeCounter.getTimeElapsed()} ms")
                    TimeCounter.reset()

                    val textContentGroups = textContentAnalyzer.analyze(textObjects, fonts)
                    println("TextContentAnalyzer.analyze -> ${TimeCounter.getTimeElapsed()} ms")

                    TimeCounter.reset()
                    val textContents = textContentAdapter.getContents(textContentGroups, fonts)
                    println("TextContentAdapter.getContents -> ${TimeCounter.getTimeElapsed()} ms")

                    contents.addAll(textContents)
                }
                is ImageObject -> {
                    contents.add(
                        ImageContent(next.imageData)
                    )
                    i++
                }
                // TODO Process other types of objects and add results to contents.
            }
        }

        return contents
    }
}