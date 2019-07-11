package marabillas.loremar.andpdf

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.RandomAccessFile

/**
 * This test will try to run getPageContents function on every page of every
 * sample PDFs stored in /library/src/test/resources/SamplePDFs/. This might take long.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AllSamplesQuickTest {
    private val samplesDir = "SamplePDFs/"
    private var samples = mutableListOf<String>()

    @Test
    fun testAll() {
        getAllSamples()
        samples.forEach { pdfFilename ->
            test(pdfFilename)
        }
    }

    private fun getAllSamples() {
        val directoryReader = javaClass.classLoader.getResourceAsStream(samplesDir).bufferedReader()
        while (true) {
            val line = directoryReader.readLine()
            if (line != null) {
                samples.add(line)
            } else {
                break
            }
        }
    }

    private fun test(pdfFilename: String) {
        print("Testing library on $pdfFilename...")
        try {
            val path = javaClass.classLoader.getResource("$samplesDir$pdfFilename").path
            val file = RandomAccessFile(path, "r")
            val pdf = AndPDF(file)
            val numPages = pdf.getTotalPages()
            repeat(numPages) { pageNum ->
                pdf.getPageContents(pageNum)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Library failed on last exception")
        }
        println("Success")
    }
}