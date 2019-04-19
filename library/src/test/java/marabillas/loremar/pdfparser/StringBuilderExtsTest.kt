package marabillas.loremar.pdfparser

import junit.framework.Assert.assertTrue
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
}