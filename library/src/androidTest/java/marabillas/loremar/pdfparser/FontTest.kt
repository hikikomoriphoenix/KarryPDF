package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.objects.Dictionary
import org.junit.Test
import java.io.RandomAccessFile

class FontTest {
    @Test
    fun testGetPageFonts() {
        val path = javaClass.classLoader.getResource("seeing-theory.pdf").path
        val file = RandomAccessFile(path, "r")
        val parser = PDFParser().loadDocument(file)
        val pageDic = parser.pages[48].resolve() as Dictionary
        pageDic.resolveReferences()
        val resources = pageDic["Resources"] as Dictionary
        resources.resolveReferences()
        val fontsDic = resources["Font"] as Dictionary?
        fontsDic?.resolveReferences()
        val fonts = parser.getPageFonts(fontsDic as Dictionary)
        fonts.forEach {
            println("resource=${it.key} font=${it.value}")
        }
    }
}