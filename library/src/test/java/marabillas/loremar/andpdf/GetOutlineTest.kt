package marabillas.loremar.andpdf

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GetOutlineTest {
    private val samplesDir = "SamplePDFs/"
    private val pdfFilename = "PDF32000_2008.pdf"

    @Test
    fun test() {
        val path = javaClass.classLoader.getResource("$samplesDir$pdfFilename").path
        val file = RandomAccessFile(path, "r")
        val pdf = AndPDF(file)

        println("Contents:")
        pdf.getDocumentOutline().forEach {
            println(it.title)
            it.subItems.forEach { sub ->
                println("-${sub.title}")
            }
        }
    }
}