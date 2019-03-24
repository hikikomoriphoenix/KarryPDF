package marabillas.loremar.pdfparser.contents

internal class TextContentAnalyzer(textObjects: ArrayList<TextObject>) {
    fun analyze() {
        // TODO If tj values are arrays resulting from TJ operator, determine from the number values between strings
        // whether to add space or not while concatenating strings. First to get glyph width for space, get all the
        // negative numbers and identify the negative number with most occurrences. Rule: If the absolute value of a
        // negative number is less than 15% of the space width, don't add space. If it is greater than 115%,
        // then add double space. Otherwise, add space. If number is positive don't add space.

        // TODO Check for multi-column texts. Get all text objects with equal Tx origin and tag each as "columned".
        // If there is more than one pair of text objects which are tagged as "columned" and have equal Ty origins, then
        // these text objects form a table. Text in adjacent columns of the same row are concatenated after each other
        // and are separated by " || ". The next row begins on the next paragraph. If "columned" text objects don't form
        // a table, then they are arranged according to their Tx origins. All "columned" text objects with lesser Tx are
        // displayed first before those with greater Tx.

        // TODO Group texts in the same line or in adjacent lines with line-spacing less than font size.

        // TODO Concatenate text contents in the same line.

        // Concatenated text contents should be merged into one.

        // TODO Check if lines end with a period. If yes, then lines stay as they were. If not, then proceed analysis.

        // TODO If a line ends with '-', then append the next line to this line and remove the '-' character.

        // TODO If any line ends with a period or contains the following pattern: "\. [\p{Lu}\p{Lt]", find the
        // previous period or a capital letter positioned on the beginning of the line, and concatenate all the lines in
        // between.

        // Check the y values of each content and rearrange accordingly.
    }
}