package marabillas.loremar.pdfparser.objects

import junit.framework.Assert
import org.junit.Test

class ArrayTest {
    @Test
    fun testParse() {
        val s = "[54 (Hello World) /Name]"
        val array = Array(s).parse()
        Assert.assertEquals((array[0] as Numeric).value.toInt(), 54)
        Assert.assertEquals(array[1], "Hello World")
        Assert.assertEquals(array[2], "Name")
    }
}