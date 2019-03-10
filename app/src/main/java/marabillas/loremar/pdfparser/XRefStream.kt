package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.objects.Array
import marabillas.loremar.pdfparser.objects.Numeric
import marabillas.loremar.pdfparser.objects.Stream
import java.io.RandomAccessFile
import java.math.BigInteger

/**
 * Class for parsing a cross reference stream
 *
 * @param file  PDF file containing the stream.
 * @param start offset position where beginning of the cross reference stream's object is located.
 */
class XRefStream(private val file: RandomAccessFile, private val start: Long) : Stream(file, start) {
    fun parse(): HashMap<String, XRefEntry> {
        val stream = decodeEncodedStream()

        val index = dictionary["Index"] as Array?
        val w = dictionary["W"] as Array
        val w0 = (w[0] as Numeric).value.toInt()
        val w1 = (w[1] as Numeric).value.toInt()
        val w2 = (w[2] as Numeric).value.toInt()

        var entries = HashMap<String, XRefEntry>()
        if (index != null) {
            println("Parsing XRefStream start")
            for (i in 0 until index.count() step 2) {
                val start = (index[i] as Numeric).value.toInt()
                val count = (index[i + 1] as Numeric).value.toInt()

                repeat(count) {
                    print("Parsing XRef entry for obj ${start + it}")
                    val fields = arrayOf(ByteArray(w0), ByteArray(w1), ByteArray(w2))
                    repeat(3) { i ->
                        repeat(fields[i].count()) { j ->
                            fields[i][j] = stream.inputStream().read().toByte()
                        }
                    }

                    val entry = XRefEntry(start + it)
                    val type = if (fields[0].count() != 0)
                        BigInteger(fields[0]).toInt()
                    else 1

                    val second = if (fields[1].count() != 0)
                        BigInteger(fields[1]).toInt()
                    else 0

                    val thirdDefault = when (type) {
                        0 -> 65535
                        1 -> 0
                        2 -> -1
                        else -> 0
                    }

                    val third = if (fields[2].count() != 0)
                        BigInteger(fields[2]).toInt()
                    else thirdDefault

                    when (type) {
                        0 -> {
                            entry.gen = third
                            entry.inUse = false
                        }
                        1 -> {
                            entry.pos = second.toLong()
                            entry.gen = third
                        }
                        2 -> {
                            entry.gen = 0
                            entry.compressed = true
                            entry.objStm = second
                            entry.index = third
                        }
                    }

                    entries["${start + it} ${entry.gen}"] = entry
                    println(" $entry")
                }
            }
            println("Parsing XRefStream end")
        }

        val prev = dictionary["Prev"] as Numeric?
        if (prev != null) {
            println("Prev = ${prev.value.toLong()}")
            val data = PDFFileReader(file).getXRefData(prev.value.toLong())
            data.putAll(entries)
            entries = data
        }

        return entries
    }
}