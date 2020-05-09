package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.document.KarryPDFContext
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ReferenceTest {
    @Test
    fun testReference() {
        val s = "12 0 R"
        val r = s.toReference(KarryPDFContext())
        assertThat(r.obj, `is`(12))
        assertThat(r.gen, `is`(0))
    }
}