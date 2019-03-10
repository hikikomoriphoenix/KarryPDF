package marabillas.loremar.pdfparser.objects

import marabillas.loremar.pdfparser.filters.DecoderFactory
import java.io.RandomAccessFile

open class Stream(file: RandomAccessFile, start: Long) : Indirect(file, start) {
    val dictionary = Dictionary(file, start).parse()
    var streamData = ByteArray((dictionary["Length"] as Numeric).value.toInt())
        private set

    init {
        file.seek(start)
        var s = ""
        while (!s.endsWith("stream", true))
            s = file.readLine()
        file.readFully(streamData)
    }

    fun decodeEncodedStream(): ByteArray {
        val filterObj = dictionary["Filter"]

        var data = streamData
        when (filterObj) {
            is Array -> for (filterEntry in filterObj) {
                if (filterEntry is Name) {
                    val decoder = DecoderFactory().getDecoder(filterEntry.value, dictionary)
                    data = decoder.decode(data)
                }
            }
            is Name -> {
                val decoder = DecoderFactory().getDecoder(filterObj.value, dictionary)
                data = decoder.decode(data)
            }
        }

        return data
    }

}