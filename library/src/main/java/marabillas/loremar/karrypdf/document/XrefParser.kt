package marabillas.loremar.karrypdf.document

import java.io.RandomAccessFile
import java.nio.ByteBuffer

internal class XrefParser(private val file: RandomAccessFile) {
    private val fileChannel = file.channel

    fun parseEntries(startObj: Int, count: Int, entries: HashMap<String, XRefEntry>) {
        val startPos = file.filePointer
        file.seek(startPos + 20)

        // Each line is 20 bytes including end of line marker. But if end of line marker consists of
        // '\r\n' then line is 21 bytes each.
        val lineLength = if (file.read().toChar() == '\n') 21 else 20
        file.seek(startPos)

        val buffer =
            ByteBuffer.allocateDirect(count * lineLength)
        fileChannel.position(file.filePointer)
        fileChannel.read(buffer)
        file.seek(fileChannel.position())
        buffer.rewind()

        var i = 0
        while (i < count) {
            // Get object position
            var tens = 1000000000
            var pos = 0L
            buffer.position(i * lineLength)
            while (tens >= 1) {
                val c = buffer.get().toChar()
                val d = Character.getNumericValue(c)
                pos += (d * tens)
                tens /= 10
            }

            // Get object generation number
            tens = 10000
            var gen = 0
            buffer.position(i * lineLength + 11)
            while (tens >= 1) {
                val c = buffer.get().toChar()
                val d = Character.getNumericValue(c)
                gen += (d * tens)
                tens /= 10
            }

            // Get in-use value
            buffer.position(i * lineLength + 17)
            val nOrF = buffer.get().toChar()

            if (nOrF == 'f') {
                entries["${startObj + i} $gen"] = XRefEntry(startObj + i, pos, gen, false)
            } else {
                entries["${startObj + i} $gen"] = XRefEntry(startObj + i, pos, gen)
            }

            i++
        }
    }
}