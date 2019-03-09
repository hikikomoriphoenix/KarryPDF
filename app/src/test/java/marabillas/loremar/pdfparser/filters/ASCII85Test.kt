package marabillas.loremar.pdfparser.filters

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ASCII85Test {
    @Test
    fun testDecode() {
        var encoded = "<~87cURD]i,\"Ebo7~>"
        var bytes = ASCII85().decode(encoded.toByteArray())
        assertThat(String(bytes), `is`("Hello World"))
        println("Encoded string $encoded is successfully decoded to 'Hello World'.")

        encoded = "<~<+ohcEHPu*CER),Dg-(AAoDo:C3=B4F!,CEATAo8BOr<&@=!2AA8c*5+>>N*1GgsI2`Ne~>"
        bytes = ASCII85().decode(encoded.toByteArray())
        assertThat(String(bytes), `is`("The quick brown fox jumps over the lazy dog. 0123456789"))
        println(
            "Encoded string $encoded is successfully decoded to 'The quick brown fox jumps over the lazy dog." +
                    " 0123456789'."
        )
    }
}