package marabillas.loremar.andpdf.objects

import marabillas.loremar.andpdf.document.AndPDFContext
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ReferenceTest {
    @Test
    fun testReference() {
        val s = "12 0 R"
        val r = s.toReference(AndPDFContext())
        assertThat(r.obj, `is`(12))
        assertThat(r.gen, `is`(0))
    }
}