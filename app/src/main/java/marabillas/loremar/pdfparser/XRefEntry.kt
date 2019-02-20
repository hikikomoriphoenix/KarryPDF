package marabillas.loremar.pdfparser

data class XRefEntry(var obj: Int, var pos: Long = 0, var gen: Int = 65535, var inUse: Boolean = true)