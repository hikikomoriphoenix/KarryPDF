package marabillas.loremar.pdfparser

import junit.framework.Assert.assertTrue
import marabillas.loremar.pdfparser.utils.exts.hexFromInt
import marabillas.loremar.pdfparser.utils.exts.hexToInt
import marabillas.loremar.pdfparser.utils.exts.isEnclosedWith
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class StringBuilderExtsTest {
    @Test
    fun testIsEnlcosedWith() {
        val sb = StringBuilder("(Hello World)")
        assertTrue(sb.isEnclosedWith(arrayOf('('), arrayOf(')')))

        sb.clear().append("<hello World>")
        assertTrue(sb.isEnclosedWith(arrayOf('<'), arrayOf('>')))

        sb.clear().append("<<Hello World>>")
        assertTrue(sb.isEnclosedWith(arrayOf('<', '<'), arrayOf('>', '>')))

        sb.clear().append("[Hello World]")
        assertTrue(sb.isEnclosedWith(arrayOf('['), arrayOf(']')))
    }

    @Test
    fun testHexToInt() {
        val i = StringBuilder("ffff").hexToInt()
        assertThat(i, `is`(65535))
    }

    @Test
    fun testHexFromInt() {
        val sb = StringBuilder().hexFromInt(65535)
        assertThat(sb.toString(), `is`("FFFF"))
    }
}