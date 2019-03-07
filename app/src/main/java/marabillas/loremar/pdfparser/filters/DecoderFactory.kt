package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.objects.Dictionary
import marabillas.loremar.pdfparser.objects.Numeric

class DecoderFactory {
    fun getDecoder(filter: String, objDic: Dictionary? = null): Decoder {
        return when (filter) {
            "ASCIIHexDecode" -> ASCIIHex()
            "ASCII85Decode" -> ASCII85()
            "FlateDecode" -> Flate(objDic?.getDecodeParams())
            "LZWDecode" -> LZW(objDic?.getDecodeParams())
            "CCITTFaxDecode" -> {
                val height = objDic?.get("height") as Numeric
                return CCITTFax(objDic.getDecodeParams(), height.value.toInt())
            }
            "RunLengthDecode" -> RunLength()
            else -> Identity()
        }
    }

    private fun Dictionary.getDecodeParams(): Dictionary? {
        val paramsDictionary = this["DecodeParams"]
        return if (paramsDictionary is Dictionary) paramsDictionary
        else null
    }
}