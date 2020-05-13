package marabillas.loremar.karrypdf.contents.text

import android.util.Log
import androidx.collection.SparseArrayCompat
import marabillas.loremar.karrypdf.contents.ContentGroup
import marabillas.loremar.karrypdf.font.Font
import marabillas.loremar.karrypdf.objects.Numeric
import marabillas.loremar.karrypdf.objects.PDFArray
import marabillas.loremar.karrypdf.objects.PDFString
import marabillas.loremar.karrypdf.objects.toPDFString
import kotlin.math.abs

internal class TextContentAnalyzer(textObjs: MutableList<TextObject> = mutableListOf()) {
    internal val contentGroups = ArrayList<ContentGroup>()
    private val textObjects = mutableListOf<TextObject>()
    private val sb = StringBuilder()

    private var currTextGroup =
        TextGroup()
    private var table = Table()
    private var currLine = ArrayList<TextElement>()

    private val fonts = HashMap<String, Font>()
    private var isDetectTables = false

    init {
        textObjects.clear()
        textObjects.addAll(textObjs)
    }

    private fun resetAnalyzer() {
        contentGroups.clear()
        textObjects.clear()
        sb.clear()
        currTextGroup = TextGroup()
        table = Table()
        currLine.clear()
        fonts.clear()
    }

    fun analyze(
        textObjs: MutableList<TextObject>,
        fonts: HashMap<String, Font>
    ): ArrayList<ContentGroup> {
        resetAnalyzer()

        textObjects.addAll(textObjs)
        this.fonts.putAll(fonts)

        val measureWidths = isMeasureWidths()
        if (measureWidths) {
            computeElementWidths()
        }

        // If tj values are arrays resulting from TJ operator, determine from the number values between strings
        // whether to add space or not while concatenating strings. A negative value indicates a shift to right adding
        // space between characters. If the value amounts to a space width, add space while concatenating surrounding
        // strings.
        handleTJArrays()

        // Tables are detected by looking for wide spaces placed on top of each other. These wide spaces serve as
        // dividers between table columns. Tables are also detected by looking for multi-linear TextObjects placed
        // horizontally adjacent to each other.
        if (isDetectTables) {
            try {
                TableDetector(
                    textObjects,
                    fonts
                ).detectTableComponents()
            } catch (e: Exception) {
                // Log exception
                Log.e("${javaClass.name}.analyze", "Exception in TableDetection: ${e.message}")
                e.stackTrace.forEach {
                    Log.e("${javaClass.name}.analyze", "$it")
                }
                // Set all TextObjects to not belong to a table column
                textObjects.forEach {
                    it.column = -1
                }
                // Restart analyze with table detection turned off
                isDetectTables = false
                analyze(textObjects, fonts)
                isDetectTables = true
                return contentGroups
            }
        }

        // Group texts in the same line or in adjacent lines with line-spacing less than font size.
        groupTexts()

        // Check if lines end with a period. If yes, then lines stay as they were. If not, then proceed analysis.
        checkForListTypeTextGroups()

        // Estimate the width of the page by getting the largest width of a line of texts
        val w = getLargestWidth(measureWidths)

        // If line is almost as long as the width of page, then append the next line in the TextGroup.
        formParagraphs(w, measureWidths)

        // Add indents
        addIndents()

        // Convert adjacent elements with same tf and rgb into one element
        mergeElementsWithSameFontAndColor()

        deleteBlankLines()

        return contentGroups
    }

    fun detectTables(isDetectTables: Boolean): TextContentAnalyzer {
        this.isDetectTables = isDetectTables
        return this
    }

    private fun isMeasureWidths(): Boolean {
        fonts.forEach {
            if (it.value.widths.get(-1) == null) {
                return false
            }
        }
        return true
    }

    internal fun computeElementWidths() {
        for (i in 0 until textObjects.size) {
            val textObj = textObjects[i]
            textObj.forEach { textElem ->
                val tj = textElem.tj

                val widths = fonts[textElem.fontResource]?.widths

                if (tj is PDFArray) {
                    tj.forEach {
                        if (it is PDFString) {
                            if (widths != null) {
                                textElem.width += computeStringWidth(it, widths, textElem.scaleX)
                            }
                        } else if (it is Numeric) {
                            val num = -(it.value.toFloat())
                            val offset = (num / 1000) * textElem.scaleX
                            textElem.width += offset
                        }
                    }
                } else if (tj is PDFString) {
                    if (widths != null) {
                        textElem.width += computeStringWidth(tj, widths, textElem.scaleX)
                    }
                }
            }
        }
    }

