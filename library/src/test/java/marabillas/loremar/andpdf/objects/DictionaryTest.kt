package marabillas.loremar.andpdf.objects

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class DictionaryTest {
    /*    @Test
        fun testDictionaryFromFile() {
            var path = javaClass.classLoader.getResource("DictionaryTestFile").path
            var file = RandomAccessFile(path, "r")
            var dictionary = PDFFileReader(file).getDictionary(15)
            assertThat((dictionary["text"] as PDFString).value, `is`("hello world"))
            assertThat((dictionary["nameKey"] as Name).value, `is`("nameValue"))
            assertThat(
                (dictionary["longText"] as PDFString).value, `is`(
                    "some loooooooooooooooooooooooooonnnnnnnggggggg teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeext"
                )
            )
            assertThat((dictionary["someKey"] as PDFString).value, `is`("value is in next line"))
            println("Testing DictionaryTestFile success")

            path = javaClass.classLoader.getResource("DictionaryTestFile1").path
            file = RandomAccessFile(path, "r")
            dictionary = PDFFileReader(file).getDictionary(0)
            assertThat((dictionary["name"] as Name).value, `is`("value"))
            println("Testing DictionaryTestFile1 success")

            path = javaClass.classLoader.getResource("DictionaryTestFile2").path
            file = RandomAccessFile(path, "r")
            dictionary = PDFFileReader(file).getDictionary(0)
            assertThat((dictionary["name"] as Name).value, `is`("value"))
            println("Testing DictionaryTestFile2 success")

            path = javaClass.classLoader.getResource("DictionaryTestFile3").path
            file = RandomAccessFile(path, "r")
            dictionary = PDFFileReader(file).getDictionary(0)
            assertTrue(dictionary["test1"] is PDFArray)
            assertTrue(dictionary["test2"] is PDFString)
            assertTrue(dictionary["test3"] is PDFString)
            assertTrue(dictionary["test4"] is Name)
            assertTrue(dictionary["test5"] is Dictionary)
            println("Testing DictionaryTestFile3 success")

            path = javaClass.classLoader.getResource("DictionaryTestFile4").path
            file = RandomAccessFile(path, "r")
            dictionary = PDFFileReader(file).getDictionary(0)
            assertThat((dictionary["Name2"] as PDFString).value, `is`("Value2"))
        }*/
    @Test
    fun testDictionaryFromString() {
        var s = "<</test1 (Hello)/test2 (World)>>"
        var dictionary = s.toDictionary()
        assertThat((dictionary["test1"] as PDFString).value, `is`("Hello"))
        assertThat((dictionary["test2"] as PDFString).value, `is`("World"))
        println("Testing dictionary from string success")

        s = "<</Reference 12 0 R >>"
        ObjectIdentifier.referenceResolver = object : ReferenceResolver {
            override fun resolveReferenceToStream(reference: Reference): Stream? {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun resolveReference(reference: Reference): PDFObject? {
                return reference
            }
        }
        dictionary = s.toDictionary()
        assertThat((dictionary["Reference"] as Reference).obj, `is`(12))
        assertThat((dictionary["Reference"] as Reference).gen, `is`(0))
        println("Testing indirect object reference entry success")
    }
}