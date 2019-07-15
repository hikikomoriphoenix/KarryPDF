package marabillas.loremar.andpdf

import marabillas.loremar.andpdf.document.XRefStream
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.RandomAccessFile

class XRefStreamTest {
    @Test
    fun testXRefStream() {
        val path = javaClass.classLoader.getResource("samplepdf1.4compressed.pdf").path
        val file = RandomAccessFile(path, "r")
        println("File Length = ${file.length()}")
        val streamObj = XRefStream(file, 96733)
        streamObj.decodeEncodedStream()
        val entries = streamObj.parse()
        entries.forEach {
            println(it.key)
        }

        assertThat(entries["1 0"]?.objStm, `is`(12))
        assertThat(entries["7 0"]?.pos, `is`(53768L))
        assertThat(entries["16 0"]?.pos, `is`(25555L))
        assertThat(entries["23 0"]?.pos, `is`(96733L))
    }
}