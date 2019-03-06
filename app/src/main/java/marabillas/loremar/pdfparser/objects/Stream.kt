package marabillas.loremar.pdfparser.objects

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
}