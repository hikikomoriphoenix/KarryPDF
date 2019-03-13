package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.objects.Numeric
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun testGetLastXRefDataFromCompressedPDF() {
        val path = javaClass.classLoader.getResource("samplepdf1.4compressed.pdf").path
        val file = RandomAccessFile(path, "r")
        val reader = PDFFileReader(file)
        val xref = reader.getLastXRefData()
        xref.forEach {
            if (it.value.compressed)
                println("Compressed-> obj:${it.value.obj} objStm:${it.value.objStm} index:${it.value.index}")
        }
        xref.forEach() {
            if (!it.value.compressed && it.value.inUse && !it.value.nullObj)
                println("Uncompressed->obj:${it.value.obj} pos:${it.value.pos} gen:${it.value.gen}")
        }
    }

    @Test
    fun testGetTrailerPosition() {
        var path = javaClass.classLoader.getResource("samplepdf1.4compressed.pdf").path
        var file = RandomAccessFile(path, "r")
        var reader = PDFFileReader(file)
        var trailerPos = reader.getTrailerPosition()
        assertNull(trailerPos)
        println("Testing samplepdf1.4compressed.pdf not having trailer -> success.")

        path = javaClass.classLoader.getResource("samplepdf1.4.pdf").path
        file = RandomAccessFile(path, "r")
        reader = PDFFileReader(file)
        trailerPos = reader.getTrailerPosition()
        if (trailerPos != null) {
            file.seek(trailerPos)
            val s = file.readLine()
            assertThat(s, startsWith("trailer"))
        } else {
            Assert.fail("Could not find the trailer.")
        }
        println("Testing samplepdf1.4.pdf having trailer -> succcess.")
    }

    @Test
    fun testGetTrailerEntries() {
        var path = javaClass.classLoader.getResource("samplepdf1.4.pdf").path
        var file = RandomAccessFile(path, "r")
        var reader = PDFFileReader(file)
        var entries = reader.getTrailerEntries()
        assertTrue(entries["Size"] is Numeric)
        println("Size entry in trailer for samplepdf1.4.pdf is ${(entries["Size"] as Numeric).value.toInt()}")

        path = javaClass.classLoader.getResource("samplepdf1.4compressed.pdf").path
        file = RandomAccessFile(path, "r")
        reader = PDFFileReader(file)
        entries = reader.getTrailerEntries()
        assertTrue(entries["Size"] is Numeric)
        println("Size entry in trailer for samplepdf1.4compressed.pdf is ${(entries["Size"] as Numeric).value.toInt()}")
    }
}