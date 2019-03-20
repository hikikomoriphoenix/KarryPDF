package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.objects.Dictionary
import marabillas.loremar.pdfparser.objects.Numeric
import marabillas.loremar.pdfparser.objects.Reference

internal class DecoderFactory {
    fun getDecoder(filter: String, objDic: Dictionary? = null): Decoder {
        //println("Filter->$filter")
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
        var paramsDictionary = this["DecodeParms"]
        if (paramsDictionary is Reference)
            paramsDictionary = paramsDictionary.resolve()
        return if (paramsDictionary is Dictionary)
            paramsDictionary.resolveReferences()
        else null
    }
}