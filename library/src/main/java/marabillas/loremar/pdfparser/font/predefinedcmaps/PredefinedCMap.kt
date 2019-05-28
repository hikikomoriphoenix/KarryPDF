package marabillas.loremar.pdfparser.font.predefinedcmaps

import marabillas.loremar.pdfparser.font.CIDFontCMap

internal class PredefinedCMap : CIDFontCMap {
    override fun cidToUnicode(cid: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun decodeString(encoded: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    // TODO Put all predefined CMap files in this package. To read a file in the same package call javaclass.getResource()
}