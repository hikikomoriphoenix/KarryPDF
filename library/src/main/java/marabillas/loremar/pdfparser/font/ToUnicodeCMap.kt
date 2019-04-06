package marabillas.loremar.pdfparser.font

import marabillas.loremar.pdfparser.CaseInsensitiveMap
import marabillas.loremar.pdfparser.objects.PDFArray
import marabillas.loremar.pdfparser.objects.PDFString
import marabillas.loremar.pdfparser.objects.toPDFObject
import java.math.BigInteger

internal class ToUnicodeCMap(private var stream: String) : CMap {
    private val codeSpaceRange = ArrayList<Array<String>>()
    private val bfChars = CaseInsensitiveMap<String>()
    private val bfRanges = ArrayList<Array<String>>()
    private var cMapName = ""

    fun parse(): ToUnicodeCMap {
        //print("cmap -> $stream")

        val cmapP = "(/CMapName)[\\s\\S]+def".toRegex()
        val cmap = cmapP.find(stream)
        cmap?.let {
            cMapName = cmap.value
                .substringAfter("/CMapName")
                .substringAfter("/")
                .substringBeforeLast("def").trim()
        }

        // Parse for code spaces.
        var codespace = stream.substringAfter("begincodespacerange").substringBefore("endcodespacerange").trim()
        while (codespace != "") {
            val p = "<[0-9A-Za-z]{2,}>".toRegex()
            var lo = p.find(codespace)?.value ?: break
            codespace = codespace.substringAfter(lo)
            var hi = p.find(codespace)?.value ?: break
            codespace = codespace.substringAfter(hi).trim()

            lo = lo.removeSurrounding("<", ">")
            hi = hi.removeSurrounding("<", ">")
            codeSpaceRange.add(arrayOf(lo, hi))
        }

        val operator = "(beginbfchar|beginbfrange|endcmap)[\\s\\S]*$".toRegex()
        loop@ while (true) {
            stream = operator.find(stream)?.value ?: break

            when {
                stream.startsWith("beginbfchar") -> {
                    var bfchar = stream.substringAfter("beginbfchar").substringBefore("endbfchar")
                    val p = "<[0-9A-Za-z]{2,}>".toRegex()
                    while (bfchar != "") {
                        var src = p.find(bfchar)?.value ?: break
                        bfchar = bfchar.substringAfter(src)
                        var dst = p.find(bfchar)?.value ?: break
                        bfchar = bfchar.substringAfter(dst).trim()

                        src = src.removeSurrounding("<", ">")
                        dst = dst.removeSurrounding("<", ">")
                        bfChars[src] = dst
                    }
                    stream = stream.substringAfter("endbfchar")
                }
                stream.startsWith("beginbfrange") -> {
                    var bfrange = stream.substringAfter("beginbfrange").substringBefore("endbfrange")
                    val srcP = "<[0-9A-Za-z]{2,}>".toRegex()
                    val dstP = "(<[0-9A-Za-z]{2,}>)|(\\[( *<[0-9A-Za-z]{2,}> *)+])".toRegex()
                    while (bfrange != "") {
                        var lo = srcP.find(bfrange)?.value ?: break
                        bfrange = bfrange.substringAfter(lo)
                        var hi = srcP.find(bfrange)?.value ?: break
                        bfrange = bfrange.substringAfter(hi)
                        val dst = dstP.find(bfrange)?.value ?: break
                        bfrange = bfrange.substringAfter(dst).trim()

                        lo = lo.removeSurrounding("<", ">")
                        hi = hi.removeSurrounding("<", ">")
                        bfRanges.add(arrayOf(lo, hi, dst))
                    }
                    stream = stream.substringAfter("endbfrange")
                }
                stream.startsWith("endcmap") -> break@loop
            }
        }

        return this
    }

    override fun decodeString(encoded: String): String {
        val sb = StringBuilder(encoded)
        val decoded = StringBuilder()

        if (cMapName.contains("Identity", true) && cMapName.contains("UCS", true)) {
            sb.insert(0, "00")
        }

        //println("sb->$sb")

        fun convertCodeToCharAndAppend(uCode: String) {
            when {
                uCode.length == 2 -> {
                    val n = Integer.parseInt(uCode, 16)
                    //print("->${n.toChar()} ")
                    decoded.append(n.toChar())
                }
                else -> {
                    for (i in 0 until uCode.length step 4) {
                        val ux = uCode.substring(i, i + 4)
                        val n = Integer.parseInt(ux, 16)
                        //print("->${n.toChar()} ")
                        decoded.append(n.toChar())
                    }
                }
            }
        }

        while (sb.isNotEmpty()) {
            var l = 0

            // Determine if next code is valid.
            val nextIsValid = fun(): Boolean {
                codeSpaceRange.forEach {
                    l = it[0].length
                    if (sb.length < l)
                        return@forEach
                    val code = sb.substring(0, l)
                    if (isWithinRange(it, code)) return true
                }
                return false
            }
            if (!nextIsValid()) {
                //println("->� ")
                decoded.append(" ")
                sb.delete(0, 2)
                continue
            }

            val code = sb.substring(0, l)
            //print(code)

            // Attempt to get unicode from bfChars
            var uCode = bfChars[code] ?: ""
            //println("unicode=$uCode")
            if (uCode.isNotEmpty()) {
                convertCodeToCharAndAppend(uCode)
                sb.delete(0, l)
                continue
            }

            val range = bfRanges
                .asSequence()
                .filter { it[0].length == l && isWithinRange(arrayOf(it[0], it[1]), code) }
            if (range.count() > 0) {
                range
                    .first()
                    .run {
                        val loInt = Integer.parseInt(this[0], 16)
                        val cInt = Integer.parseInt(code, 16)
                        val offset = cInt - loInt

                        val u = this[2].toPDFObject()
                        uCode = when (u) {
                            is PDFString -> {
                                val uValue = u.original.removeSurrounding("<", ">")
                                val u0Int = BigInteger(uValue, 16)
                                val uInt = u0Int.toLong() + offset
                                uInt.toBigInteger().toString(16)
                            }
                            is PDFArray -> {
                                (u[offset] as PDFString).original.removeSurrounding("<", ">")
                            }
                            else -> ""
                        }
                    }
                if (uCode.isNotEmpty()) {
                    convertCodeToCharAndAppend(uCode)
                    sb.delete(0, l)
                    continue
                }
            }

            // If unicode value not found, substitute with � character.
            //println("->� ")
            decoded.append(" ")
            sb.delete(0, 2)
        }

        return decoded.toString()
    }
}