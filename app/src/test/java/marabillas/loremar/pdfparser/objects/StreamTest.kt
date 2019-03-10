package marabillas.loremar.pdfparser.objects

import org.junit.Test
import java.io.RandomAccessFile

class StreamTest {
    @Test
    fun testDecodeEncodedStream() {
        val path = javaClass.classLoader.getResource("samplepdf1.7.pdf").path
        val file = RandomAccessFile(path, "r")
        val streamObj = Stream(file, 9)
        println("Length of encoded stream: ${streamObj.streamData.size}")
        val stream = streamObj.decodeEncodedStream()
        println("Length of decoded stream: ${stream.size}")
        println(String(stream, Charsets.US_ASCII))
    }
}