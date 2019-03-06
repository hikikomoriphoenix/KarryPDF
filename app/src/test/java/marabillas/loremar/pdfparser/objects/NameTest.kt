package marabillas.loremar.pdfparser.objects

import junit.framework.Assert
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class NameTest {
    @Test
    fun testNameOperations() {
        val name = "/Loremar".toName()
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        print(name)
        assertThat(out.toString(), `is`("Loremar"))

        val nameIs = "Name is $name"
        assertThat(nameIs, `is`("Name is Loremar"))

        if (!name.equals("Loremar")) {
            Assert.fail()
        }
    }
}