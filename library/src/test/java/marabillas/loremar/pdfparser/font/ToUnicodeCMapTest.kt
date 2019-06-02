package marabillas.loremar.pdfparser.font

import marabillas.loremar.pdfparser.font.cmap.ToUnicodeCMap
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ToUnicodeCMapTest {
    @Test
    fun testDecodeString() {
        val path = javaClass.classLoader.getResource("ToUnicodeTestFile")
        val file = File(path.toURI())
        val cmapSrc = file.readText()
        val map = ToUnicodeCMap(cmapSrc).parse()
        val encoded = "<011F2023EFB3EFB4EFB5FBB3FBD3>"
        val decoded = map.decodeString(encoded)
        val expected = "( 숢꽖뀂ꪪꪻᄢ㍄啦퀀퀠)"
        assertEquals(expected, decoded)
        println("$encoded is decoded to $expected")
    }
}