    private fun computeStringWidth(
        string: PDFString,
        widths: SparseArrayCompat<Float>,
        scaleX: Float
    ): Float {
        var width = 0f
        string.value.forEach { c ->
            val w = widths[c.toInt()] ?: widths[-1]
            w?.let { width += ((w / 1000f) * scaleX) }
        }
        return width
    }

    internal fun handleTJArrays() {
        /*textObjects.forEach { texObj ->
            val spW = getSpaceWidth(texObj)
            handleSpacing(spW, texObj)
        }*/
        handleSpacing(fontSpaceWidths())
    }

    private fun fontSpaceWidths(): HashMap<String, Float> {
        // Font key to space width mapping
        val fsw = HashMap<String, Float>()
        // Width to count mapping for each font
        val fws = HashMap<String, HashMap<Float, Int>>()
        // Get existing space widths from each font in fonts array
        val esw = existingSpaceWidths()
        fsw.putAll(esw)

        for (i in 0 until textObjects.size) {
            val textObj = textObjects[i]
            textObj.forEach forEachTextElem@{ textElem ->
                // Get font key
                val f = textElem.fontResource

                // Existing space widths from fonts array will be used
                if (esw.containsKey(f)) return@forEachTextElem

                // Evaluate each tj number to determine space width for current font. The space width would be the width
                // with the most count.
                val tj = textElem.tj
                if (tj is PDFArray) {
                    tj.forEach {
                        // Process if object is a negative number. A negative number indicates that the next character
                        // will be adjusted to the right, increasing its spacing.
                        if (it is Numeric && it.value.toFloat() < 0) {
                            // Positive value is used for space widths.
                            val w = -it.value.toFloat()
                            if (fws[f] != null) {
                                // Get width's count. Increment it and update fws with new count.
                                val count = fws[f]?.get(w) ?: 0
                                fws[f]?.put(w, count + 1)

                                // Evaluate for new space width
                                val swCount = fws[f]?.get(fsw[f]) ?: 0
                                if (count + 1 > swCount) {
                                    fsw[f] = w
                                }
                            } else {
                                // If width has no count value, initialize fws and fsw with new width.
                                fws[f] = hashMapOf()
                                fws[f]?.put(w, 1)
                                fsw[f] = w
                            }
                        }
                    }
                }
            }
        }

        return fsw
    }

    private fun existingSpaceWidths(): HashMap<String, Float> {
        val existingSpaceWidths = HashMap<String, Float>()
        fonts.forEach {
            val font = it.value
            val spaceWidth = font.widths.extractMinWidthIntoSpaceWidth()
            if (spaceWidth is Float) {
                existingSpaceWidths[it.key] = spaceWidth
            }
        }
        return existingSpaceWidths
    }

    private fun handleSpacing(spaceWidths: HashMap<String, Float>) {
        for (i in 0 until textObjects.size) {
            val textObj = textObjects[i]

            textObj.forEachIndexed { index, textElement ->
                // Get TextElement's font's space width
                val spaceWidth = spaceWidths[textElement.fontResource] ?: 0f

                if (textElement.tj is PDFArray) {
                    val tj = textElement.tj as PDFArray
                    sb.clear().append('(')
                    tj.forEach {
                        if (it is PDFString)
                            sb.append(it.value) // If string, append
                        else if (it is Numeric) {
                            val num = -it.value.toFloat()
                            if (num >= 0.6 * spaceWidth)
                                sb.append(' ')
                        }
                    }
                    sb.append(')')
                    textElement.tj = sb.toString().toPDFString()
                }
            }
        }
    }

