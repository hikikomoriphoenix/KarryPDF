package marabillas.loremar.andpdf.filters

import marabillas.loremar.andpdf.objects.*

internal class DecoderFactory {
    fun getDecoder(filter: String, objDic: Dictionary? = null): Decoder {
        //println("Filter->$filter")
        return when (filter) {
            "ASCIIHexDecode" -> ASCIIHex()
            "ASCII85Decode" -> ASCII85()
            "FlateDecode" -> Flate(objDic?.getDecodeParams(filter))
            "LZWDecode" -> LZW(objDic?.getDecodeParams(filter))
            "CCITTFaxDecode" -> {
                val height = objDic?.get("Height") as Numeric
                return CCITTFax(objDic.getDecodeParams(filter), height.value.toInt())
            }
            "RunLengthDecode" -> RunLength()
            else -> Identity()
        }
    }

    private fun Dictionary.getDecodeParams(filter: String): Dictionary? {
        var paramsDictionary = this["DecodeParms"]
        if (paramsDictionary is PDFArray) {
            val filters = this["Filter"] as PDFArray
            val filterIndex = filters.indexOfFirst { (it as Name).value == filter }
            paramsDictionary = paramsDictionary[filterIndex]
        }

        if (paramsDictionary is Reference)
            paramsDictionary = paramsDictionary.resolve()
        return if (paramsDictionary is Dictionary)
            paramsDictionary.resolveReferences()
        else null
    }
}