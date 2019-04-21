package marabillas.loremar.pdfparser.contents

internal class ContentStreamParser {
    private val sb = StringBuilder()
    private val TF = "TF"
    private val BT = "BT"
    private val operatorList = hashSetOf(TF, BT)
    private val token = StringBuilder()

    fun parse(streamData: String): ArrayList<PageObject> {
        sb.clear().append(streamData)
        println("ContentStreamParser.parse begins")
        val pageObjects = ArrayList<PageObject>()
        val textObjectParser = TextObjectParser()
        val tf = StringBuilder()
        var i = 0

        while (i < sb.length) {
            i = sb.indexOfAny(operatorList, i)
            when {
                sb.startsWith(TF, i) -> {
                    var j = 3
                    while (true) {
                        if (sb[i - j] == 'F') {
                            token.clear().append(sb, i - j, i - 1)
                            break
                        }
                        j++
                    }
                    tf.clear().append(token)
                    i += 2
                }
                sb.startsWith(BT, i) -> {
                    val textObj = TextObject()
                    i = textObjectParser.parse(sb, textObj, tf, i + 2)
                    pageObjects.add(textObj)
                }
                i == -1 -> i = sb.length
            }
        }

        return pageObjects
    }
}