    internal fun groupTexts() {
        currTextGroup = TextGroup()
        contentGroups.add(currTextGroup)
        table = Table()
        currLine = ArrayList()

        for (index in 0 until textObjects.size) {
            val textObj = textObjects[index]

            var prevTextObj: TextObject? = null
            if (index > 0)
                prevTextObj = textObjects[index - 1]

            when {
                textObj.column >= 0 -> {
                    when {
                        // If first cell of table or if not in the same row, then add new row
                        table.size() == 0 || textObj.getY() != (prevTextObj as TextObject).getY() -> {
                            // Add new table if first cell of table
                            if (table.size() == 0) {
                                contentGroups.add(table)
                            }

                            // Add new row
                            val row =
                                Table.Row()
                            table.add(row)

                            val blankCells = textObj.column

                            // Add empty cells
                            repeat(blankCells) {
                                val textGroup =
                                    TextGroup()
                                textGroup.add(
                                    arrayListOf(
                                        TextElement(
                                            tj = "( )".toPDFString(),
                                            tf = "/F1000000 1000000"
                                        )
                                    )
                                )
                                val cell =
                                    Table.Cell()
                                cell.add(textGroup)
                                row.add(cell)
                            }

                            // Add new cell
                            currTextGroup =
                                TextGroup()
                            val cell =
                                Table.Cell()
                            cell.add(currTextGroup)
                            row.add(cell)

                            var prevY = textObj.getY()
                            textObj.forEach {
                                val dty = prevY - it.ty
                                sortGroup(it, dty)
                                prevY = it.ty
                            }
                        }
                        // If in the same column of previous TextObject
                        textObj.column == prevTextObj.column -> {
                            var prevY = textObj.getY()
                            textObj.forEach {
                                val dty = prevY - it.ty
                                sortGroup(it, dty)
                                prevY = it.ty
                            }
                        }
                        else -> {
                            val colDiff = textObj.column - prevTextObj.column
                            val blankCells = colDiff - 1

                            // Add empty cells
                            repeat(blankCells) {
                                val textGroup =
                                    TextGroup()
                                textGroup.add(
                                    arrayListOf(
                                        TextElement(
                                            tj = "( )".toPDFString(),
                                            tf = "/F1000000 1000000"
                                        )
                                    )
                                )
                                val cell =
                                    Table.Cell()
                                cell.add(textGroup)
                                table[table.size() - 1].add(cell)
                            }

                            // Add new cell
                            currTextGroup =
                                TextGroup()
                            val cell =
                                Table.Cell()
                            cell.add(currTextGroup)
                            table[table.size() - 1].add(cell)
                            var prevY = textObj.getY()
                            textObj.forEach {
                                val dty = prevY - it.ty
                                sortGroup(it, dty)
                                prevY = it.ty
                            }
                        }
                    }
                }
                table.size() > 0 -> {
                    table =
                        Table() // Reset to empty table
                    currTextGroup =
                        TextGroup()
                    contentGroups.add(currTextGroup)

                    var prevX = textObj.getX()
                    var prevY = textObj.getY()
                    textObj.forEach {
                        val dty = prevY - it.ty
                        sortGroup(it, dty, it.tx, prevX)
                        prevX = it.tx
                        prevY = it.ty
                    }
                }
                else -> {
                    var prevX = prevTextObj?.last()?.tx ?: textObj.getX()
                    var prevY = prevTextObj?.last()?.ty ?: textObj.getY()
                    textObj.forEach {
                        val dty = prevY - it.ty
                        sortGroup(it, dty, it.tx, prevX)
                        prevX = it.tx
                        prevY = it.ty
                    }
                }
            }
        }
    }

    private fun sameLine(dty: Float, textHeight: Float): Boolean {
        return abs(dty) < textHeight
    }

    private fun near(dty: Float, textHeight: Float): Boolean {
        return abs(dty) < textHeight * 2
    }

    private fun newLine(textElement: TextElement, xPos: Float) {
        currLine = ArrayList()
        currLine.add(textElement)
        currTextGroup.add(currLine)
        currTextGroup.addX(xPos)
    }

    private fun newTextGroup(textElement: TextElement, xPos: Float) {
        currTextGroup = TextGroup()
        newLine(textElement, xPos)

        if (table.size() > 0) {
            val lastRow = table[table.size() - 1]
            lastRow[lastRow.size() - 1].add(currTextGroup)
        } else {
            contentGroups.add(currTextGroup)
        }
    }

    private fun sortGroup(
        textElement: TextElement,
        dty: Float,
        xPos: Float = 0f,
        xPosPrev: Float = 0f
    ) {
        val tj = textElement.tj as PDFString

        when {
            currTextGroup.size() == 0 -> newLine(textElement, xPos)
            sameLine(dty, textElement.scaleY) -> {
                if (
                    (currLine.last().tj as PDFString).value.last() != ' ' // If last TextElement does not end with space
                    && (textElement.tj as PDFString).value.first() != ' ' // If new TextElement does not start with space
                    && areSpaceSeparated(textElement, xPos, xPosPrev)
                ) {
                    // Put space between last TextElement and new TextElement
                    sb.clear().append(' ').append(tj.value)
                    sb.insert(0, '(').append(')')
                    textElement.tj = sb.toPDFString()
                }
                currLine.add(textElement)
            }
            near(dty, textElement.scaleY) -> newLine(textElement, xPos)
            else -> newTextGroup(textElement, xPos)
        }
    }

