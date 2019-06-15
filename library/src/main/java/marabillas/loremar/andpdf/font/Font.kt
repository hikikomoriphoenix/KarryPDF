package marabillas.loremar.andpdf.font

import android.graphics.Typeface
import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.andpdf.font.cmap.AGLCMap
import marabillas.loremar.andpdf.font.cmap.CIDFontCMap
import marabillas.loremar.andpdf.font.cmap.CMap
import marabillas.loremar.andpdf.font.cmap.ToUnicodeCMap
import marabillas.loremar.andpdf.font.encoding.*
import marabillas.loremar.andpdf.font.ttf.TTFParser
import marabillas.loremar.andpdf.objects.*

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
        // Get type of font
        val subtype = dictionary["Subtype"]
        // Get name of font
        val baseFont = dictionary["BaseFont"]
        // Get encoding used for font
        val encoding = dictionary["Encoding"]
        // Get cmap of character code to unicode mappings
        val toUnicode = dictionary["ToUnicode"]

        processDictionaryEntries(fontDescriptor, subtype, baseFont, encoding, toUnicode, referenceResolver)

        if (subtype is Name && subtype.value != "Type0") {
            setupSimpleFont(dictionary, fontDescriptor, subtype, baseFont, encoding, referenceResolver)
        } else {
            setupCompositeFont(dictionary, encoding, referenceResolver)
        }
    }

    private fun processDictionaryEntries(
        fontDescriptor: PDFObject?,
        subtype: PDFObject?,
        baseFont: PDFObject?,
        encoding: PDFObject?,
        toUnicode: PDFObject?,
        referenceResolver: ReferenceResolver
    ) {
        // FontDescriptor
        if (fontDescriptor is Dictionary) {
            fontDescriptor.resolveReferences()
        }

        // Subtype
        if (subtype is Name) {
            println("Using ${subtype.value} font")
        }

        // BaseFont
        if (baseFont is Name) {
            typeface = FontIdentifier().identifyFont(baseFont.value)
            println("Basefont=${baseFont.value}")
        }

        // Encoding
        if (encoding is Name) {
            println("Font uses ${encoding.value}")
        } else if (encoding is Dictionary) {
            println("Font uses an Encoding dictionary")
        } else if (encoding is Reference) {
            println("Font is a Composite Font and may be using a CMap")
        }

        // ToUnicode
        if (toUnicode is Reference) {
            val stream = referenceResolver.resolveReferenceToStream(toUnicode)
            val b = stream?.decodeEncodedStream()
            b?.let {
                cmap = ToUnicodeCMap(String(b)).parse()
            }
        }
    }

    private fun setupSimpleFont(
        dictionary: Dictionary,
        fontDescriptor: PDFObject?,
        subtype: Name,
        baseFont: PDFObject?,
        encoding: PDFObject?,
        referenceResolver: ReferenceResolver
    ) {
        val encodingArray = SparseArrayCompat<String>()
        val symbolic = checkSymbolicSimpleFont(fontDescriptor, baseFont, encodingArray)

        if ((encoding == null || symbolic) && subtype.value == "TrueType" && fontDescriptor is Dictionary) {
            val fontFile2 = fontDescriptor["FontFile2"]
            if (fontFile2 is Reference) {
                val fontProgram = referenceResolver.resolveReferenceToStream(fontFile2)
                val data = fontProgram?.decodeEncodedStream()
                if (data is ByteArray) {
                    TTFParser(data).getDefaultTTFEncoding(encodingArray)
                }
            }
        } else {
            getSimpleFontEncoding(encoding, encodingArray, fontDescriptor, subtype, referenceResolver, symbolic)

            if (cmap !is ToUnicodeCMap) {
                cmap = AGLCMap(encodingArray)
            }
        }

        getSimpleFontWidths(dictionary, fontDescriptor, referenceResolver, encodingArray, subtype)
    }

    private fun checkSymbolicSimpleFont(
        fontDescriptor: PDFObject?,
        baseFont: PDFObject?,
        encodingArray: SparseArrayCompat<String>
    ): Boolean {
        var symbolic = false
        if (fontDescriptor is Dictionary) {
            val flags = (fontDescriptor["Flags"] as Numeric).value.toInt()
            // Check if third lower bit is set as 1 in flags(equals to 4 if only this flag is set). If it is,
            // then font is symbolic.
            if ((flags and 4) != 0) {
                symbolic = true
                println("Symbolic flag is set true")
                if (baseFont is Name && baseFont.value.contains("Zapf", true)
                    && baseFont.value.contains("Dingbats", true)
                ) {
                    ZapfDingbatsEncoding.putAllTo(encodingArray)
                } else if (baseFont is Name && baseFont.value.contains("Symbol", true)) {
                    SymbolEncoding.putAllTo(encodingArray)
                }
            }
        }
        return symbolic
    }

    private fun getSimpleFontEncoding(
        encoding: PDFObject?,
        encodingArray: SparseArrayCompat<String>,
        fontDescriptor: PDFObject?,
        subtype: Name,
        referenceResolver: ReferenceResolver,
        symbolic: Boolean
    ) {
        when (encoding) {
            is Name -> {
                handleSimpleFontPredefinedEncoding(encoding, encodingArray)
                // If symbolic, get encoding from embedded font program to get symbolic characters
                if (symbolic && fontDescriptor is Dictionary)
                    getEncodingFromBuiltInProgram(fontDescriptor, referenceResolver, encodingArray, subtype)
            }
            is Dictionary -> {
                val baseEncoding = encoding["BaseEncoding"]
                if (baseEncoding is Name) {
                    handleSimpleFontPredefinedEncoding(baseEncoding, encodingArray)
                    // If symbolic, get encoding from embedded font program to get symbolic characters
                    if (symbolic && fontDescriptor is Dictionary) {
                        getEncodingFromBuiltInProgram(fontDescriptor, referenceResolver, encodingArray, subtype)
                    }
                } else {
                    // Get encoding from embedded font program.
                    if (fontDescriptor is Dictionary)
                        getEncodingFromBuiltInProgram(fontDescriptor, referenceResolver, encodingArray, subtype)
                    else if (!symbolic)
                        StandardEncoding.putAllTo(encodingArray)
                }
                getDifferencesArray(encoding, encodingArray)
            }
            else -> {
                // Get character names from embedded font program. Get unicode from AGL
                if (fontDescriptor is Dictionary)
                    getEncodingFromBuiltInProgram(fontDescriptor, referenceResolver, encodingArray, subtype)
                else if (!symbolic)
                    StandardEncoding.putAllTo(encodingArray)
            }
        }
    }

    private fun handleSimpleFontPredefinedEncoding(encoding: Name, encodingArray: SparseArrayCompat<String>) {
        when (encoding.value) {
            "MacRomanEncoding" -> MacRomanEncoding.putAllTo(encodingArray)
            "WinAnsiEncoding" -> WinAnsiEncoding.putAllTo(encodingArray)
            "MacExpertEncoding" -> MacExpertEncoding.putAllTo(encodingArray)
            "StandardEncoding" -> StandardEncoding.putAllTo(encodingArray)
        }
    }

    private fun getSimpleFontWidths(
        dictionary: Dictionary,
        fontDescriptor: PDFObject?,
        referenceResolver: ReferenceResolver,
        encodingArray: SparseArrayCompat<String>,
        subtype: Name
    ) {
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
                getWidthsFromFontProgram(fontDescriptor, referenceResolver, encodingArray, subtype)
            }
        }
    }

    private fun setupCompositeFont(dictionary: Dictionary, encoding: PDFObject?, referenceResolver: ReferenceResolver) {
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
                    val cidSubType = cidFont["Subtype"]
                    if (cidSubType is Name && cidSubType.value == "CIDFontType2") {
                        println("Getting widths for CIDFontType2")
                        getCIDType2Widths(cidFont, cidFontDescriptor, referenceResolver)
                        println("obtained ${widths.size()} widths")
                    } else if (cidSubType is Name) {
                        // Subtype is CIDFontType0. Use FontFile3 with subtype either CIDFontType0C or OpenType
                        getWidthsFromFontProgram(cidFontDescriptor, referenceResolver, null, cidSubType)
                    }
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

    private fun getWidthsFromFontProgram(
        fontDescriptor: Dictionary,
        referenceResolver: ReferenceResolver,
        encodingArray: SparseArrayCompat<String>?,
        fontType: Name
    ) {
        val fontFile = fontDescriptor["FontFile"]
        val fontFile2 = fontDescriptor["FontFile2"]
        val fontFile3 = fontDescriptor["FontFile3"]

        when {
            (fontType.value == "Type1" || fontType.value == "MMType1") && fontFile is Reference -> {
                val fontProgram = referenceResolver.resolveReferenceToStream(fontFile)
                val data = fontProgram?.decodeEncodedStream()
                if (data is ByteArray && encodingArray != null) {
                    widths = Type1Parser(data).getCharacterWidths(encodingArray, cmap)
                }
            }
            (fontType.value == "TrueType" || fontType.value == "CIDFontType2") && fontFile2 is Reference -> {
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

    private fun getCIDType2Widths(
        cidFont: Dictionary,
        cidFontDescriptor: Dictionary,
        referenceResolver: ReferenceResolver
    ) {
        val fontFile = cidFontDescriptor["FontFile2"]
        if (fontFile is Reference) {
            val fontProgram = referenceResolver.resolveReferenceToStream(fontFile)
            val data = fontProgram?.decodeEncodedStream()
            if (data is ByteArray) {
                val glyphWidths = TTFParser(data).getGlyphWidths()

                if (glyphWidths.isNotEmpty()) {
                    widths.put(-1, glyphWidths[0].toFloat())
                } else {
                    return
                }

                val cidToGidMap = cidFont["CIDToGIDMap"]
                if (cidToGidMap is Reference) {
                    val cid2gidMapping = referenceResolver.resolveReferenceToStream(cidToGidMap)
                    val mappingData = cid2gidMapping?.decodeEncodedStream()
                    if (mappingData is ByteArray) {
                        for (i in 0 until mappingData.size step 2) {
                            var glyphIndex = 0
                            glyphIndex = glyphIndex or (mappingData[i].toInt() and 0xff)
                            glyphIndex = glyphIndex shl 8
                            glyphIndex = glyphIndex or (mappingData[i + 1].toInt() and 0xff)

                            if (glyphIndex in 0 until glyphWidths.size) {
                                val width = glyphWidths[glyphIndex]
                                var unicode = i
                                if (cmap is CIDFontCMap) {
                                    unicode = (cmap as CIDFontCMap).cidToUnicode(i)
                                }
                                widths.put(unicode, width.toFloat())
                            }
                        }
                    }
                } else {
                    for (i in 0 until glyphWidths.size) {
                        var unicode = i
                        if (cmap is CIDFontCMap) {
                            unicode = (cmap as CIDFontCMap).cidToUnicode(i)
                        }
                        widths.put(unicode, glyphWidths[i].toFloat())
                    }
                }
            }
        } else {
            TODO("Needs to get glyph widths from outside the document using predefined cmap indicated in CIDSystemInfo")
        }
    }

    private fun getEncodingFromBuiltInProgram(
        fontDescriptor: Dictionary,
        referenceResolver: ReferenceResolver,
        encodingArray: SparseArrayCompat<String>,
        fontType: Name
    ) {
        val fontFile = fontDescriptor["FontFile"]
        val fontFile2 = fontDescriptor["FontFile2"]
        val fontFile3 = fontDescriptor["FontFile3"]

        when {
            (fontType.value == "Type1" || fontType.value == "MMType1") && fontFile is Reference -> {
                val fontProgram = referenceResolver.resolveReferenceToStream(fontFile)
                val data = fontProgram?.decodeEncodedStream()
                if (data is ByteArray) {
                    Type1Parser(data).getBuiltInEncoding(encodingArray)
                }
            }
            (fontType.value == "TrueType" || fontType.value == "CIDFontType2") && fontFile2 is Reference -> {
                val fontProgram = referenceResolver.resolveReferenceToStream(fontFile2)
                val data = fontProgram?.decodeEncodedStream()
                if (data is ByteArray) {
                    TTFParser(data).getBuiltInEncoding(encodingArray)
                }
            }
            fontFile3 is Stream -> {
                TODO("Get encoding from FontFile3")
            }
        }

    }
}