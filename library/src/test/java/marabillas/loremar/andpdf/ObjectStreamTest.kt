package marabillas.loremar.andpdf

import marabillas.loremar.andpdf.objects.Dictionary
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.RandomAccessFile

class ObjectStreamTest {
    @Test
    fun testObjectStream() {
        val path = javaClass.classLoader.getResource("samplepdf1.4compressed.pdf").path
        val file = RandomAccessFile(path, "r")
        val objStm = ObjectStream(file, 95516)
        val obj = objStm.extractObjectBytes(0) as Dictionary
        assertThat(obj["Type"].toString(), `is`("Page"))
    }
}