    private fun areSpaceSeparated(
        textElement: TextElement,
        xPos: Float,
        xPosPrev: Float
    ): Boolean {
        val font = fonts[textElement.fontResource]
        if (font != null) {
            // Get width of space or 'i'
            //val spaceWidth = ((font.widths[32] ?: font.widths[105] ?: return true) / 1000) * fSize * textObj.scaleX
            val spaceWidth = ((font.widths.extractMinWidthIntoSpaceWidth()
                ?: return true) / 1000f) * textElement.scaleX

            val lastCharPosition = xPosPrev + currLine.last().width
            return (xPos - lastCharPosition) >= (spaceWidth / 2)
        } else {
            return true
        }
    }

    internal fun checkForListTypeTextGroups() {
        fun checkIfAllLinesEndWithPeriods(textGroup: TextGroup) {
            textGroup.isAList = true
            for (i in 0 until textGroup.size()) {
                // For each line, check if the last element ends with a period.
                val line = textGroup[i]
                val s = (line[line.size - 1].tj as PDFString).value
                if (!s.endsWith('.'))
                    textGroup.isAList = false
            }
        }
        for (i in 0 until contentGroups.size) {
            when (val it = contentGroups[i]) {
                is TextGroup -> checkIfAllLinesEndWithPeriods(it)
                is Table -> {
                    for (j in 0 until it.size()) {
                        for (k in 0 until it[j].size()) {
                            for (l in 0 until it[j][k].size()) {
                                val textGroup = it[j][k][l]
                                checkIfAllLinesEndWithPeriods(textGroup)
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun getLargestWidth(measureWidths: Boolean): Float {
        var maxWidth = 0f

        if (measureWidths) {
            contentGroups
                .asSequence()
                .filter { it is TextGroup }
                .forEach {
                    val g = it as TextGroup
                    for (i in 0 until g.size()) {
                        var width = 0f
                        val line = g[i]
                        for (j in 0 until line.size) {
                            width += line[j].width
                        }
                        if (width > maxWidth)
                            maxWidth = width
                    }
                }
        } else {
            maxWidth = getLargestLength().toFloat()
        }

        return maxWidth
    }

    private fun getLargestLength(): Int {
        var maxWidth = 0
        contentGroups
            .asSequence()
            .filter { it is TextGroup }
            .forEach {
                val g = it as TextGroup
                for (i in 0 until g.size()) {
                    var charCount = 0
                    val line = g[i]
                    for (j in 0 until line.size) {
                        charCount += (line[j].tj as PDFString).value.length
                    }
                    if (charCount > maxWidth)
                        maxWidth = charCount
                }
            }
        return maxWidth
    }

    internal fun formParagraphs(longestWidth: Float, measureWidths: Boolean) {
        contentGroups
            .asSequence()
            .filter { it is TextGroup && !it.isAList }
            .forEach {
                var i = 0
                val g = it as TextGroup

                /**
                 * A group of TextElements that is to be rendered as one line in PDF document. Its width will determine
                 * if the next line will be appended. It itself may have also been appended to the previous line as this
                 * and both the previous and next line may have formed a paragraph.
                 */
                var toMeasure = g[0]

                // Iterate until the second last of the list. The last line will be appended to it if necessary.
                while (i + 1 < g.size()) {
                    /**
                     * The current line that may also include any succeeding lines appended to it. It represents a single
                     * paragraph.
                     */
                    val line = g[i]

                    // If the next line is to be appended then 'i' will not be incremented, keeping the current line for
                    // other succeeding lines to appended to it. If not, 'i' will be incremented and the next line will
                    // form a new paragraph appending other lines to it.

                    var width = 0f
                    toMeasure.forEach { e ->
                        if (measureWidths) {
                            width += e.width
                        } else {
                            width += (e.tj as PDFString).value.length
                        }
                    }

                    // If almost equal to estimated page width, append next line to current line and the number of lines
                    // in TextGroup is reduced by 1. Else, evaluate the next line.
                    if (width >= (0.8 * (longestWidth))) {
                        val next = g[i + 1]

                        val lastElementStr = (line.last().tj as PDFString).value
                        if (lastElementStr.endsWith('-')) {
                            // Remove the hyphen before appending
                            sb.clear().append(lastElementStr, 0, lastElementStr.lastIndex)
                            sb.insert(0, '(')
                            sb.append(')')
                            line.last().tj = sb.toPDFString()
                        } else {
                            // Add space in between when appending.
                            sb.clear().append('(').append(' ').append((next.first().tj as PDFString).value).append(')')
                            next.first().tj = sb.toPDFString()
                        }

                        // Append next line to current line. The appended line will be removed from the TextGroup's list.
                        // The line following it in the list will be the next to append in case.
                        line.addAll(next)
                        g.remove(next)

                        // Do not increment i but the text that was just appended will be assigned to toMeasure variable
                        // which will be evaluated for the next iteration.
                        toMeasure = next
                    } else {
                        i++
                        toMeasure = g[i]
                    }
                }
            }
    }

    fun addIndents() {
        val indents = HashMap<Float, Int>()
        for (i in 0 until contentGroups.size) {
            val textGroup = contentGroups[i]
            if (textGroup is TextGroup) {
                for (j in 0 until textGroup.size()) {
                    val xNew = textGroup.getLineX(j)
                    val xes = indents.keys
                    if (xes.contains(xNew)) continue
                    var xNewIndent = 0
                    for (x in xes) {
                        val xIndent = indents[x] as Int
                        when {
                            xNew > x && xNewIndent <= xIndent -> xNewIndent = xIndent + 1
                            xNew == x && xNewIndent < xIndent -> xNewIndent = xIndent
                            xNew == x && xNewIndent > xIndent -> indents[x] = xNewIndent
                            xNew < x && xIndent <= xIndent -> indents[x] = xNewIndent + 1
                        }
                    }
                    indents[xNew] = xNewIndent
                }
            }
        }

        for (i in 0 until contentGroups.size) {
            val textGroup = contentGroups[i]
            if (textGroup is TextGroup) {
                for (j in 0 until textGroup.size()) {
                    val indent = indents[textGroup.getLineX(j)]
                    if (indent != null && indent > 0) {
                        sb.clear()
                        sb.append('(')
                        repeat(indent) {
                            sb.append(' ')
                        }

                        val line = textGroup[j]
                        val newTj = sb.append((line.first().tj as PDFString).value).append(')').toPDFString()
                        line.first().tj = newTj
                    }
                }
            }
        }
    }

    internal fun mergeElementsWithSameFontAndColor() {
        for (i in 0 until contentGroups.size) {
            when (val it = contentGroups[i]) {
                is TextGroup -> mergeElementsInTextGroup(it)
                is Table -> {
                    for (j in 0 until it.size()) {
                        val row = it[j]
                        for (k in 0 until row.size()) {
                            val cell = row[k]
                            for (l in 0 until cell.size()) {
                                val textGroup = cell[l]
                                mergeElementsInTextGroup(textGroup)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun mergeElementsInTextGroup(textGroup: TextGroup) {
        for (i in 0 until textGroup.size()) {
            val line = textGroup[i]
            var first = 0
            var j = 1
            sb.clear()
            while (j < line.size) {
                if (line[j].tf == line[j - 1].tf && line[j].rgb.contentEquals(line[j - 1].rgb)) {
                    if (sb.isEmpty()) {
                        first = j - 1
                        sb.append(
                            (line[j - 1].tj as PDFString).value
                        )
                        sb.append(
                            (line[j].tj as PDFString).value
                        )
                    } else {
                        sb.append(
                            (line[j].tj as PDFString).value
                        )
                    }
                } else {
                    if (sb.isNotEmpty()) {
                        mergeTextElements(line, first, j)
                        sb.clear()
                    }
                    first = j
                }
                j++
            }
            if (sb.isNotEmpty()) {
                mergeTextElements(line, first, line.size)
                sb.clear()
            }
        }
    }

    private fun mergeTextElements(line: ArrayList<TextElement>, start: Int, end: Int) {
        for (k in (end - 1) downTo (start + 1)) {
            line.removeAt(k)
        }
        sb.insert(0, '(')
        sb.append(')')
        line[start].tj = sb.toPDFString()
    }

    private fun deleteBlankLines() {
        var i = 0
        while (i < contentGroups.size) {
            val textGroup = contentGroups[i]
            if (textGroup is TextGroup) {
                // Iterate through each line.
                var j = 0
                while (j < textGroup.size()) {
                    val line = textGroup[j]

                    // Concatenate all TextElements in the line. If the resulting text is empty or entirely made of
                    // white spaces, remove the line.
                    sb.clear()
                    for (k in 0 until line.size) {
                        val e = line[k]
                        sb.append((e.tj as PDFString).value)
                    }
                    if (sb.isBlank())
                        textGroup.remove(line)
                    j++
                }

                // If TextGroup no longer has any lines, then remove the TextGroup itself.
                if (textGroup.size() == 0)
                    contentGroups.remove(textGroup)
            }
            // Ignore table, since a blank line may mean an empty cell.
            i++
        }
    }

    private fun SparseArrayCompat<Float>.extractMinWidthIntoSpaceWidth(): Float? {
        return if (size() > 0) {
            var min = Float.MAX_VALUE
            for (i in 0 until size()) {
                if (valueAt(i) < min && valueAt(i) != 0f) min = valueAt(i)
            }
            if (min == Float.MAX_VALUE) null else min
        } else
            null
    }
}