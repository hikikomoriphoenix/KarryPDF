package marabillas.loremar.andpdf

import android.graphics.Typeface
import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.andpdf.contents.ContentStreamParser
import marabillas.loremar.andpdf.contents.PageContent
import marabillas.loremar.andpdf.contents.PageContentAdapter
import marabillas.loremar.andpdf.contents.XObjectsResolver
import marabillas.loremar.andpdf.exceptions.InvalidDocumentException
import marabillas.loremar.andpdf.exceptions.NoDocumentException
import marabillas.loremar.andpdf.exceptions.UnsupportedPDFElementException
import marabillas.loremar.andpdf.font.Font
import marabillas.loremar.andpdf.font.FontDecoder
import marabillas.loremar.andpdf.font.FontMappings
import marabillas.loremar.andpdf.font.FontName
import marabillas.loremar.andpdf.objects.*
import marabillas.loremar.andpdf.utils.TimeCounter
import java.io.RandomAccessFile

class AndPDF {
    private var fileReader: PDFFileReader? = null
    private var objects: HashMap<String, XRefEntry>? = null
    internal val pages: ArrayList<Reference> = ArrayList()
    private val referenceResolver = ReferenceResolverImpl()

    fun loadDocument(file: RandomAccessFile): AndPDF {
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
            "AndPDF library does not support encrypted pdf files yet."
        )

        val pageTree = (documentCatalog?.resolveReferences()?.get("Pages") ?: throw InvalidDocumentException(
            "This document does not have a root page tree."
        )) as Dictionary
        getPageTreeLeafNodes(pageTree)

        println("AndPDF.loadDocument() -> ${TimeCounter.getTimeElapsed()} ms")
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

        // Attempt to get ActualText
        val structParents = pageDic["StructParents"]
        if (structParents is Numeric) {
            println("StructParents=${structParents.value}")
        } else {
            println("No StructParents")
        }

        pageDic.resolveReferences()
        val resources = pageDic["Resources"] as Dictionary
        resources.resolveReferences()

        // Get all fonts and CMaps required for this page
        val fontsDic = resources["Font"] as Dictionary?
        fontsDic?.resolveReferences()
        val fonts = SparseArrayCompat<Font>()
        val fKeys = fontsDic?.getKeys()
        fKeys?.forEach { key ->
            val font = fontsDic[key] as Dictionary
            fonts.put(key.substring(1, key.length).toInt(), Font(font, referenceResolver))
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
                    val pageContent = parseContent(content, fonts, xObjs)
                    contentsList.addAll(pageContent)
                }
            contentsList
        } else {
            TimeCounter.reset()
            return parseContent(contents, fonts, xObjs)
        }

    }

    private fun parseContent(
        content: PDFObject,
        fonts: SparseArrayCompat<Font>,
        xObjects: HashMap<String, Stream>
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
            FontDecoder(pageObjs, fonts).decodeEncoded()
            println("FontDecoder.decodeEncoded -> ${TimeCounter.getTimeElapsed()} ms")

            TimeCounter.reset()
            XObjectsResolver(pageObjs, xObjects).resolve()
            println("XObjectsResolver.resolve -> ${TimeCounter.getTimeElapsed()} ms")

            return PageContentAdapter(pageObjs, fonts).getPageContents()
        }
        return ArrayList()
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

    fun getTotalPages(): Int {
        return pages.size
    }

    private fun parseStructureTree() {
        documentCatalog?.resolveReferences()
        val structTreeRoot = documentCatalog?.get("StructTreeRoot")
        if (structTreeRoot is Dictionary) {

        }
    }
}