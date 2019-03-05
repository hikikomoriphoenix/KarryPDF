package marabillas.loremar.pdfparser.filters

import junit.framework.Assert.assertTrue
import marabillas.loremar.pdfparser.objects.Dictionary
import org.junit.Test

class DecoderFactoryTest {
    @Test
    fun testForASCIIHex() {
        val decoder = DecoderFactory().getDecoder("ASCIIHexDecode")
        assertTrue(decoder is ASCIIHex)
    }

    @Test
    fun testForASCII85() {
        val decoder = DecoderFactory().getDecoder("ASCII85Decode")
        assertTrue(decoder is ASCII85)
    }

    @Test
    fun testForLZW() {
        val objDic = Dictionary("<</DecodeParams <<>>>>").parse()
        val decoder = DecoderFactory().getDecoder("LZWDecode", objDic)
        assertTrue(decoder is LZW)
    }

    @Test
    fun testForFlate() {
        val objDic = Dictionary("<</DecodeParams <<>>>>").parse()
        val decoder = DecoderFactory().getDecoder("FlateDecode", objDic)
        assertTrue(decoder is Flate)
    }

    @Test
    fun testForCCITTFax() {
        val objDic = Dictionary("<</DecodeParams <<>>/height 0>>").parse()
        val decoder = DecoderFactory().getDecoder("CCITTFaxDecode", objDic)
        assertTrue(decoder is CCITTFax)
    }

    @Test
    fun testForRunLength() {
        val decoder = DecoderFactory().getDecoder("RunLengthDecode")
        assertTrue(decoder is RunLength)
    }

    @Test
    fun testForIdentity() {
        val decoder = DecoderFactory().getDecoder("DCTDecode")
        assertTrue(decoder is Identity)
    }
}