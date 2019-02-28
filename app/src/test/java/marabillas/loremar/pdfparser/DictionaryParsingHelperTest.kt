package marabillas.loremar.pdfparser

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class DictionaryParsingHelperTest {
    @Test
    fun testFindIndexOfClosingParentheses() {
        var s = "(123(345(67)89)0) 123243254435"
        var i = DictionaryParsingHelper().findIndexOfClosingDelimiter(s)
        assertThat(i, `is`(16))

        // Test for unbalanced parentheses
        s = "((("
        i = DictionaryParsingHelper().findIndexOfClosingDelimiter(s)
        assertThat(i, `is`(0))

        // Test for escaped parentheses
        s = "(\\()"
        i = DictionaryParsingHelper().findIndexOfClosingDelimiter(s)
        assertThat(i, `is`(3))
    }
}