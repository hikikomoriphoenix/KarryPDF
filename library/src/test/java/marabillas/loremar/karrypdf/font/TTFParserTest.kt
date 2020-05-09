package marabillas.loremar.karrypdf.font

import marabillas.loremar.karrypdf.font.ttf.TTFParser
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class TTFParserTest {
    @Test
    fun testGetCharacterWidths() {
        val file1 = File(javaClass.classLoader.getResource("fonts/FontAwesome.ttf").path)
        val file2 = File(javaClass.classLoader.getResource("fonts/DroidSans.ttf").path)
        val file3 = File(javaClass.classLoader.getResource("fonts/tahoma.ttf").path)

        println("Testing FontAwesome.ttf")
        var input = FileInputStream(file1)
        var data = input.readBytes()
        //println("Data:\n${String(data)}")
        input.close()
        var ttf = TTFParser(data)
        ttf.tables.forEach {
            println()
            println(it.key)
            println("checksum=${it.value.checksum}")
            println("offset=${it.value.offset}")
            println("length=${it.value.length}")
            assertTrue(it.value.offset < data.size)
        }
        println()
        var glyphWidths = ttf.getCharacterWidths()
        println("glyphWidthsCount = ${glyphWidths.size()}")
        for (i in 0 until glyphWidths.size()) {
            println("character=${glyphWidths.keyAt(i)} width=${glyphWidths.valueAt(i)}")
        }
        var expected = 750f
        assertThat(glyphWidths[-1], `is`(expected))
        println("Missing character width is verified to be $expected")

        println("\nTesting DroidSans.ttf")
        input = FileInputStream(file2)
        data = input.readBytes()
        input.close()
        ttf = TTFParser(data)
        glyphWidths = ttf.getCharacterWidths()
        expected = 841f
        assertThat(glyphWidths[-1], `is`(expected))
        println("Missing character width is verified to be $expected")

        println("\nTesting tahoma.ttf")
        input = FileInputStream(file3)
        data = input.readBytes()
        input.close()
        ttf = TTFParser(data)
        glyphWidths = ttf.getCharacterWidths()
        expected = 1536f
        assertThat(glyphWidths[-1], `is`(expected))
        println("Missing character width is verified to be $expected")
    }
}