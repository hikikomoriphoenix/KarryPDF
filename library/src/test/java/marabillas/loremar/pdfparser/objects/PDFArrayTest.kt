package marabillas.loremar.pdfparser.objects

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class PDFArrayTest {
    @Test
    fun testParse() {
        val s = "[54 (Hello World) /Name]"
        val array = s.toPDFArray()
        assertThat((array[0] as Numeric).value.toInt(), `is`(54))
        assertThat((array[1] as PDFString).value, `is`("Hello World"))
        assertThat((array[2] as Name).value, `is`("Name"))
    }

    @Test
    fun testReferenceEntry() {
        ObjectIdentifier.referenceResolver = object : ReferenceResolver {
            override fun resolveReference(reference: Reference): PDFObject? {
                return reference
            }
        }
        val s = "[(Hello World)12 0 R/Nameless]"
        val array = s.toPDFArray()
        assertThat((array[1] as Reference).obj, `is`(12))
        assertThat((array[1] as Reference).gen, `is`(0))
    }
}