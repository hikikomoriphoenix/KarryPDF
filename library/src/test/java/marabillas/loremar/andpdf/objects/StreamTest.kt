package marabillas.loremar.andpdf.objects

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class StreamTest {
    @Test
    fun testDecodeEncodedStream() {
        val path1 = javaClass.classLoader.getResource("samplepdf1.4.pdf").path
        val file1 = RandomAccessFile(path1, "r")
        val streamObj = Stream(file1, 42603)
        val stream = streamObj.decodeEncodedStream()
        val path2 = javaClass.classLoader.getResource("samplepdf1.4expectedtext.txt").path
        val file2 = File(path2)
        val expected = file2.readText()
        assertThat(String(stream), `is`(expected))
    }
}