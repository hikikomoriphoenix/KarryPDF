package marabillas.loremar.pdfparser

import android.graphics.Typeface
import marabillas.loremar.pdfparser.contents.ContentStreamParser
import marabillas.loremar.pdfparser.contents.PageContent
import marabillas.loremar.pdfparser.contents.PageContentAdapter
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

    fun loadDocument(file: RandomAccessFile): PDFParser {
        val tCtr = TimeCounter()

        val fileReader = PDFFileReader(file)
        this.fileReader = fileReader
        ObjectIdentifier.referenceResolver = ReferenceResolverImpl()

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

        println("PDFParser.loadDocument() -> ${tCtr.getTimeElapsed()} ms")
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
    }

    private fun resolveReferenceToStream(reference: Reference): Stream? {
        val fileReader = fileReader ?: throw NoDocumentException()
        val objects = this.objects ?: throw NoDocumentException()
        val obj = objects["${reference.obj} ${reference.gen}"]
        val pos = obj?.pos ?: return null
        return fileReader.getStream(pos)
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
        val tCtr = TimeCounter()

        val contentsList = ArrayList<PageContent>()
        val pageDic = pages[pageNum].resolve() as Dictionary

        // Get all fonts required for this page
        pageDic.resolveReferences()
        val resources = pageDic["Resources"] as Dictionary
        resources.resolveReferences()
        val fontsDic = resources["Font"] as Dictionary?
        fontsDic?.resolveReferences()
        var pageFonts = HashMap<String, Typeface>()
        var cmaps = HashMap<String, CMap>()
        fontsDic?.let {
            pageFonts = getPageFonts(it)
            cmaps = getCMaps(it)
        }

        val contents = pageDic["Contents"] ?: throw InvalidDocumentException("Missing Contents entry in Page object.")
        return if (contents is PDFArray) {
            contents.asSequence()
                .filterNotNull()
                .forEach { content ->
                    val pageContent = parseContent(content, pageFonts, cmaps)
                    contentsList.addAll(pageContent)
                }
            println("PDFParser.getPageContents() -> ${tCtr.getTimeElapsed()} ms")
            contentsList
        } else {
            println("PDFParser.getPageContents() -> ${tCtr.getTimeElapsed()} ms")
            parseContent(contents, pageFonts, cmaps)
        }

    }

    private fun parseContent(
        content: PDFObject,
        pageFonts: HashMap<String, Typeface>,
        cmaps: HashMap<String, CMap>
    ): ArrayList<PageContent> {
        val ref = content as Reference
        val stream = resolveReferenceToStream(ref)
        stream?.let {
            val data = it.decodeEncodedStream()
            val pageObjs = ContentStreamParser().parse(String(data))
            FontDecoder(pageObjs, cmaps).decodeEncoded()
            return PageContentAdapter(pageObjs, pageFonts).getPageContents()
        }
        return ArrayList()
    }

    internal fun getPageFonts(fontsDic: Dictionary): HashMap<String, Typeface> {
        fontsDic.resolveReferences()
        val fKeys = fontsDic.getKeys()
        val idier = FontIdentifier()
        val fonts = HashMap<String, Typeface>()
        fKeys.forEach { key ->
            val font = fontsDic[key] as Dictionary
            val basefont = font["BaseFont"] as Name?
            var typeface = FontMappings[FontName.DEFAULT]
            basefont?.value?.let {
                typeface = idier.identifyFont(it)
            }
            fonts[key] = typeface
        }
        return fonts
    }

    private fun getCMaps(fontsDic: Dictionary): HashMap<String, CMap> {
        fontsDic.resolveReferences()
        val fkeys = fontsDic.getKeys()
        val cmaps = HashMap<String, CMap>()
        fkeys.forEach foreachKey@{ key ->
            val font = fontsDic[key] as Dictionary

            val toUnicode = font["ToUnicode"]
            if (toUnicode is Reference) {
                val stream = resolveReferenceToStream(toUnicode)
                val b = stream?.decodeEncodedStream()
                b?.let {
                    val cmap = ToUnicodeCMap(String(b)).parse()
                    cmaps.put(key, cmap)
                    return@foreachKey
                }
            }

            // TODO Get Cmap from Encoding entry.
        }
        return cmaps
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