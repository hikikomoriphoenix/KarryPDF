package marabillas.loremar.pdfparser.font

import android.graphics.Typeface
import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.font.ttf.TTFParser
import marabillas.loremar.pdfparser.objects.*

internal class Font() {
    var typeface: Typeface = FontMappings[FontName.DEFAULT]
        private set
    var cmap: CMap? = null
        private set
    var widths = SparseArrayCompat<Float>()
        private set

    constructor(dictionary: Dictionary, referenceResolver: ReferenceResolver) : this() {
        dictionary.resolveReferences()
        val fontDescriptor = dictionary["FontDescriptor"]
        if (fontDescriptor is Dictionary) {
            fontDescriptor.resolveReferences()
        }

        // Get type of font
        val subtype = dictionary["Subtype"]
        if (subtype is Name) {
            println("Using ${subtype.value} font")
        }

        // Get typeface
        val baseFont = dictionary["BaseFont"]
        if (baseFont is Name) {
            typeface = FontIdentifier().identifyFont(baseFont.value)
            println("Basefont=${baseFont.value}")
        }

        // Get cmap
        val toUnicode = dictionary["ToUnicode"]
        if (toUnicode is Reference) {
            val stream = referenceResolver.resolveReferenceToStream(toUnicode)
            val b = stream?.decodeEncodedStream()
            b?.let {
                cmap = ToUnicodeCMap(String(b)).parse()
            }
        }

        // Get Encoding
        val encoding = dictionary["Encoding"]
        if (encoding is Name) {
            println("Font uses ${encoding.value}")
        } else if (encoding is Dictionary) {
            println("Font uses an Encoding dictionary")
        } else if (encoding is Reference) {
            println("Font is a Composite Font and may be using a CMap")
        }

        // If a Simple Font
        if (subtype is Name && subtype.value != "Type0") {
            // Get FirstChar
            var firstChar = 0
            val fc = dictionary["FirstChar"]
            if (fc is Numeric) {
                firstChar = fc.value.toInt()
            }

            // Get MissingWidth
            var missingWidth: Float? = null
            if (fontDescriptor is Dictionary) {
                val mw = fontDescriptor["MissingWidth"]
                if (mw is Numeric) {
                    missingWidth = mw.value.toFloat()
                }
            }

            // Get widths
            val ws = dictionary["Widths"]
            if (ws is PDFArray && missingWidth != null) {
                // Assign missing character width
                widths.put(-1, missingWidth)

                for (i in 0 until ws.count()) {
                    widths.put(firstChar + i, (ws[i] as Numeric).value.toFloat())
                }
            } else {
                if (fontDescriptor is Dictionary) {
                    getWidthsFromFontProgram(fontDescriptor, referenceResolver, encoding)
                }
            }
        } else {
            // Else a Composite Font

            // Handle encoding
            if (encoding is Name && !encoding.value.contains("Identity")) {
                TODO("Needs to handle predefined CMaps. If ToUnicodeCMap exist, pass it to constructor.")
            } else if (encoding is Reference) {
                TODO("Needs to handle embedded CMaps. If ToUnicodeCMap exist, pass it to constructor.")
            }

            // Get widths and missing characters width
            val descendant = dictionary["DescendantFonts"]
            if (descendant is PDFArray) {
                descendant.resolveReferences()
                val cidFont = descendant[0]
                if (cidFont is Dictionary) {
                    cidFont.resolveReferences()

                    // Get default width
                    val dw = cidFont["DW"]
                    if (dw is Numeric) {
                        widths.put(-1, dw.value.toFloat())
                    } else {
                        widths.put(-1, 1000f)
                    }

                    // Get MissingWidth
                    val cidFontDescriptor = cidFont["FontDescriptor"]
                    if (cidFontDescriptor is Dictionary) {
                        cidFontDescriptor.resolveReferences()
                        val mw = cidFontDescriptor["MissingWidth"]
                        if (mw is Numeric) {
                            widths.put(-1, mw.value.toFloat())
                        }
                    }

                    if (widths[-1] == 0f && cidFontDescriptor is Dictionary) {
                        cidFontDescriptor.resolveReferences()
                        getWidthsFromFontProgram(cidFontDescriptor, referenceResolver, encoding)
                    } else {
                        // Get widths
                        val w = cidFont["W"]
                        if (w is PDFArray) {
                            getCIDFontWidths(w)
                            println("obtained ${widths.size()} widths")
                        }
                    }

                    println("missing width = ${widths[-1]}")
                }
            }
        }
    }

    private fun getWidthsFromFontProgram(
        fontDescriptor: Dictionary,
        referenceResolver: ReferenceResolver,
        encoding: PDFObject? = null
    ) {
        val fontFile = fontDescriptor["FontFile"]
        val fontFile2 = fontDescriptor["FontFile2"]
        val fontFile3 = fontDescriptor["FontFile3"]

        when {
            fontFile is Reference -> {
                val fontProgram = referenceResolver.resolveReferenceToStream(fontFile)
                val data = fontProgram?.decodeEncodedStream()
                if (data is ByteArray) {
                    val diffArray = SparseArrayCompat<String>()
                    if (encoding is Dictionary) {
                        getDifferencesArray(encoding, diffArray)
                    }
                    widths = Type1Parser(data).getCharacterWidths(diffArray)
                }
            }
            fontFile2 is Reference -> {
                val fontProgram = referenceResolver.resolveReferenceToStream(fontFile2)
                val data = fontProgram?.decodeEncodedStream()
                if (data is ByteArray) {
                    widths = TTFParser(data).getCharacterWidths()
                }
            }
            fontFile3 is Stream -> {
                TODO("Getting widths from fontFile3 not implemented")
            }
        }
    }

    private fun getDifferencesArray(encoding: Dictionary, diffArray: SparseArrayCompat<String>) {
        encoding.resolveReferences()
        val differences = encoding["Differences"]
        if (differences is PDFArray) {
            var startCode = 0
            var offset = 0
            differences.forEach {
                if (it is Numeric) {
                    startCode = it.value.toInt()
                    offset = 0
                } else if (it is Name) {
                    diffArray.put(
                        startCode + offset,
                        it.value
                    )
                    offset++
                }
            }
        }
    }

    private fun getCIDFontWidths(w: PDFArray) {
        var c = 0
        var cFirst = 0
        var cLast = 0
        w.forEach {
            if (it is Numeric) {
                when (c) {
                    0 -> {
                        cFirst = it.value.toInt()
                        c++
                    }
                    1 -> {
                        cLast = it.value.toInt()
                        c++
                    }
                    else -> {
                        // Assign width to range
                        for (cid in cFirst..cLast) {
                            var unicode = cid
                            if (cmap is CIDFontCMap) {
                                unicode = (cmap as CIDFontCMap).cidToUnicode(cid)
                            }
                            widths.put(unicode, it.value.toFloat())
                        }

                        c = 0
                    }
                }
            } else if (it is PDFArray) {
                c = 0
                var i = cFirst
                it.forEach { cw ->
                    var unicode = i
                    if (cmap is CIDFontCMap) {
                        unicode = (cmap as CIDFontCMap).cidToUnicode(i)
                    }
                    if (cw is Numeric) {
                        widths.put(unicode, cw.value.toFloat())
                    }
                    i++
                }
            }
        }
    }
}