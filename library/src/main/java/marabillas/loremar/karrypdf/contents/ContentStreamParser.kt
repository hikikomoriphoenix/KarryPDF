package marabillas.loremar.karrypdf.contents

import marabillas.loremar.karrypdf.contents.image.ImageObject
import marabillas.loremar.karrypdf.contents.image.ImageObjectParser
import marabillas.loremar.karrypdf.contents.text.TextObject
import marabillas.loremar.karrypdf.contents.text.TextObjectParser
import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.objects.toName
import marabillas.loremar.karrypdf.utils.exts.indexOfClosingChar
import marabillas.loremar.karrypdf.utils.exts.isEnclosingAt
import marabillas.loremar.karrypdf.utils.exts.isWhiteSpaceAt
import marabillas.loremar.karrypdf.utils.exts.toDouble
import marabillas.loremar.karrypdf.utils.logd
import marabillas.loremar.karrypdf.utils.loge
import java.util.*
import kotlin.collections.ArrayList

internal class ContentStreamParser(private val context: KarryPDFContext) {
    private val sb = StringBuilder()
    private val token = StringBuilder()

    private val identityCm = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)
    private val noRgb = floatArrayOf(-1f, -1f, -1f, -1f)
    private val gsStack = Stack<GraphicsState>()
    private val gsHolders = mutableListOf<GraphicsState>()

    fun parse(streamData: String, obj: Int, gen: Int): ArrayList<PageObject> {
        try {
            sb.clear().append(streamData)

            // Initialize graphics state stack
            repeat(4) {
                gsHolders.add(GraphicsState())
            }
            gsStack.push(gsHolders[0])

            logd("ContentStreamParser.parse begins")
            //logd("stream->$streamData")
            val pageObjects = ArrayList<PageObject>()
            val textObjectParser =
                TextObjectParser(
                    context,
                    obj,
                    gen
                )
            val imageObjectParser =
                ImageObjectParser(
                    context,
                    obj,
                    gen
                )
            val tf = StringBuilder()
            var i = 0

            while (i < sb.length) {
                i = nextOperator(i)
                if (i >= sb.length) break

                when {
                    // RG or rg
                    (i + 1 < sb.length) && ((sb[i] == 'R' && sb[i + 1] == 'G') || (sb[i] == 'r' && sb[i + 1] == 'g')) -> {
                        gsStack.lastElement().rgb = getRGB(i)
                        i += 2
                    }
                    // K or k
                    sb[i] == 'K' || sb[i] == 'k' -> {
                        val cmyk = getCMYK(i)
                        gsStack.lastElement().rgb =
                            CmykToRgbConverter.convert(
                                cmyk
                            )
                        i++
                    }
                    // TF
                    i + 1 < sb.length && sb[i] == 'T' && (sb[i + 1] == 'F' || sb[i + 1] == 'f') -> {
                        var j = 3
                        while (true) {
                            if (sb[i - j] == '/') {
                                token.clear().append(sb, i - j + 1, i - 1)
                                break
                            }
                            j++
                        }
                        gsStack.lastElement().tf = token.toString()
                        tf.clear().append(token)
                        textObjectParser.fontSizeOverride = null
                        i += 2
                    }
                    // BT
                    i + 1 < sb.length && sb[i] == 'B' && sb[i + 1] == 'T' -> {
                        val textObj =
                            TextObject()
                        val cm = gsStack.lastElement().cm
                        val rgb = gsStack.lastElement().rgb
                        i = textObjectParser.parse(sb, textObj, tf, i + 2, rgb)

                        if (textObj.count() > 0) {
                            textObj.transformMatrix = cm
                            textObj.computeAllElementsTransformation()
                            pageObjects.add(textObj)
                        }
                        //logd("textObj -> ${textObj.getX()}, ${textObj.getY()}")
                    }
                    // q
                    sb[i] == 'q' -> {
                        //logd("QSTART")

                        // If next index for stack is greater than gsHolders' size, add a new GraphicsState
                        val index = gsStack.size
                        if (index >= gsHolders.size) {
                            gsHolders.add(GraphicsState())
                        }

                        // Use values from previous element in the stack
                        gsHolders[index].cm = gsStack.lastElement().cm.copyOf()
                        gsHolders[index].rgb = gsStack.lastElement().rgb.copyOf()
                        gsHolders[index].tf = gsStack.lastElement().tf

                        gsStack.push(gsHolders[index])
                        i++
                    }
                    // Q
                    sb[i] == 'Q' -> {
                        //logd("QEND")
                        gsStack.pop()
                        if (gsStack.size > 0) {
                            tf.clear().append(gsStack.lastElement().tf)
                        } else {
                            // Restore Graphics State to default
                            gsHolders[0].cm = identityCm
                            gsHolders[0].rgb = floatArrayOf(-1f, -1f, -1f)
                            gsHolders[0].tf = ""
                            gsStack.add(gsHolders[0])
                            tf.clear()
                        }
                        i++
                    }
                    // cm
                    i + 1 < sb.length && sb[i] == 'c' && sb[i + 1] == 'm' -> {
                        val cm = getCM(i)
                        //logd("cm -> ${cm[0]}, ${cm[1]}, ${cm[2]}, ${cm[3]}, ${cm[4]}, ${cm[5]}")
                        gsStack.lastElement().cm = cm
                        i += 2
                    }
                    // Do
                    i + 1 < sb.length && sb[i] == 'D' && sb[i + 1] == 'o' -> {
                        //logd("Do XObject")
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
                            XObject(
                                x,
                                y,
                                token.toName()
                            )
                        )
                        i += 2
                        //logd("xObj -> ${pageObjects.last().getX()}, ${pageObjects.last().getY()}")
                    }
                    // BI
                    i + 1 < sb.length && sb[i] == 'B' && sb[i + 1] == 'I' -> {
                        var x = gsStack.lastElement().cm[4]
                        var y = gsStack.lastElement().cm[5]
                        val sx = gsStack.lastElement().cm[0]
                        val sy = gsStack.lastElement().cm[3]
                        if (sx < 0)
                            x = -x
                        if (sy < 0)
                            y = -y
                        val imageObj =
                            ImageObject(
                                x,
                                y
                            )

                        i = imageObjectParser.parse(sb, imageObj, i + 2)
                    }
                }
            }

            return pageObjects
        } catch (e: Exception) {
            loge("Exception on parsing content stream", e)
            return arrayListOf()
        }
    }

    private fun nextOperator(startIndex: Int): Int {
        var i = startIndex
        while (i < sb.length) {
            if (
                (i + 1 < sb.length && sb[i] == 'T' && (sb[i + 1] == 'F' || sb[i + 1] == 'f'))
                || (i + 1 < sb.length && sb[i] == 'B' && sb[i + 1] == 'T')
                || sb[i] == 'q'
                || sb[i] == 'Q'
                || (i + 1 < sb.length && sb[i] == 'c' && sb[i + 1] == 'm')
                || (i + 1 < sb.length && sb[i] == 'D' && sb[i + 1] == 'o')
                || (i + 1 < sb.length && sb[i] == 'R' && sb[i + 1] == 'G')
                || (i + 1 < sb.length && sb[i] == 'r' && sb[i + 1] == 'g')
                || sb[i] == 'K'
                || sb[i] == 'k'
                || (i + 1 < sb.length && sb[i] == 'B' && sb[i + 1] == 'I')
            ) {
                return i
            } else if (sb.isEnclosingAt(i)) {
                i = sb.indexOfClosingChar(i)
            } else if (sb[i] == '/') {
                i++
                while (i < sb.length && !sb.isEnclosingAt(i) && !sb.isWhiteSpaceAt(i) && sb[i] != '/')
                    i++
                continue
            }
            i++
        }
        return i
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
        var tf: String = ""
    )
}