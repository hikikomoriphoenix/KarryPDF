package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.contents.image.ImageObjectParser
import marabillas.loremar.pdfparser.contents.text.TextObject
import marabillas.loremar.pdfparser.contents.text.TextObjectParser
import marabillas.loremar.pdfparser.objects.toName
import marabillas.loremar.pdfparser.toDouble

internal class ContentStreamParser {
    private val sb = StringBuilder()
    private val TF = "TF"
    private val BT = "BT"
    private val QSTART = "q"
    private val QEND = "Q"
    private val BI = "BI"
    private val DO = "Do"
    private val CM = "cm"
    private val operatorList = hashSetOf(TF, BT, QSTART, QEND, CM, DO) // TODO Add BI
    private val token = StringBuilder()

    /**
     * All cm operations within nested q and Q.
     */
    private val allCMs = mutableListOf<MutableList<FloatArray>>()

    fun parse(streamData: String): ArrayList<PageObject> {
        sb.clear().append(streamData)
        allCMs.add(mutableListOf())
        println("ContentStreamParser.parse begins")
        //println("stream->$streamData")
        val pageObjects = ArrayList<PageObject>()
        val textObjectParser = TextObjectParser()
        val imageObjectParser = ImageObjectParser()
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
                sb.startsWith(QSTART, i) -> {
                    allCMs.add(mutableListOf())
                    i++
                }
                sb.startsWith(QEND, i) -> {
                    allCMs.remove(allCMs.last())
                    i++
                }
                sb.startsWith(CM, i) -> {
                    val cm = getCM(i)
                    allCMs.last().add(cm)
                    i += 2
                }
                sb.startsWith(DO, i) -> {
                    val nameIndex = sb.lastIndexOf('/', i)
                    token.clear().append(sb, nameIndex, i - 1)
                    pageObjects.add(
                        XObject(getTotalX(), getTotalY(), token.toName())
                    )
                    i += 2
                }
                /*sb.startsWith(BI, i) -> {
                    // TODO parse inline image object
                }*/
            }
        }

        return pageObjects
    }

    private fun getCM(start: Int): FloatArray {
        var i = start - 1

        val opIndices = IntArray(7)
        // Mark the ending index for 6th operand.
        opIndices[6] = start

        var expectOperand = true
        var op = 5
        while (op >= 0) {
            if (expectOperand) {
                if (sb[i].isDigit()) {
                    expectOperand = false
                }
            } else if (i == -1 || sb[i] == ' ' || sb[i] == '\n' || sb[i] == '\r') {
                opIndices[op] = i + 1
                op--
                expectOperand = true
            }
            i--
        }
        val cm = FloatArray(6)
        for (j in 0 until 6) {
            token.clear().append(
                sb,
                opIndices[j],
                opIndices[j + 1] - 1
            )
            cm[j] = token.toDouble().toFloat()
        }

        return cm
    }

    private fun getTotalX(): Float {
        var tx = 0f
        var sx = 1f
        allCMs.forEach { q ->
            q.forEach { cm ->
                tx += cm[4]
                if (cm[0] < 0) {
                    sx *= (-1)
                }
            }
        }
        if (sx < 0) {
            tx = -tx
        }
        return tx
    }

    private fun getTotalY(): Float {
        var ty = 0f
        var sy = 0f
        allCMs.forEach { q ->
            q.forEach { cm ->
                ty += cm[5]
                if (cm[3] < 0) {
                    sy *= (-1)
                }
            }
        }
        if (sy < 0) {
            ty = -ty
        }
        return ty
    }
}