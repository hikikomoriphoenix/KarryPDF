package marabillas.loremar.karrypdf.contents.text

import marabillas.loremar.karrypdf.font.Font
import marabillas.loremar.karrypdf.utils.exts.toDouble

internal class TableDetector(
    private val textObjects: MutableList<TextObject>,
    private val fonts: HashMap<String, Font>
) {
    private val prevLineWideSpaces = mutableListOf<WideSpace>()
    private val sb = StringBuilder()

    fun detectTableComponents() {
        if (fonts.size > 0) {
            detectTables()
        }
        detectMultiLinearColumns()
    }

    private fun detectTables() {
        /**
         * List to hold wide spaces for current line
         */
        val currLineWideSpaces = mutableListOf<WideSpace>()

        /**
         * Column number to assign to the left of a detected divider for current line
         */
        var belowColumn = 0

        /**
         * Column number to assign to the left of a detected divider for previous line
         */
        var aboveColumn = 0

        /**
         * Current index of previous wide spaces
         */
        var w = 0

        var i = 0
        while (i < textObjects.count()) {
            val textObj = textObjects[i]
            //logd((textObj.first().tj as PDFString).value)

            // Check if the first TextObject of the current line is to the right of any divider of the previous line. Look
            // for the rightmost divider that is at the left of the TextObject. Initialize belowColumn based on the
            // number of columns to the left of the divider.
            if (prevLineWideSpaces.count() > 0 && textObj.td[1] != textObjects[i - 1].td[1]) {
                for (k in prevLineWideSpaces.lastIndex downTo 0) {
                    val aboveSpace = prevLineWideSpaces[k]
                    if (aboveSpace.isDivider && textObj.td[0] > aboveSpace.left) {
                        val aboveTextObj = textObjects[aboveSpace.leftTextObj]

                        // Get the distance between the left of the divider and the TextObject. Check if the divider
                        // extends below.
                        val diff = textObj.td[0] - aboveSpace.left
                        val spaceWidth = getSpaceWidthOfFirstElement(textObj)
                        if (spaceWidth != Float.MAX_VALUE && diff > (spaceWidth * 3f)) {
                            // Set the next column number of the current line to reflect the number of columns to skip
                            // based on the number of columns on the left of the current wide space of the previous line.
                            belowColumn = aboveTextObj.column + 1

                            // The current wide space will be evaluated next together with current TextObject. Set
                            // aboveColumn equal to the column number on the left of the current wide space.
                            aboveColumn = aboveTextObj.column
                            w = k

                            // Flag the space before the current TextObject as a WideSpace and also as a divider.
                            val currWideSpace =
                                WideSpace(
                                    left = aboveSpace.left,
                                    right = textObj.td[0],
                                    rightTextObj = i,
                                    isDivider = true
                                )
                            currLineWideSpaces.add(currWideSpace)

                            /*logd("Added widespace before first TextObject")
                            logd("w=$w belowColumn=$belowColumn aboveColumn=$aboveColumn")*/

                            break
                        } else {
                            continue
                        }
                    }
                }
            }

            // Check if another text object exists following the current text object on the same line. Otherwise, the
            // current TextObject is the last TextObject of the current line.
            if (i + 1 < textObjects.count() && textObj.td[1] == textObjects[i + 1].td[1]) {
                val rbArr = getLocationAndSpaceWidthOfRightBoundary(textObj)
                val diff = textObjects[i + 1].td[0] - rbArr[0]

                // Check if the distance between the two TextObjects is wider than 3 space characters. If it is, assume
                // a widespace between them, and proceed to detecting for divider. Else continue iterating through
                // TextObjects.
                if (rbArr[1] != Float.MAX_VALUE && diff > (rbArr[1] * 3f)) {
                    //logd("A wide space")
                    val currWideSpace =
                        WideSpace(
                            left = rbArr[0],
                            right = textObjects[i + 1].td[0],
                            leftTextObj = i,
                            rightTextObj = i + 1
                        )
                    currLineWideSpaces.add(currWideSpace)

                    // Iterate through wide spaces of the previous line. Each wide space will be evaluated together with
                    // the current wide space for divider detection.
                    while (w < prevLineWideSpaces.count()) {
                        //logd("w=$w")
                        val aboveWideSpace = prevLineWideSpaces[w]

                        // Check if current WideSpace and selected WideSpace from previous line forms a divider between
                        // two columns in a table.
                        if (isFormingColumnDivider(currWideSpace, aboveWideSpace, rbArr[1])) {
                            //logd("A column divider is formed")
                            aboveWideSpace.isDivider = true
                            currWideSpace.isDivider = true

                            /*logd("aboveColumn = $aboveColumn")
                            logd("belowColumn = $belowColumn")*/

                            // Set column number for text objects at the left of column boundary and whose column numbers
                            // are not set.
                            // Set column number for text objects in previous line.
                            setColumnNumberForTextObjectsToLeft(aboveWideSpace.leftTextObj, aboveColumn)
                            // Set column number for text objects in current line.
                            setColumnNumberForTextObjectsToLeft(i, belowColumn)

                            // Set next column number for previous line. Every time a divider is formed, and the above
                            // wide space is not a divider, a new divider is formed above, hence, the increase of column
                            // number of previous line.
                            aboveColumn++

                            // Set next column number for current line
                            belowColumn++

                            if (aboveColumn > belowColumn) {
                                // This implies that the current column spans for multiple columns. Hence, the next
                                // column number should reflect the number of columns being skipped. Set next column
                                // number for current line equal to next column number of previous line.
                                belowColumn = aboveColumn
                            }

                            // For the previous line: If column number is already set for text objects to the right of
                            // divider and is less than  or equal to the current column number of previous line, adjust
                            // their column numbers.
                            var r = aboveWideSpace.rightTextObj
                            if (textObjects[r].column != -1 && textObjects[r].column <= aboveColumn) {
                                // TODO Adjust column number of all previous rows.
                                //logd("Column number to the right of divider is adjusted in previous line")
                                val inc = aboveColumn - textObjects[r].column + 1
                                textObjects[r].column += inc
                                r++
                                while (r < textObjects.count()) {
                                    if (textObjects[r].column == -1)
                                        break
                                    if (textObjects[r].td[1] != textObjects[r - 1].td[1])
                                        break
                                    else
                                        textObjects[r].column += inc
                                    r++
                                }
                            }

                            // Evaluate next wide space. Check if it is also forming divider with the current wide space.
                            // If it is, then it implies that a blank empty Table cell is formed between these dividers.
                            w++
                        } else if (aboveWideSpace.left > currWideSpace.right) {
                            //logd("above.left > curr.right")
                            // If not forming divider and above wide space is to the right of current wide space.
                            // Allow the previous WideSpace to be checked in the next iteration.
                            if (w - 1 >= 0) {
                                w--
                            }
                            break
                        } else if (aboveWideSpace.isDivider) {
                            //logd("above.isdivider")
                            // If above wide space is not forming divider with current wide space but has already formed
                            // a divider previously.
                            // Update next column number for previous line. The column number for current line might now
                            // be less than the column number of previous line, implying that the current column of current
                            // line spans multiple columns.
                            aboveColumn++
                            w++
                        } else {
                            // Proceed to next wide space.
                            w++
                        }
                    }
                }
            } else { // If current TextObject is the last one in the line.
                if (belowColumn > 0) {
                    /*logd("belowColumn=$belowColumn")
                    logd("aboveColumn=$aboveColumn")*/
                    // Set the last TextObject's column first to avoid out of bounds in setColumnNumberForTextObjectsToLeft.
                    textObjects[i].column = belowColumn
                    setColumnNumberForTextObjectsToLeft(i - 1, belowColumn)
                    // Set column number for remaining text objects from previous line whose column number is not set yet
                    if (w >= prevLineWideSpaces.count())
                        w = prevLineWideSpaces.lastIndex
                    val aboveWideSpace = prevLineWideSpaces[w]
                    setColumnNumberForTextObjectsToLeft(aboveWideSpace.leftTextObj, aboveColumn)
                    var j = aboveWideSpace.rightTextObj
                    while (j < textObjects.count()) {
                        if (
                            textObjects[j].column >= 0 ||
                            textObjects[j].td[1] != textObjects[j - 1].td[1]
                        ) break

                        textObjects[j].column = aboveColumn
                        j++
                    }
                }

                belowColumn = 0
                aboveColumn = 0
                w = 0
                prevLineWideSpaces.clear()
                prevLineWideSpaces.addAll(currLineWideSpaces)
                //logd("Previous line's wide spaces are set. Count -> ${prevLineWideSpaces.size}")
                currLineWideSpaces.clear()
            }

            i++
        }
    }

    private fun setColumnNumberForTextObjectsToLeft(start: Int, column: Int) {
        var i = start
        while (i >= 0) {
            if (
                textObjects[i].column >= 0 ||
                textObjects[i].td[1] != textObjects[i + 1].td[1]
            ) break

            textObjects[i].column = column
            i--
        }
    }

    private fun isFormingColumnDivider(below: WideSpace, above: WideSpace, spaceWidth: Float): Boolean {
        // Check if spaces are positioned on top of one another.
        if (below.left > above.right || below.right < above.left)
            return false

        // Get the left and right boundary for the space common to the two.
        val left = if (below.left > above.left)
            below.left else above.left
        val right = if (below.right < above.right)
            below.right else above.right

        // Check if it is wide enough.
        val hole = right - left
        return hole > (spaceWidth * 3)
    }

    private fun getLocationAndSpaceWidthOfRightBoundary(textObj: TextObject): FloatArray {
        var rightmost = textObj.first().td[0]
        var spaceWidth = Float.MAX_VALUE
        var currX = textObj.first().td[0]
        textObj.forEachIndexed { i, e ->
            if (e != textObj.first())
                currX += e.td[0]
            if (i + 1 == textObj.count() || textObj.elementAt(i + 1).td[1] != 0f) {
                val elemWdth = e.width
                if (elemWdth == 0f) {
                    spaceWidth = Float.MAX_VALUE
                } else {
                    val rightBound = currX + elemWdth
                    if (rightBound > rightmost) {
                        rightmost = rightBound
                        val fKey = sb.clear().append(e.tf, 1, e.tf.indexOf(' ')).toString()
                        val fSize = sb.clear().append(e.tf, e.tf.indexOf(' ') + 1, e.tf.length).toDouble().toFloat()
                        val widths = fonts[fKey]?.widths
                        val spWdth = widths?.get(32)
                        val missingWidth = widths?.get(-1)
                        spaceWidth = spWdth ?: missingWidth ?: Float.MAX_VALUE
                        if (spaceWidth != Float.MAX_VALUE) {
                            spaceWidth = (spaceWidth / 1000) * fSize * textObj.scaleX
                        }
                    }
                }
            }
        }
        return floatArrayOf(rightmost, spaceWidth)
    }

    private fun detectMultiLinearColumns() {
        var i = 0
        while (i < textObjects.count() && textObjects[i].column == -1) {
            if (isTextObjectMultiLinear(textObjects[i])) {
                val rowStart = findRowStart(i)
                val rowEnd = findRowEnd(i)
                if (i > rowStart || i < rowEnd) {
                    var column = 0
                    for (j in rowStart..rowEnd) {
                        textObjects[j].column = column++
                    }
                }
                i = rowEnd + 1
            } else {
                i++
            }
        }
    }

    private fun isTextObjectMultiLinear(textObj: TextObject): Boolean {
        textObj.forEach { textElement ->
            if (textElement != textObj.first()) {
                if (textElement.td[1] != 0f)
                    return true
            }
        }
        return false
    }

    private fun findRowStart(current: Int): Int {
        var i = current
        while (i >= 0) {
            if (i - 1 < 0 || textObjects[i].td[1] != textObjects[i - 1].td[1])
                return i
            i--
        }
        return 0
    }

    private fun findRowEnd(current: Int): Int {
        var i = current
        while (i < textObjects.count()) {
            if (i + 1 > textObjects.lastIndex || textObjects[i].td[1] != textObjects[i + 1].td[1])
                return i
            i++
        }
        return textObjects.lastIndex
    }

    private fun getSpaceWidthOfFirstElement(textObj: TextObject): Float {
        var spaceWidth: Float
        val e = textObj.first()

        val fKey = sb.clear().append(e.tf, 1, e.tf.indexOf(' ')).toString()
        val fSize = sb.clear().append(e.tf, e.tf.indexOf(' ') + 1, e.tf.length).toDouble().toFloat()

        val widths = fonts[fKey]?.widths
        val missingWidth = widths?.get(-1)

        spaceWidth = widths?.get(32) ?: missingWidth ?: Float.MAX_VALUE
        if (spaceWidth != Float.MAX_VALUE) {
            spaceWidth = (spaceWidth / 1000) * fSize * textObj.scaleX
        }
        return spaceWidth
    }
}

data class WideSpace(
    val left: Float,
    val right: Float,
    val leftTextObj: Int = -1,
    val rightTextObj: Int = -1,
    var isDivider: Boolean = false
)