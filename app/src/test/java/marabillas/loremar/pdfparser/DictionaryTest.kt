package marabillas.loremar.pdfparser

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.RandomAccessFile

class DictionaryTest {
    @Test
    fun testInitial() {
        var path = javaClass.classLoader.getResource("DictionaryTestFile").path
        var file = RandomAccessFile(path, "r")
        var dictionary = Dictionary(file, 15)
        assertThat(dictionary.entries["text"], `is`("(hello world)"))
        assertThat(dictionary.entries["nameKey"], `is`("/nameValue"))
        assertThat(
            dictionary.entries["longText"], `is`(
                "(some loooooooooooooooooooooooooonnnnnnn" +
                        "ggggggg teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext)"
            )
        )
        assertThat(dictionary.entries["longNumber"], `is`("123456789"))
        assertThat(dictionary.entries["someKey"], `is`("(value is in next line)"))

        path = javaClass.classLoader.getResource("DictionaryTestFile1").path
        file = RandomAccessFile(path, "r")
        dictionary = Dictionary(file, 0)
        assertThat(dictionary.entries["name"], `is`("value"))

        path = javaClass.classLoader.getResource("DictionaryTestFile2").path
        file = RandomAccessFile(path, "r")
        dictionary = Dictionary(file, 0)
        assertThat(dictionary.entries["name"], `is`("value"))
    }
}