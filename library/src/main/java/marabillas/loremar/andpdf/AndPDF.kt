package marabillas.loremar.andpdf

import android.graphics.Typeface
import marabillas.loremar.andpdf.contents.ContentStreamParser
import marabillas.loremar.andpdf.contents.PageContent
import marabillas.loremar.andpdf.contents.PageContentAdapter
import marabillas.loremar.andpdf.contents.XObjectsResolver
import marabillas.loremar.andpdf.document.AndPDFContext
import marabillas.loremar.andpdf.document.PDFFileReader
import marabillas.loremar.andpdf.encryption.Decryptor
import marabillas.loremar.andpdf.exceptions.InvalidDocumentException
import marabillas.loremar.andpdf.exceptions.InvalidStreamException
import marabillas.loremar.andpdf.exceptions.NoDocumentException
import marabillas.loremar.andpdf.font.Font
import marabillas.loremar.andpdf.font.FontDecoder
import marabillas.loremar.andpdf.font.FontMappings
import marabillas.loremar.andpdf.font.FontName
import marabillas.loremar.andpdf.objects.*
import marabillas.loremar.andpdf.utils.*
import java.io.RandomAccessFile

class AndPDF(private val file: RandomAccessFile, password: String = "") {
    private val context = AndPDFContext()
    internal val pages: ArrayList<Reference> = ArrayList()
    internal var size: Int? = null; get() = field ?: throw NoDocumentException()
    internal var documentCatalog: Dictionary? = null; get() = field ?: throw NoDocumentException()
    internal var info: Dictionary? = null

    init {
        TimeCounter.reset()
        if (BuildConfig.DEBUG && !forceHideLogs) {
            showAndPDFLogs = true
        }

        val fileReader = PDFFileReader(context, file)
        context.fileReader = fileReader
        context.objects = if (fileReader.isLinearized()) {
            logd("Detected linearized PDF document")
            val startXRef = fileReader.getStartXRefPositionLinearized()
            fileReader.getXRefData(startXRef)
        } else {
            fileReader.getLastXRefData()
        }

        // Process trailer
        val trailerEntries = fileReader.getTrailerEntries(true)
        size = (trailerEntries["Size"] as Numeric).value.toInt()
        documentCatalog = trailerEntries["Root"] as Dictionary
        info = trailerEntries["Info"] as Dictionary?
        val id = trailerEntries["ID"] as PDFArray?
        val encrypt = trailerEntries["Encrypt"] as Dictionary?
        if (encrypt is Dictionary) {
            context.decryptor = Decryptor(encrypt, id, password)
        }

        val pageTree = (documentCatalog?.resolveReferences()?.get("Pages") ?: throw InvalidDocumentException(
            "This document does not have a root page tree."
        )) as Dictionary
        getPageTreeLeafNodes(pageTree)

        logd("AndPDF.loadDocument() -> ${TimeCounter.getTimeElapsed()} ms")
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
        logd("Getting page $pageNum")
        TimeCounter.reset()

        val contentsList = ArrayList<PageContent>()
        val pageDic = pages[pageNum].resolve() as Dictionary
        pageDic.resolveReferences()

        // Attempt to get ActualText
        val structParents = pageDic["StructParents"]
        if (structParents is Numeric) {
            logd("StructParents=${structParents.value}")
        } else {
            logd("No StructParents")
        }

        val resources = try {
            (pageDic["Resources"] as Dictionary?) ?: getPageResourcesFromAncestors(pageDic)
        } catch (e: InvalidDocumentException) {
            loge("Blank Page")
            return ArrayList()
        }
        resources.resolveReferences()

        // Get all fonts and CMaps required for this page
        val fontsDic = resources["Font"] as Dictionary?
        fontsDic?.resolveReferences()
        val fonts = HashMap<String, Font>()
        val fKeys = fontsDic?.getKeys()
        fKeys?.forEach { key ->
            val font = fontsDic[key] as Dictionary
            fonts[key] = Font(font, context)
        }

        // Get XObjects
        val xObjectDic = resources["XObject"] as Dictionary?
        var xObjs = HashMap<String, Stream>()
        xObjectDic?.let {
            xObjs = getXObjects(it)
        }

        val contents = pageDic["Contents"] ?: throw InvalidDocumentException("Missing Contents entry in Page object.")
        logd("Preparations -> ${TimeCounter.getTimeElapsed()} ms")
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
        fonts: HashMap<String, Font>,
        xObjects: HashMap<String, Stream>
    ): ArrayList<PageContent> {
        try {
            TimeCounter.reset()
            val ref = content as Reference
            val stream = context.resolveReferenceToStream(ref)
            stream?.let {
                TimeCounter.reset()
                val data = it.decodeEncodedStream()
                logd("Stream.decodeEncodedStream -> ${TimeCounter.getTimeElapsed()} ms")

                TimeCounter.reset()
                val pageObjs = ContentStreamParser(context).parse(String(data), ref.obj, ref.gen)
                logd("ContentStreamParser.parse -> ${TimeCounter.getTimeElapsed()} ms")

                TimeCounter.reset()
                FontDecoder(context, pageObjs, fonts).decodeEncoded()
                logd("FontDecoder.decodeEncoded -> ${TimeCounter.getTimeElapsed()} ms")

                TimeCounter.reset()
                XObjectsResolver(pageObjs, xObjects).resolve()
                logd("XObjectsResolver.resolve -> ${TimeCounter.getTimeElapsed()} ms")

                return PageContentAdapter(pageObjs, fonts).getPageContents()
            }
            return ArrayList()
        } catch (e: InvalidStreamException) {
            loge("Unable to parse content due to InvalidStreamException", e)
            return ArrayList()
        }
    }

    private fun getXObjects(xObjectDic: Dictionary): HashMap<String, Stream> {
        val xKeys = xObjectDic.getKeys()
        val xObjMap = HashMap<String, Stream>()
        xKeys.forEach { xKey ->
            val xObjRef = xObjectDic[xKey] as Reference
            val xObj = context.resolveReferenceToStream(xObjRef)
            xObj?.let {
                xObjMap[xKey] = it
            }
        }
        return xObjMap
    }

    private fun getPageResourcesFromAncestors(pageDic: Dictionary): Dictionary {
        val parent = pageDic["Parent"]
        if (parent is Dictionary) {
            parent.resolveReferences()
            return parent["Resources"] as Dictionary? ?: getPageResourcesFromAncestors(parent)
        } else {
            throw InvalidDocumentException("Page dictionary is missing a Resources Entry")
        }
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