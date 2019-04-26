package marabillas.loremar.pdfparser.objects

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ReferenceTest {
    @Test
    fun testReference() {
        val s = "12 0 R"
        val r = s.toReference()
        assertThat(r.obj, `is`(12))
        assertThat(r.gen, `is`(0))
    }
}