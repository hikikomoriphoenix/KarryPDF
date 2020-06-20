package marabillas.loremar.karrypdf.document

import marabillas.loremar.karrypdf.objects.Numeric
import marabillas.loremar.karrypdf.objects.Reference
import marabillas.loremar.karrypdf.objects.toPDFObject
import marabillas.loremar.karrypdf.utils.logd
import java.io.RandomAccessFile

internal class TopDownParser(
    private val context: KarryPDFContext,
    private val file: RandomAccessFile
) {
    private var lastAdded: XRefEntry? = null
    private val stringBuilder = StringBuilder()

    fun parseObjects(): HashMap<String, XRefEntry> {
        logd("TopDownParser.parseObjects start")
        val objects = HashMap<String, XRefEntry>()
        file.seek(0)
        while (file.filePointer + 2 < file.length()) {
            var c = file.readByte().toChar()
            if (c == 'o') {
                c = file.readByte().toChar()
                if (c == 'b') {
                    c = file.readByte().toChar()
                    if (c == 'j') {
                        if (verifyObj()) {
                            extractObj(objects)
                        } else {
                            file.seek(file.filePointer + 3)
                            continue
                        }
                    }
                }
            } else if (c == 's') {
                c = file.readByte().toChar()
                if (c == 't') {
                    c = file.readByte().toChar()
                    if (c == 'r') {
                        c = file.readByte().toChar()
                        if (c == 'e') {
                            c = file.readByte().toChar()
                            if (c == 'a') {
                                c = file.readByte().toChar()
                                if (c == 'm') {
                                    val continuePos = file.filePointer
                                    if (!isEndStream()) {
                                        file.seek(continuePos)
                                        skipStream(objects)
                                    } else {
                                        file.seek(continuePos)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        logd("TopDownParser.parseObjects end")
        return objects
    }

    private fun verifyObj(): Boolean {
        file.seek(file.filePointer - 4)
        val c = file.readByte().toChar()
        return c == ' '
    }

    private fun extractObj(objects: HashMap<String, XRefEntry>) {
        val continuePosition = file.filePointer + 3
        file.seek(file.filePointer - 1)

        var skipSpace = skipSpace(continuePosition)
        if (!skipSpace) return

        val gen = extractNumber()
        file.seek(file.filePointer - 1)

        skipSpace = skipSpace(continuePosition)
        if (!skipSpace) return

        val obj = extractNumber()

        val newEntry = XRefEntry(
            obj = obj,
            gen = gen,
            pos = file.filePointer
        )
        objects["$obj $gen"] = newEntry
        lastAdded = newEntry
        file.seek(continuePosition)
    }

    private fun skipSpace(continuePosition: Long): Boolean {
        var haveSpace = false
        while (true) {
            val c = file.readByte().toChar()
            if (c != ' ') {
                return if (haveSpace && c.isDigit()) {
                    file.seek(file.filePointer - 1)
                    true
                } else {
                    file.seek(continuePosition)
                    false
                }
            } else {
                haveSpace = true
            }
            file.seek(file.filePointer - 2)
        }
    }

    private fun extractNumber(): Int {
        var number = 0
        var factor = 1
        while (true) {
            val c = file.readByte().toChar()
            if (c.isDigit()) {
                val num = Character.getNumericValue(c)
                number += (num * factor)
                factor *= 10
            } else {
                return number
            }
            file.seek(file.filePointer - 2)
        }
    }

    private fun skipStream(objects: HashMap<String, XRefEntry>) {
        lastAdded?.let { lastAdded ->
            val continuePos = file.filePointer
            val streamDic = context.fileReader.getDictionary(
                context,
                lastAdded.pos,
                lastAdded.obj,
                lastAdded.gen
            )
            var length = streamDic["Length"]
            if (length is Reference) {
                val lengthObj = length.obj
                val lengthGen = length.gen
                length = context.resolveReference(Reference(context, lengthObj, lengthGen), false)
                if (length == null) {
                    val lengthPos = objects["$lengthObj $lengthGen"]?.pos
                    if (lengthPos != null) {
                        stringBuilder.clear()
                        context.fileReader
                            .getIndirectObject(lengthPos)
                            .extractContent(stringBuilder)
                        length = stringBuilder.toPDFObject(context, lengthObj, lengthGen)
                    }
                }
            }
            if (length is Numeric) {
                file.seek(continuePos + length.value.toLong())
            } else {
                file.seek(continuePos)
            }
        }
    }

    private fun isEndStream(): Boolean {
        file.seek(file.filePointer - 7)
        while (true) {
            var c = file.readByte().toChar()
            return if (c == 'd') {
                file.seek(file.filePointer - 2)
                c = file.readByte().toChar()
                if (c == 'n') {
                    file.seek(file.filePointer - 2)
                    c = file.readByte().toChar()
                    c == 'e'
                } else {
                    false
                }
            } else {
                false
            }
        }
    }
}