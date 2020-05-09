package marabillas.loremar.karrypdf.objects

class PDFArrayTest {
    /*@Test
    fun testParse() {
        val s = "[54 (Hello World) /Name]"
        val array = s.toPDFArray()
        assertThat((array[0] as Numeric).value.toInt(), `is`(54))
        assertThat((array[1] as PDFString).value, `is`("Hello World"))
        assertThat((array[2] as Name).value, `is`("Name"))
    }

    @Test
    fun testReferenceEntry() {
        PDFObjectAdapter.referenceResolver = object : ReferenceResolver {
            override fun resolveReferenceToStream(reference: Reference): Stream? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun resolveReference(reference: Reference): PDFObject? {
                return reference
            }
        }
        val s = "[(Hello World)12 0 R/Nameless]"
        val array = s.toPDFArray()
        assertThat((array[1] as Reference).obj, `is`(12))
        assertThat((array[1] as Reference).gen, `is`(0))
    }*/
}