package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.contents.image.ImageObjectParser
import marabillas.loremar.pdfparser.contents.text.TextObject
import marabillas.loremar.pdfparser.contents.text.TextObjectParser
import marabillas.loremar.pdfparser.exceptions.UnsupportedPDFElementException
import marabillas.loremar.pdfparser.objects.toName
import marabillas.loremar.pdfparser.toDouble
import java.util.*
import kotlin.collections.ArrayList

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

    private val ctmStack = Stack<FloatArray>()
    private val identityCm = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)

    fun parse(streamData: String): ArrayList<PageObject> {
        sb.clear().append(streamData)
        ctmStack.push(identityCm)
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
                    val cm = ctmStack.last()
                    i = textObjectParser.parse(sb, textObj, tf, i + 2, cm)
                    textObj.scaleX = Math.abs(cm[0])
                    textObj.scaleY = Math.abs(cm[3])
                    pageObjects.add(textObj)
                    //println("textObj -> ${textObj.getX()}, ${textObj.getY()}")
                }
                i == -1 -> i = sb.length
                sb.startsWith(QSTART, i) -> {
                    //println("QSTART")
                    ctmStack.push(identityCm)
                    i++
                }
                sb.startsWith(QEND, i) -> {
                    //println("QEND")
                    ctmStack.pop()
                    i++
                }
                sb.startsWith(CM, i) -> {
                    val cm = getCM(i)
                    //println("cm -> ${cm[0]}, ${cm[1]}, ${cm[2]}, ${cm[3]}, ${cm[4]}, ${cm[5]}")
                    ctmStack.pop()
                    ctmStack.push(cm)
                    i += 2
                }
                sb.startsWith(DO, i) -> {
                    //println("Do XObject")
                    val nameIndex = sb.lastIndexOf('/', i)
                    token.clear().append(sb, nameIndex, i - 1)

                    var x = ctmStack.last()[4]
                    var y = ctmStack.last()[5]
                    val sx = ctmStack.last()[0]
                    val sy = ctmStack.last()[3]
                    if (sx < 0)
                        x = -x
                    if (sy < 0)
                        y = -y
                    pageObjects.add(
                        XObject(x, y, token.toName())
                    )
                    i += 2
                    //println("xObj -> ${pageObjects.last().getX()}, ${pageObjects.last().getY()}")
                }
                sb.startsWith(BI, i) -> {
                    // TODO parse inline image object
                    throw UnsupportedPDFElementException("Extraction of inline image objects is not yet supported.")
                }
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
}