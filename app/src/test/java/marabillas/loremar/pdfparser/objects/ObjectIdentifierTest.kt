package marabillas.loremar.pdfparser.objects

import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectIdentifierTest {
    @Test
    fun testObjectIdentifier() {
        var obj = "false".toPDFObject()
        assertTrue("Did not convert to PDFBoolean as expected", obj is PDFBoolean)

        obj = "12345.67890".toPDFObject()
        assertTrue("Did not convert to Numeric as expected", obj is Numeric)

        obj = "(Hello World)".toPDFObject()
        assertTrue("Did not convert to PDFString as expected", obj is PDFString)

        obj = "<123ffaebcde>".toPDFObject()
        assertTrue("Did not convert to PDFString as expected", obj is PDFString)

        obj = "<</Name Value>>".toPDFObject()
        assertTrue("Did not convert to Dictionary as expected", obj is Dictionary)

        obj = "/SomeName".toPDFObject()
        assertTrue("Did not convert to Name as expected", obj is Name)

        obj = "[/Name 12345 (Text)]".toPDFObject()
        assertTrue("Did not convert to PDFArray as expected", obj is PDFArray)

        obj = "12 0 R".toPDFObject()
        assertTrue("Did not convert to Reference as expected", obj is Reference)

        obj = "null".toPDFObject()
        assertTrue("Did not convert to null as expected", obj == null)
    }
}