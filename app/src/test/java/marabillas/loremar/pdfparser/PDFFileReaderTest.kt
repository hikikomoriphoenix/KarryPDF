package marabillas.loremar.pdfparser

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.RandomAccessFile

class PDFFileReaderTest {
    @Test
    fun testGetLastXRefData() {
        val path = javaClass.classLoader.getResource("sample.pdf").path
        val file = RandomAccessFile(path, "r")
        val reader = PDFFileReader(file)
        val xref = reader.getLastXRefData()

        val obj0 = xref["0 65535"]
        assertThat(obj0?.obj, `is`(0))
        assertThat(obj0?.pos, `is`(0L))
        assertThat(obj0?.gen, `is`(65535))
        assertThat(obj0?.inUse, `is`(false))
        val obj10 = xref["10 0"]
        assertThat(obj10?.obj, `is`(10))
        assertThat(obj10?.pos, `is`(2574L))
        assertThat(obj10?.gen, `is`(0))
        assertThat(obj10?.inUse, `is`(true))
    }
}