package marabillas.loremar.andpdf

import android.graphics.Typeface
import marabillas.loremar.andpdf.contents.ContentStreamParser
import marabillas.loremar.andpdf.contents.PageContent
import marabillas.loremar.andpdf.contents.PageContentAdapter
import marabillas.loremar.andpdf.contents.XObjectsResolver
import marabillas.loremar.andpdf.document.AndPDFContext
import marabillas.loremar.andpdf.document.GetPageContentsContext
import marabillas.loremar.andpdf.document.OutlineItem
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

class AndPDF(file: RandomAccessFile, password: String = "") {
    private val context = AndPDFContext()
    private val pages: ArrayList<Reference> = ArrayList()
    internal var size: Int? = null; get() = field ?: throw NoDocumentException()
    internal var documentCatalog: Dictionary? = null; get() = field ?: throw NoDocumentException()
    internal var info: Dictionary? = null

    init {
        TimeCounter.reset()

        if (BuildConfig.DEBUG && !forceHideLogs) {
            showAndPDFLogs = true
        }

        val fileReader = PDFFileReader(file)
        context.fileReader = fileReader

        parseCrossReferences(fileReader)

        parseTrailer(fileReader, password)

        setupPagesList()

        logd("AndPDF.loadDocument() -> ${TimeCounter.getTimeElapsed()} ms")
    }

    private fun parseCrossReferences(fileReader: PDFFileReader) {
        // Parse cross-reference table/streams which hold all references to document's objects.
        context.objects = if (fileReader.isLinearized(context)) {
            logd("Detected linearized PDF document")
            val startXRef = fileReader.getStartXRefPositionLinearized(context)
            fileReader.getXRefData(context, startXRef)
        } else {
            fileReader.getLastXRefData(context)
        }
    }

    private fun parseTrailer(fileReader: PDFFileReader, password: String) {
        // Parse trailer. Initialize decryptor if document is encrypted.
        val trailerEntries = fileReader.getTrailerEntries(context, true)
        size = (trailerEntries["Size"] as Numeric).value.toInt()
        documentCatalog = trailerEntries["Root"] as Dictionary
        info = trailerEntries["Info"] as Dictionary?
        val id = trailerEntries["ID"] as PDFArray?
        val encrypt = trailerEntries["Encrypt"] as Dictionary?
        if (encrypt is Dictionary) {
            context.decryptor = Decryptor(encrypt, id, password)
        }
    }

    private fun setupPagesList() {
        val pageTree = (
                documentCatalog?.resolveReferences()?.get("Pages")
                    ?: throw InvalidDocumentException("This document does not have a root page tree.")
                ) as Dictionary
        getPageTreeLeafNodes(pageTree)
    }

    private fun getPageTreeLeafNodes(pageTree: Dictionary) {
        // Traverse page tree.
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

    fun getPageContents(pageNum: Int): List<PageContent> {
        TimeCounter.reset()

        val getPageContentsContext = GetPageContentsContext(context)
        pages.forEach { it.context = getPageContentsContext }

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
            fonts[key] = Font(font, getPageContentsContext)
        }

        // Get XObjects
        val xObjectDic = resources["XObject"] as Dictionary?
        var xObjs = HashMap<String, Stream>()
        xObjectDic?.let {
            xObjs = getXObjects(getPageContentsContext, it)
        }

        val contents = pageDic["Contents"] ?: throw InvalidDocumentException("Missing Contents entry in Page object.")
        logd("Preparations -> ${TimeCounter.getTimeElapsed()} ms")
        return if (contents is PDFArray) {
            contents.asSequence()
                .filterNotNull()
                .forEach { content ->
                    val pageContent = parseContent(getPageContentsContext, content, fonts, xObjs)
                    contentsList.addAll(pageContent)
                }
            contentsList
        } else {
            TimeCounter.reset()
            return parseContent(getPageContentsContext, contents, fonts, xObjs)
        }

    }

    private fun parseContent(
        context: AndPDFContext,
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

    private fun getXObjects(
        context: AndPDFContext,
        xObjectDic: Dictionary
    ): HashMap<String, Stream> {
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

    fun getDocumentOutline(): List<OutlineItem> {
        documentCatalog?.resolveReferences()
        val outlineDic = documentCatalog?.get("Outlines") as Dictionary?
        return outlineDic?.let { getOutlineItems(it) } ?: listOf()
    }

    private fun getOutlineItems(outlineDic: Dictionary): List<OutlineItem> {
        val first = outlineDic["First"]
        val last = outlineDic["Last"]
        val list = mutableListOf<OutlineItem>()

        if (first is Reference && last is Reference) {
            var curr = first
            while (curr is Reference) {
                val currDic = curr.resolve() as Dictionary?
                val subItems = currDic?.let { getOutlineItems(it) }

                val next = currDic?.get("Next")
                currDic?.resolveReferences()

                val obj = curr.obj
                val gen = curr.gen

                val title = (currDic?.get("Title") as PDFString?)?.run {
                    context.decryptor?.decrypt(this, obj, gen) ?: textString
                }

                val dest = currDic?.get("Dest")
                val a = currDic?.get("A")
                var pageIndex = -1
                if (dest is PDFArray || dest is Name)
                    pageIndex = dest.getDestinationPageIndex()
                else if (a is Dictionary) {
                    a.resolveReferences()
                    val actionType = a["S"]
                    if (actionType is Name && actionType.value == "GoTo")
                        pageIndex = a["D"]?.getDestinationPageIndex() ?: -1
                }

                val item = OutlineItem(
                    title = title ?: "",
                    pageIndex = pageIndex,
                    subItems = subItems ?: listOf()
                )
                list.add(item)

                if (curr.toString() == last.toString())
                    break

                curr = next
            }
        }

        return list
    }

    private fun PDFObject.getDestinationPageIndex(): Int {
        if (this is PDFArray) {
            return getPageIndex()
        } else if (this is Name) {
            val names = documentCatalog?.get("Names")
            val dests = documentCatalog?.get("Dests")
            if (names is Dictionary) {
                // TODO("Requires traversing name tree to get destinations")
            } else if (dests is Dictionary) {
                dests.resolveReferences()
                val d = dests[value]
                if (d is PDFArray) {
                    return d.getPageIndex()
                } else if (d is Dictionary) {
                    d.resolveReferences()
                    val dValue = d["D"]
                    if (dValue is PDFArray)
                        return dValue.getPageIndex()
                }
            }
        }
        return -1
    }

    private fun PDFArray.getPageIndex(): Int {
        val page = get(0)
        return if (page is Reference) {
            pages.indexOfFirst { it.toString() == page.toString() }
        } else
            -1
    }

    private fun parseStructureTree() {
        documentCatalog?.resolveReferences()
        val structTreeRoot = documentCatalog?.get("StructTreeRoot")
        if (structTreeRoot is Dictionary) {
            TODO()
        }
    }
}