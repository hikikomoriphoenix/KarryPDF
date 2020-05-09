package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.exceptions.NoDocumentException
import marabillas.loremar.karrypdf.filters.DecoderFactory
import java.io.RandomAccessFile

internal open class Stream(
    private val context: KarryPDFContext,
    file: RandomAccessFile,
    start: Long,
    reference: Reference? = null
) :
    Indirect(file, start, reference) {
    val dictionary = context.fileReader?.getDictionary(context, start, obj ?: -1, 0)
        ?: throw NoDocumentException()
    var streamData = byteArrayOf()
        private set

    init {
        var length = dictionary["Length"]
        if (length is Reference) {
            length = length.resolve()
        }
        streamData = ByteArray((length as Numeric).value.toInt())

        file.seek(start)
        var s = ""
        while (!s.endsWith("stream", true))
            s = file.readLine()
        file.readFully(streamData)
    }

    fun decodeEncodedStream(): ByteArray {
        val filterObj = dictionary["Filter"]

        var data = streamData
        data = context.decryptor?.decrypt(data, obj as Int, gen) ?: data
        when (filterObj) {
            is PDFArray -> for (filterEntry in filterObj) {
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