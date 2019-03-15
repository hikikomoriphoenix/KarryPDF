package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.objects.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.RandomAccessFile

class PDFParserTest {
    @Test
    fun testLoadDocument() {
        val path = javaClass.classLoader.getResource("samplepdf1.4.pdf").path
        val file = RandomAccessFile(path, "r")
        val parser = PDFParser().loadDocument(file)
        assertThat(parser.size, `is`(20))
        val docCat = parser.documentCatalog
        val pages = docCat?.get("Pages") as Dictionary
        val count = pages["Count"] as Numeric
        assertThat(count.value.toInt(), `is`(2))
        val info = parser.info
        val creator = info?.get("Creator") as PDFString
        assertThat(
            creator.value,
            `is`("Mozilla/5.0 \\(X11; Linux x86_64\\) AppleWebKit/537.36 \\(KHTML, like Gecko\\) Chrome/72.0.3626.121 Safari/537.36")
        )
    }

    @Test
    fun testResolveReference() {
        val path = javaClass.classLoader.getResource("samplepdf1.4.pdf").path
        val file = RandomAccessFile(path, "r")
        PDFParser().loadDocument(file) // This also sets ReferenceResolver for ObjectIdentifier class
        var obj = "3 0 R".toPDFObject() // resolveReference() is indirectly called
        assertTrue(obj is Dictionary)
        obj = "19 0 R".toPDFObject() // resolveReference() is indirectly called
        assertTrue(obj is Reference)
    }
}