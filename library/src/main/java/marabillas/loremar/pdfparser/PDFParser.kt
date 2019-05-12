package marabillas.loremar.pdfparser

import android.graphics.Typeface
import android.support.v4.util.SparseArrayCompat
import android.util.SparseArray
import marabillas.loremar.pdfparser.contents.ContentStreamParser
import marabillas.loremar.pdfparser.contents.PageContent
import marabillas.loremar.pdfparser.contents.PageContentAdapter
import marabillas.loremar.pdfparser.contents.XObjectsResolver
import marabillas.loremar.pdfparser.exceptions.InvalidDocumentException
import marabillas.loremar.pdfparser.exceptions.NoDocumentException
import marabillas.loremar.pdfparser.exceptions.UnsupportedPDFElementException
import marabillas.loremar.pdfparser.font.*
import marabillas.loremar.pdfparser.objects.*
import java.io.RandomAccessFile

class PDFParser {
    private var fileReader: PDFFileReader? = null
    private var objects: HashMap<String, XRefEntry>? = null
    internal val pages: ArrayList<Reference> = ArrayList()
    private val referenceResolver = ReferenceResolverImpl()

    fun loadDocument(file: RandomAccessFile): PDFParser {
        TimeCounter.reset()

        val fileReader = PDFFileReader(file)
        this.fileReader = fileReader
        ObjectIdentifier.referenceResolver = referenceResolver

        objects = fileReader.getLastXRefData()

        // Process trailer
        val trailerEntries = fileReader.getTrailerEntries(true)
        size = (trailerEntries["Size"] as Numeric).value.toInt()
        documentCatalog = trailerEntries["Root"] as Dictionary
        info = trailerEntries["Info"] as Dictionary?
        if (trailerEntries["Encrypt"] != null) throw UnsupportedPDFElementException(
            "PDFParser library does not support encrypted pdf files yet."
        )

        val pageTree = (documentCatalog?.resolveReferences()?.get("Pages") ?: throw InvalidDocumentException(
            "This document does not have a root page tree."
        )) as Dictionary
        getPageTreeLeafNodes(pageTree)

        println("PDFParser.loadDocument() -> ${TimeCounter.getTimeElapsed()} ms")
        return this
    }

    internal var size: Int? = null
        get() = field ?: throw NoDocumentException()

    internal var documentCatalog: Dictionary? = null
        get() = field ?: throw NoDocumentException()

    internal var info: Dictionary? = null

    private inner class ReferenceResolverImpl : ReferenceResolver {
        override fun resolveReference(reference: Reference): PDFObject? {
            val fileReader = fileReader ?: throw NoDocumentException()
            val objects = objects ?: throw NoDocumentException()
            val obj = objects["${reference.obj} ${reference.gen}"] ?: return null

            if (obj.compressed) {
                val objStmEntry = objects["${obj.objStm} 0"]
                val objStm = objStmEntry?.pos?.let { fileReader.getObjectStream(it) }
                return objStm?.getObject(obj.index)
            } else {
                val content = fileReader.getIndirectObject(obj.pos).extractContent()
                if (content == "" || content == "null") return null
                return content.toPDFObject(false) ?: reference
            }
        }

        override fun resolveReferenceToStream(reference: Reference): Stream? {
            val fileReader = fileReader ?: throw NoDocumentException()
            val objects = objects ?: throw NoDocumentException()
            val obj = objects["${reference.obj} ${reference.gen}"]
            val pos = obj?.pos ?: return null
            return fileReader.getStream(pos)
        }
    }

    private fun getPageTreeLeafNodes(pageTree: Dictionary) {
        pageTree.resolveReferences()
        val arr = pageTree["Kids"] as PDFArray
        arr.forEach {
            val node = it as Reference
            val nodeDic = node.resolve() as Dictionary
            val type = nodeDic["Type"] as Name
            if (type.value == "Pages")
                getPageTreeLeafNodes(nodeDic)
            else
                pages.add(node)
        }
    }

    fun getPageContents(pageNum: Int): ArrayList<PageContent> {
        println("Getting page $pageNum")
        TimeCounter.reset()

        val contentsList = ArrayList<PageContent>()
        val pageDic = pages[pageNum].resolve() as Dictionary

        pageDic.resolveReferences()
        val resources = pageDic["Resources"] as Dictionary
        resources.resolveReferences()

        // Get all fonts and CMaps required for this page
        val fontsDic = resources["Font"] as Dictionary?
        fontsDic?.resolveReferences()
        var pageFonts = SparseArray<Typeface>()
        var cmaps = SparseArray<CMap>()
        var characterWidths = SparseArrayCompat<FloatArray>()
        var fontFirstChars = SparseArrayCompat<Int>()
        fontsDic?.let {
            pageFonts = getPageFonts(it)
            cmaps = getCMaps(it)
            characterWidths = getCharacterWidths(it)
            fontFirstChars = getFontFirstChars(it)
        }

        // Get XObjects
        val xObjectDic = resources["XObject"] as Dictionary?
        var xObjs = HashMap<String, Stream>()
        xObjectDic?.let {
            xObjs = getXObjects(it)
        }

        val contents = pageDic["Contents"] ?: throw InvalidDocumentException("Missing Contents entry in Page object.")
        println("Preparations -> ${TimeCounter.getTimeElapsed()} ms")
        return if (contents is PDFArray) {
            contents.asSequence()
                .filterNotNull()
                .forEach { content ->
                    val pageContent = parseContent(content, pageFonts, cmaps, xObjs, characterWidths, fontFirstChars)
                    contentsList.addAll(pageContent)
                }
            contentsList
        } else {
            TimeCounter.reset()
            return parseContent(contents, pageFonts, cmaps, xObjs, characterWidths, fontFirstChars)
        }

    }

