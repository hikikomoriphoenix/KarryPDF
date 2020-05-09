package marabillas.loremar.karrypdf

import marabillas.loremar.karrypdf.contents.text.TextContent
import marabillas.loremar.karrypdf.utils.forceHideLogs
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MultiThreadedGetPageContentsTest {
    private val samplesDir = "SamplePDFs/"
    private val pdfFilename = "seeing-theory.pdf"

    @Test
    fun test() {
        forceHideLogs = true
        println("Testing getPageContents when used multiple times simultaneously in different threads.")
        val path = javaClass.classLoader.getResource("$samplesDir$pdfFilename").path
        val file = RandomAccessFile(path, "r")
        val pdf = KarryPDF(file)

        var thread1Success = false
        var thread2Success = false
        var thread3Success = false

        val thread1 = Thread {
            val sb = StringBuilder()
            pdf.getPageContents(20).forEach {
                if (it is TextContent) {
                    sb.append(it.content)
                }
            }
            if (sb.isNotBlank()) {
                thread1Success = true
                println("Successfully extracted contents from page 20.")
                //println("p20 -> $sb")
            } else
                Assert.fail("Failed to extract contents from page 20.")
        }.apply { start() }
        val thread2 = Thread {
            val sb = StringBuilder()
            pdf.getPageContents(30).forEach {
                if (it is TextContent) {
                    sb.append(it.content)
                }
            }
            if (sb.isNotBlank()) {
                thread2Success = true
                println("Successfully extracted contents from page 30.")
                //println("p30 -> $sb")
            } else
                Assert.fail("Failed to extract contents from page 30.")
        }.apply { start() }
        val thread3 = Thread {
            val sb = StringBuilder()
            pdf.getPageContents(40).forEach {
                if (it is TextContent) {
                    sb.append(it.content)
                }
            }
            if (sb.isNotBlank()) {
                thread3Success = true
                println("Successfully extracted contents from page 40.")
                //println("p40 -> $sb")
            } else
                Assert.fail("Failed to extract contents from page 40.")
        }.apply { start() }

        thread1.join(5000)
        thread2.join(5000)
        thread3.join(5000)

        println("End of test.")

        if (!thread1Success || !thread2Success || !thread3Success)
            Assert.fail("At least one of the threads failed.")

        forceHideLogs = false
    }
}