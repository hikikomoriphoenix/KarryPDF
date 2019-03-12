package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.objects.Stream
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class FlateTest {
    @Test
    fun testDecode() {
        val path1 = javaClass.classLoader.getResource("samplepdf1.4.pdf").path
        val file1 = RandomAccessFile(path1, "r")
        val streamObj = Stream(file1, 42603)
        val path2 = javaClass.classLoader.getResource("samplepdf1.4expectedtext.txt").path
        val file2 = File(path2)
        val expected = file2.readText()
        val stream = Flate().decode(streamObj.streamData)
        assertThat(String(stream), `is`(expected))
    }
}