package marabillas.loremar.andpdf.document

import marabillas.loremar.andpdf.objects.Numeric
import marabillas.loremar.andpdf.objects.PDFArray
import marabillas.loremar.andpdf.objects.Stream
import marabillas.loremar.andpdf.utils.logd
import java.io.RandomAccessFile
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels

/**
 * Class for parsing a cross reference stream
 *
 * @param file  PDF file containing the stream.
 * @param start offset position where beginning of the cross reference stream's object is located.
 */
internal class XRefStream(
    private val context: AndPDFContext,
    private val file: RandomAccessFile,
    private val start: Long
) : Stream(context, file, start) {
    var entries = HashMap<String, XRefEntry>()

    fun parse(): HashMap<String, XRefEntry> {
        val stream = decodeEncodedStream()

        val index = dictionary["Index"] as PDFArray?
        val w = dictionary["W"] as PDFArray
        val w0 = (w[0] as Numeric).value.toInt()
        val w1 = (w[1] as Numeric).value.toInt()
        val w2 = (w[2] as Numeric).value.toInt()

        if (index != null) {
            logd("Parsing XRefStream start")
            val channel = Channels.newChannel(stream.inputStream())
            for (i in 0 until index.count() step 2) {
                val start = (index[i] as Numeric).value.toInt()
                val count = (index[i + 1] as Numeric).value.toInt()

                repeat(count) {
                    //logd("Parsing XRef entry for obj ${start + it}")
                    val fields = arrayOf(ByteBuffer.allocate(w0), ByteBuffer.allocate(w1), ByteBuffer.allocate(w2))
                    repeat(3) { m ->
                        if (fields[m].capacity() > 0) {
                            fields[m].order(ByteOrder.BIG_ENDIAN)
                            while (fields[m].hasRemaining()) {
                                channel.read(fields[m])
                            }
                        }
                    }

                    /*if (fields[0].capacity() > 0 && fields[1].capacity() > 0 && fields[2].capacity() > 0) {
                        logd(
                            " x=${getUnsignedNumber(fields[0])} y=${getUnsignedNumber(fields[1])} z=${getUnsignedNumber(
                                fields[2]
                            )} "
                        )
                    }*/

                    addEntry(start + it, fields)
                }
            }
            logd("Parsing XRefStream end")
            channel.close()
        } else {
            logd("Parsing XRefStream start")
            val channel = Channels.newChannel(stream.inputStream())
            var count = 0
            loop@ while (true) {
                val fields = arrayOf(ByteBuffer.allocate(w0), ByteBuffer.allocate(w1), ByteBuffer.allocate(w2))
                for (i in 0 until 3) {
                    if (fields[i].capacity() > 0) {
                        fields[i].order(ByteOrder.BIG_ENDIAN)
                        while (fields[i].hasRemaining()) {
                            val read = channel.read(fields[i])
                            if (read == -1) break@loop
                        }
                    }
                }
                addEntry(count, fields)
                count++
            }
            logd("Parsing XRefStream end")
            channel.close()
        }

        val prev = dictionary["Prev"] as Numeric?
        if (prev != null) {
            logd("Prev = ${prev.value.toLong()}")
            val data = PDFFileReader(context, file).getXRefData(prev.value.toLong())
            data.putAll(entries)
            entries = data
        }

        return entries
    }

    private fun addEntry(obj: Int, fields: Array<ByteBuffer>) {
        val entry = XRefEntry(obj)
        val type = if (fields[0].capacity() != 0) getUnsignedNumber(fields[0]).toInt() else 1

        val second = if (fields[1].capacity() != 0) getUnsignedNumber(fields[1]).toLong() else 0

        val thirdDefault = when (type) {
            0 -> 65535
            1 -> 0
            2 -> -1
            else -> 0
        }
        val third = if (fields[2].capacity() != 0) getUnsignedNumber(fields[2]).toInt() else thirdDefault

        when (type) {
            0 -> {
                entry.gen = third
                entry.inUse = false
            }
            1 -> {
                if (second < 0) {
                    entry.compressed = true
                    entry.objStm = -second.toInt()
                    entry.gen = third
                } else {
                    entry.pos = second
                    entry.gen = third
                }
            }
            2 -> {
                entry.gen = 0
                entry.compressed = true
                entry.objStm = second.toInt()
                entry.index = third
            }
            else -> {
                entry.nullObj = true
            }
        }

        entries["$obj ${entry.gen}"] = entry
    }


    private fun getUnsignedNumber(buffer: ByteBuffer): BigInteger {
        // If buffer contains only one byte, return number from 0 to 255
        return if (buffer.capacity() == 1) {
            BigInteger(buffer.array()) and 0xff.toBigInteger()
        } else {
            BigInteger(buffer.array())
        }
    }
}