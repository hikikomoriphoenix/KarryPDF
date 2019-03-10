package marabillas.loremar.pdfparser

import org.junit.Test
import java.io.RandomAccessFile

class XRefStreamTest {
    @Test
    fun testXRefStream() {
        val path = javaClass.classLoader.getResource("testPDF_Version.7.x.pdf").path
        val file = RandomAccessFile(path, "r")
        println("File Length = ${file.length()}")
        val streamObj = XRefStream(file, 116)
        streamObj.decodeEncodedStream()
        val entries = streamObj.parse()
        entries.forEach {
            println(it.key)
        }
    }
}