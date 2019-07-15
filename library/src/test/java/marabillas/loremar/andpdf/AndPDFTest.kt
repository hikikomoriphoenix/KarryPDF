package marabillas.loremar.andpdf

import marabillas.loremar.andpdf.objects.Dictionary
import marabillas.loremar.andpdf.objects.Numeric
import marabillas.loremar.andpdf.objects.PDFString
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.RandomAccessFile

class AndPDFTest {
    @Test
    fun testLoadDocument() {
        val path = javaClass.classLoader.getResource("samplepdf1.4.pdf").path
        val file = RandomAccessFile(path, "r")
        val parser = AndPDF(file)
        assertThat(parser.size, `is`(20))
        val docCat = parser.documentCatalog
        val pages = docCat?.resolveReferences()?.get("Pages") as Dictionary
        val count = pages.resolveReferences()["Count"] as Numeric
        assertThat(count.value.toInt(), `is`(2))
        val info = parser.info
        val creator = info?.resolveReferences()?.get("Creator") as PDFString
        assertThat(
            creator.value,
            `is`("Mozilla/5.0 \\(X11; Linux x86_64\\) AppleWebKit/537.36 \\(KHTML, like Gecko\\) Chrome/72.0.3626.121 Safari/537.36")
        )
    }

    /*   @Test
       fun testResolveReference() {
           var path = javaClass.classLoader.getResource("samplepdf1.4.pdf").path
           var file = RandomAccessFile(path, "r")
           AndPDF(file) // This also sets ReferenceResolver for ObjectIdentifier class
           var obj = "3 0 R".toPDFObject(true) // resolveReference() is indirectly called
           assertTrue(obj is Dictionary)
           obj = "19 0 R".toPDFObject(true) // resolveReference() is indirectly called
           assertTrue(obj is Reference)

           path = javaClass.classLoader.getResource("samplepdf1.4compressed.pdf").path
           file = RandomAccessFile(path, "r")
           AndPDF(file)
           obj = "10 0 R".toPDFObject(true)
           assertTrue(obj is Reference)
           obj = "1 0 R".toPDFObject(true)
           assertTrue(obj is Dictionary)
       }*/
}