    private fun parseContent(
        content: PDFObject,
        pageFonts: SparseArray<Typeface>,
        cmaps: SparseArray<CMap>,
        xObjects: HashMap<String, Stream>,
        characterWidths: SparseArrayCompat<FloatArray>,
        firstChars: SparseArrayCompat<Int>
    ): ArrayList<PageContent> {
        TimeCounter.reset()
        val ref = content as Reference
        val stream = referenceResolver.resolveReferenceToStream(ref)
        stream?.let {
            TimeCounter.reset()
            val data = it.decodeEncodedStream()
            println("Stream.decodeEncodedStream -> ${TimeCounter.getTimeElapsed()} ms")

            TimeCounter.reset()
            val pageObjs = ContentStreamParser().parse(String(data))
            println("ContentStreamParser.parse -> ${TimeCounter.getTimeElapsed()} ms")

            TimeCounter.reset()
            FontDecoder(pageObjs, cmaps).decodeEncoded()
            println("FontDecoder.decodeEncoded -> ${TimeCounter.getTimeElapsed()} ms")

            TimeCounter.reset()
            XObjectsResolver(pageObjs, xObjects).resolve()
            println("XObjectsResolver.resolve -> ${TimeCounter.getTimeElapsed()} ms")

            return PageContentAdapter(pageObjs, pageFonts, characterWidths, firstChars).getPageContents()
        }
        return ArrayList()
    }

    internal fun getPageFonts(fontsDic: Dictionary): SparseArray<Typeface> {
        val fKeys = fontsDic.getKeys()
        val idier = FontIdentifier()
        val fonts = SparseArray<Typeface>()
        fKeys.forEach { key ->
            val font = fontsDic[key] as Dictionary
            val basefont = font["BaseFont"] as Name?
            var typeface = FontMappings[FontName.DEFAULT]
            basefont?.value?.let {
                typeface = idier.identifyFont(it)
            }
            fonts.put(key.substring(1, key.length).toInt(), typeface)
        }
        return fonts
    }

    private fun getCMaps(fontsDic: Dictionary): SparseArray<CMap> {
        val fkeys = fontsDic.getKeys()
        val cmaps = SparseArray<CMap>()
        fkeys.forEach foreachKey@{ key ->
            val font = fontsDic[key] as Dictionary

            val toUnicode = font["ToUnicode"]
            if (toUnicode is Reference) {
                val stream = referenceResolver.resolveReferenceToStream(toUnicode)
                val b = stream?.decodeEncodedStream()
                b?.let {
                    val cmap = ToUnicodeCMap(String(b)).parse()
                    cmaps.put(key.substring(1, key.length).toInt(), cmap)
                    return@foreachKey
                }
            }

            // TODO Get Cmap from Encoding entry.
            val encoding = font["Encoding"]
            if (
                (encoding is Name && !encoding.value.endsWith("Encoding"))
                || (encoding != null && encoding !is Name)
            ) {
                throw UnsupportedPDFElementException(
                    "Decoding of characters with the given type of encoding is not yet" +
                            "supported."
                )
            }
        }
        return cmaps
    }

    private fun getCharacterWidths(fontsDic: Dictionary): SparseArrayCompat<FloatArray> {
        val fKeys = fontsDic.getKeys()
        val charWdths = SparseArrayCompat<FloatArray>()
        fKeys.forEach { key ->
            val font = fontsDic[key] as Dictionary
            font.resolveReferences()
            val widths = font["Widths"]
            if (widths is PDFArray) {
                val widthsArray = FloatArray(widths.count())
                widths.forEachIndexed { i, width ->
                    widthsArray[i] = (width as Numeric).value.toFloat()
                }
                charWdths.put(key.substring(1, key.length).toInt(), widthsArray)
            }
        }
        return charWdths
    }

    private fun getFontFirstChars(fontsDic: Dictionary): SparseArrayCompat<Int> {
        val fKeys = fontsDic.getKeys()
        val firstChars = SparseArrayCompat<Int>()
        fKeys.forEach { key ->
            val font = fontsDic[key] as Dictionary
            font.resolveReferences()
            val firstChar = font["FirstChar"]
            if (firstChar is Numeric) {
                firstChars.put(key.substring(1, key.length).toInt(), firstChar.value.toInt())
            }
        }
        return firstChars
    }

    private fun getXObjects(xObjectDic: Dictionary): HashMap<String, Stream> {
        val xKeys = xObjectDic.getKeys()
        val xObjMap = HashMap<String, Stream>()
        xKeys.forEach { xKey ->
            val xObjRef = xObjectDic[xKey] as Reference
            val xObj = referenceResolver.resolveReferenceToStream(xObjRef)
            xObj?.let {
                xObjMap[xKey] = it
            }
        }
        return xObjMap
    }

    /**
     * Set the Typeface to use when a specific font is used in the PDF file. Fonts are initially set to Android SDK's
     * default Typefaces for Serif, San-serif, and Monospace.
     *
     * @param fontName A value from the FontName enum class representing the font you want to set the Typeface for.
     * @param typeface The Typeface you want to set for a specific font.
     */
    fun setPreferredTypeface(fontName: FontName, typeface: Typeface) {
        FontMappings[fontName] = typeface
    }
}