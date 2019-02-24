package marabillas.loremar.pdfparser

import junit.framework.Assert
import org.junit.Test

class CaseInsensitiveMapTest {
    @Test
    fun test() {
        val map = CaseInsensitiveMap()
        map["cAt"] = 10
        map["bAT"] = 20
        Assert.assertEquals(map["CaT"], 10)
        Assert.assertEquals(map["Bat"], 20)
    }
}