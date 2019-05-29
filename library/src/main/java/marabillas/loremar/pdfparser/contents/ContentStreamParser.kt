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
    private val RG = "RG"
    private val RGX = "rg"
    private val K = "K"
    private val KX = "k"
    private val operatorList = hashSetOf(TF, BT, QSTART, QEND, CM, DO, RG, RGX, K, KX) // TODO Add BI
    private val token = StringBuilder()

    private val identityCm = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)
    private val noRgb = floatArrayOf(-1f, -1f, -1f, -1f)
    private val gsStack = Stack<GraphicsState>()
    private val gsHolders = mutableListOf<GraphicsState>()

    fun parse(streamData: String): ArrayList<PageObject> {
        sb.clear().append(streamData)

        // Initialize graphics state stack
        repeat(4) {
            gsHolders.add(GraphicsState())
        }
        gsStack.push(gsHolders[0])

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
                sb.startsWith(RG, i) || sb.startsWith(RGX, i) -> {
                    gsStack.lastElement().rgb = getRGB(i)
                    i += 2
                }
                sb.startsWith(K, i) || sb.startsWith(KX, i) -> {
                    val cmyk = getCMYK(i)
                    gsStack.lastElement().rgb = CmykToRgbConverter.inst.convert(cmyk)
                    i++
                }
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
                    val cm = gsStack.lastElement().cm
                    val rgb = gsStack.lastElement().rgb
                    i = textObjectParser.parse(sb, textObj, tf, i + 2, cm, rgb)
                    textObj.scaleX = Math.abs(cm[0])
                    textObj.scaleY = Math.abs(cm[3])
                    pageObjects.add(textObj)
                    //println("textObj -> ${textObj.getX()}, ${textObj.getY()}")
                }
                i == -1 -> i = sb.length
                sb.startsWith(QSTART, i) -> {
                    //println("QSTART")

                    // If next index for stack is greater than gsHolders' size, add a new GraphicsState
                    val index = gsStack.size
                    if (index >= gsHolders.size) {
                        gsHolders.add(GraphicsState())
                    }

                    // Reset variable in selected GraphicsState
                    gsHolders[index].cm = identityCm
                    gsHolders[index].rgb = noRgb
                    gsHolders[index].tf = null

                    gsStack.push(gsHolders[index])
                    i++
                }
                sb.startsWith(QEND, i) -> {
                    //println("QEND")
                    gsStack.pop()
                    i++
                }
                sb.startsWith(CM, i) -> {
                    val cm = getCM(i)
                    //println("cm -> ${cm[0]}, ${cm[1]}, ${cm[2]}, ${cm[3]}, ${cm[4]}, ${cm[5]}")
                    gsStack.lastElement().cm = cm
                    i += 2
                }
                sb.startsWith(DO, i) -> {
                    //println("Do XObject")
                    val nameIndex = sb.lastIndexOf('/', i)
                    token.clear().append(sb, nameIndex, i - 1)

                    var x = gsStack.lastElement().cm[4]
                    var y = gsStack.lastElement().cm[5]
                    val sx = gsStack.lastElement().cm[0]
                    val sy = gsStack.lastElement().cm[3]
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
        return getOperands(start, 6)
    }

    private fun getRGB(start: Int): FloatArray {
        return getOperands(start, 3)
    }

    private fun getCMYK(start: Int): FloatArray {
        return getOperands(start, 4)
    }

    private fun getOperands(start: Int, numOperands: Int): FloatArray {
        // Set starting point to be the space after the last operand and before the operator.
        var i = start - 1
        // Initialize indices. Include the index of space after the last operand. This allows extraction of last operand.
        val opIndices = IntArray(numOperands + 1)
        // When extracting operands, ending index is taken as index of next operand minus 1. Here, the index of operator
        // is assumed as the index of next operand.
        opIndices[numOperands] = start

        // Get indices of each operand
        var expectOperand = true
        var op = numOperands - 1
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

        // Extract operands
        val operands = FloatArray(numOperands)
        for (j in 0 until numOperands) {
            token.clear().append(
                sb,
                opIndices[j],
                opIndices[j + 1] - 1
            )
            operands[j] = token.toDouble().toFloat()
        }

        return operands

    }

    inner class GraphicsState(
        var cm: FloatArray = identityCm,
        var rgb: FloatArray = floatArrayOf(-1f, -1f, -1f),
        var tf: String? = null
    )
}