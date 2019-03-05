package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.objects.Dictionary

class DecoderFactory {
    fun getDecoder(filter: String, objDic: Dictionary? = null): Decoder {
        return when (filter) {
            "ASCIIHexDecode" -> ASCIIHex()
            "ASCII85Decode" -> ASCII85()
            "FlateDecode" -> Flate(objDic?.getDecodeParams())
            "LZWDecode" -> LZW(objDic?.getDecodeParams())
            "CCITTFaxDecode" -> {
                val height = Integer.valueOf(objDic?.get("height"))
                return CCITTFax(objDic?.getDecodeParams(), height)
            }
            "RunLengthDecode" -> RunLength()
            else -> Identity()
        }
    }

    private fun Dictionary.getDecodeParams(): Dictionary? {
        val paramsString = this["DecodeParams"]
        var paramsDictionary: Dictionary? = null
        if (paramsString != null) {
            paramsDictionary = Dictionary(paramsString).parse()
        }
        return paramsDictionary
    }
}