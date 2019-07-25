package marabillas.loremar.andpdf

import android.graphics.Typeface
import marabillas.loremar.andpdf.contents.ContentStreamParser
import marabillas.loremar.andpdf.contents.PageContent
import marabillas.loremar.andpdf.contents.PageContentAdapter
import marabillas.loremar.andpdf.contents.XObjectsResolver
import marabillas.loremar.andpdf.document.AndPDFContext
import marabillas.loremar.andpdf.document.PDFFileReader
import marabillas.loremar.andpdf.document.TopDownParser
import marabillas.loremar.andpdf.document.XRefEntry
import marabillas.loremar.andpdf.encryption.Decryptor
import marabillas.loremar.andpdf.exceptions.*
import marabillas.loremar.andpdf.font.Font
import marabillas.loremar.andpdf.font.FontDecoder
import marabillas.loremar.andpdf.font.FontMappings
import marabillas.loremar.andpdf.font.FontName
import marabillas.loremar.andpdf.objects.*
import marabillas.loremar.andpdf.utils.*
import marabillas.loremar.andpdf.utils.exts.appendBytes
import marabillas.loremar.andpdf.utils.exts.containedEqualsWith
import java.io.RandomAccessFile

class AndPDF(private val file: RandomAccessFile, password: String = "") {
    private val context = AndPDFContext()
    internal val pages: ArrayList<Reference> = ArrayList()
    internal var size: Int? = null; get() = field ?: throw NoDocumentException()
    internal var documentCatalog: Dictionary? = null; get() = field ?: throw NoDocumentException()
    internal var info: Dictionary? = null

    private var topDownReferences: HashMap<String, XRefEntry>? = null
        get() {
            if (field == null) {
                field = TopDownParser(context, file).parseObjects()
            }
            return field
        }

    init {
        TimeCounter.reset()
        if (BuildConfig.DEBUG && !forceHideLogs) {
            showAndPDFLogs = true
        }

        context.referenceResolver = ReferenceResolverImpl()
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

    private inner class ReferenceResolverImpl : ReferenceResolver {
        private val stringBuilder = StringBuilder()

        override fun resolveReference(reference: Reference, checkTopDownReferences: Boolean): PDFObject? {
            val fileReader = context.fileReader ?: throw NoDocumentException()
            val objects = context.objects ?: throw NoDocumentException()
            var objEntry = objects["${reference.obj} ${reference.gen}"] ?: return null

            if (objEntry.compressed) {
                var objStmEntry = objects["${objEntry.objStm} 0"]
                if ((objStmEntry == null || objStmEntry.pos < 0L) && checkTopDownReferences) {
                    objStmEntry = topDownReferences?.get("${objEntry.objStm} 0")
                }

                return if (objStmEntry != null) {
                    val objStm = try {
                        fileReader.getObjectStream(objStmEntry.pos, Reference(context, objEntry.objStm, 0))
                    } catch (e: IndirectObjectMismatchException) {
                        if (!checkTopDownReferences) return null
                        val pos = topDownReferences?.get("${objEntry.objStm} 0")?.pos
                        if (pos != null) {
                            fileReader.getObjectStream(pos)
                        } else {
                            return null
                        }
                    }

                    if (objEntry.index != -1) {
                        val objBytes = objStm.extractObjectBytes(objEntry.index)
                        stringBuilder.clear().appendBytes(objBytes ?: byteArrayOf())
                            .toPDFObject(context, reference.obj, reference.gen)
                    } else {
                        val objBytes = objStm.extractObjectBytesGivenObjectNum(reference.obj)
                        stringBuilder.clear().appendBytes(objBytes ?: byteArrayOf())
                            .toPDFObject(context, reference.obj, reference.gen)
                    }
                } else {
                    null
                }
            } else {
                if (objEntry.pos < 0L && checkTopDownReferences) {
                    objEntry = topDownReferences?.get("${objEntry.obj} ${objEntry.gen}") ?: return null
                }

                val content = try {
                    fileReader.getIndirectObject(objEntry.pos, reference).extractContent()
                } catch (e: IndirectObjectMismatchException) {
                    if (checkTopDownReferences) {

                        val pos = topDownReferences?.get("${objEntry.obj} ${objEntry.gen}")?.pos
                        if (pos != null) {
                            fileReader.getIndirectObject(pos, reference).extractContent()
                        } else {
                            StringBuilder()
                        }
                    } else {
                        StringBuilder()
                    }
                }
                if (content.isEmpty() || content.containedEqualsWith('n', 'u', 'l', 'l')) return null
                return content.toPDFObject(context, reference.obj, reference.gen, false) ?: reference
            }
        }

        override fun resolveReferenceToStream(reference: Reference): Stream? {
            val fileReader = context.fileReader ?: throw NoDocumentException()
            val objects = context.objects ?: throw NoDocumentException()
            val obj = objects["${reference.obj} ${reference.gen}"]
            return if (obj != null) {
                if (obj.pos < 0) {
                    val objEntry = topDownReferences?.get("${reference.obj} ${reference.gen}")
                    objEntry?.pos?.let { fileReader.getStream(it, reference) }
                } else {
                    try {
                        fileReader.getStream(obj.pos, reference)
                    } catch (e: IndirectObjectMismatchException) {
                        val pos = topDownReferences?.get("${reference.obj} ${reference.gen}")?.pos
                        if (pos != null) {
                            fileReader.getStream(pos)
                        } else {
                            return null
                        }
                    }
                }
            } else {
                null
            }
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
        logd("Getting page $pageNum")
        TimeCounter.reset()

        val contentsList = ArrayList<PageContent>()
        val pageDic = pages[pageNum].resolve() as Dictionary

        // Attempt to get ActualText
        val structParents = pageDic["StructParents"]
        if (structParents is Numeric) {
            logd("StructParents=${structParents.value}")
        } else {
            logd("No StructParents")
        }

        pageDic.resolveReferences()
        val resources = pageDic["Resources"] as Dictionary
        resources.resolveReferences()

        // Get all fonts and CMaps required for this page
        val fontsDic = resources["Font"] as Dictionary?
        fontsDic?.resolveReferences()
        val fonts = HashMap<String, Font>()
        val fKeys = fontsDic?.getKeys()
        fKeys?.forEach { key ->
            val font = fontsDic[key] as Dictionary
            fonts[key] = Font(font, context.referenceResolver ?: throw NoReferenceResolverException())
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
            val stream = context.referenceResolver?.resolveReferenceToStream(ref)
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
            val xObj = context.referenceResolver?.resolveReferenceToStream(xObjRef)
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