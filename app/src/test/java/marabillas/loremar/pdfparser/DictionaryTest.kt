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
        println("Testing DictionaryTestFile success")

        path = javaClass.classLoader.getResource("DictionaryTestFile1").path
        file = RandomAccessFile(path, "r")
        dictionary = Dictionary(file, 0)
        assertThat(dictionary.entries["name"], `is`("value"))
        println("Testing DictionaryTestFile1 success")

        path = javaClass.classLoader.getResource("DictionaryTestFile2").path
        file = RandomAccessFile(path, "r")
        dictionary = Dictionary(file, 0)
        assertThat(dictionary.entries["name"], `is`("value"))
        println("Testing DictionaryTestFile2 success")

        path = javaClass.classLoader.getResource("DictionaryTestFile3").path
        file = RandomAccessFile(path, "r")
        dictionary = Dictionary(file, 0)
        assertThat(dictionary.entries["test1"], `is`("[[yes]hey]"))
        assertThat(dictionary.entries["test2"], `is`("((hello))"))
        assertThat(dictionary.entries["test3"], `is`("<no<no>>"))
        assertThat(dictionary.entries["test4"], `is`("/wow"))
        assertThat(dictionary.entries["test5"], `is`("<<<<what>>who>>"))
        println("Testing DictionaryTestFile3 success")
    }
}