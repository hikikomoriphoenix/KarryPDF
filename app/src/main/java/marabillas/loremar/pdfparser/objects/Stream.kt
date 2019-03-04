package marabillas.loremar.pdfparser.objects

import marabillas.loremar.pdfparser.filters.DecoderFactory
import java.io.RandomAccessFile

open class Stream(file: RandomAccessFile, start: Long) : Indirect(file, start) {
    val dictionary = Dictionary(file, start).parse()
    var streamData: String = ""
        private set

    init {
        file.seek(start)
        var s = ""
        while (!s.equals("stream", true))
            s = file.readLine()

        val sb = StringBuilder()
        while (true) {
            val l = file.readLine()
            if (l.equals("endstream", true)) break
            sb.append(l)
        }
        streamData = sb.toString()
    }

    fun decodeEncodedStream(): String {
        val filterObj = dictionary["Filter"]

        var streamString = streamData
        when (filterObj) {
            is Array -> for (filterEntry in filterObj) {
                if (filterEntry is Name) {
                    val decoder = DecoderFactory().getDecoder(filterEntry.value, dictionary)
                    streamString = decoder.decode(streamString).toString()
                }
            }
            is Name -> {
                val decoder = DecoderFactory().getDecoder(filterObj.value, dictionary)
                streamString = decoder.decode(streamString).toString()
            }
        }

        return streamString
    }

}