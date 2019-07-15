package marabillas.loremar.andpdf

import marabillas.loremar.andpdf.utils.filterErrorLogs
import marabillas.loremar.andpdf.utils.forceHideLogs
import marabillas.loremar.andpdf.utils.loge
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
    private var numFails = 0
    private var samples = mutableListOf<String>()

    @Test
    fun testAll() {
        forceHideLogs = true
        getAllSamples()
        samples.forEach { pdfFilename ->
            test(pdfFilename)
        }
        if (numFails > 0) Assert.fail("Library failed in $numFails/${samples.size} samples")
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
        val path = javaClass.classLoader.getResource("$samplesDir$pdfFilename").path
        val file = RandomAccessFile(path, "r")
        val pdf = loadDocument(file, pdfFilename)
        if (pdf != null) {
            val numPages = pdf.getTotalPages()
            var numExceptions = 0
            repeat(numPages) { pageNum ->
                val success = executeGetPageContents(pdf, pageNum)
                if (!success) numExceptions++
            }
            if (numExceptions == 0)
                println("Success")
            else {
                println("Fail $numExceptions exceptions")
                numFails++
            }
        } else {
            println("Fail")
            numFails++
        }
    }

    private fun loadDocument(file: RandomAccessFile, filename: String): AndPDF? {
        return try {
            AndPDF(file)
        } catch (e: Exception) {
            System.err.println("Exception on loading $filename: ${e.message}")
            null
        }
    }

    private fun executeGetPageContents(pdf: AndPDF, pageNum: Int): Boolean {
        return try {
            pdf.getPageContents(pageNum)
            true
        } catch (e: Exception) {
            loge("UNCAUGHT EXCEPTION on page $pageNum", e)
            false
        }
    }

    @Test
    fun testOneSample() {
        forceHideLogs = true
        val pdfFilename = "ColorfulText.pdf"
        print("Testing library on $pdfFilename...")
        val path = javaClass.classLoader.getResource("$samplesDir$pdfFilename").path
        val file = RandomAccessFile(path, "r")
        val pdf = loadDocument(file, pdfFilename)
        if (pdf != null) {
            val numPages = pdf.getTotalPages()
            var numExceptions = 0
            repeat(numPages) { pageNum ->
                val success = executeGetPageContents(pdf, pageNum)
                if (success) {
                    println("Test page $pageNum...success")
                } else {
                    System.err.println("Testpage $pageNum...fail")
                    numExceptions++
                }
            }
            if (numExceptions > 0) {
                Assert.fail("$numExceptions Exceptions!")
            }
        } else {
            Assert.fail("Document does not exist")
        }
    }

    @Test
    fun testOnePage() {
        forceHideLogs = false
        filterErrorLogs = true

        val pdfFilename = "ColorfulText.pdf"
        val pageNum = 0
        print("Testing library on $pdfFilename at page $pageNum")
        val path = javaClass.classLoader.getResource("$samplesDir$pdfFilename").path
        val file = RandomAccessFile(path, "r")
        val pdf = loadDocument(file, pdfFilename)

        if (pdf != null) {
            val success = executeGetPageContents(pdf, pageNum)
            if (!success) {
                Assert.fail("This page threw an uncaught exception")
            }
        } else {
            Assert.fail("Document does not exist")
        }
    }
}