package marabillas.loremar.pdfparser.objects

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ArrayTest {
    @Test
    fun testParse() {
        val s = "[54 (Hello World) /Name]"
        val array = Array(s).parse()
        assertThat(array[0], `is`("54"))
        assertThat(array[1], `is`("(Hello World)"))
        assertThat(array[2], `is`("/Name"))
    }
}