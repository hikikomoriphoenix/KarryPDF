package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.objects.Dictionary
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.RandomAccessFile

class DictionaryTest {
    @Test
    fun testDictionaryFromFile() {
        var path = javaClass.classLoader.getResource("DictionaryTestFile").path
        var file = RandomAccessFile(path, "r")
        var dictionary = Dictionary(file, 15).parse()
        assertThat(dictionary["text"], `is`("(hello world)"))
        assertThat(dictionary["nameKey"], `is`("/nameValue"))
        assertThat(
            dictionary["longText"], `is`(
                "(some loooooooooooooooooooooooooonnnnnnn" +
                        "ggggggg teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext)"
            )
        )
        assertThat(dictionary["longNumber"], `is`("123456789"))
        assertThat(dictionary["someKey"], `is`("(value is in next line)"))
        println("Testing DictionaryTestFile success")

        path = javaClass.classLoader.getResource("DictionaryTestFile1").path
        file = RandomAccessFile(path, "r")
        dictionary = Dictionary(file, 0).parse()
        assertThat(dictionary["name"], `is`("value"))
        println("Testing DictionaryTestFile1 success")

        path = javaClass.classLoader.getResource("DictionaryTestFile2").path
        file = RandomAccessFile(path, "r")
        dictionary = Dictionary(file, 0).parse()
        assertThat(dictionary["name"], `is`("value"))
        println("Testing DictionaryTestFile2 success")

        path = javaClass.classLoader.getResource("DictionaryTestFile3").path
        file = RandomAccessFile(path, "r")
        dictionary = Dictionary(file, 0).parse()
        assertThat(dictionary["test1"], `is`("[[yes]hey]"))
        assertThat(dictionary["test2"], `is`("((hello))"))
        assertThat(dictionary["test3"], `is`("<no<no>>"))
        assertThat(dictionary["test4"], `is`("/wow"))
        assertThat(dictionary["test5"], `is`("<<<<what>>who>>"))
        println("Testing DictionaryTestFile3 success")
    }

    @Test
    fun testDictionaryFromString() {
        var s = "<</test1 (Hello)/test2 (World)>>"
        var dictionary = Dictionary(s).parse()
        assertThat(dictionary["test1"], `is`("(Hello)"))
        assertThat(dictionary["test2"], `is`("(World)"))
        println("Testing dictionary from string success")

        s = "<</Reference 12 0 R >>"
        dictionary = Dictionary(s).parse()
        assertThat(dictionary["Reference"], `is`("12 0 R"))
        println("Testing indirect object reference entry success")
    }
}