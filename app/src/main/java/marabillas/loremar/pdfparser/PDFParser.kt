package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.contents.ContentStreamParser
import marabillas.loremar.pdfparser.contents.PageContent
import marabillas.loremar.pdfparser.exceptions.InvalidDocumentException
import marabillas.loremar.pdfparser.exceptions.NoDocumentException
import marabillas.loremar.pdfparser.exceptions.UnsupportedPDFElementException
import marabillas.loremar.pdfparser.objects.*
import java.io.RandomAccessFile

class PDFParser {
    private var fileReader: PDFFileReader? = null
    private var objects: HashMap<String, XRefEntry>? = null
    private var pages: ArrayList<Reference> = ArrayList()

    fun loadDocument(file: RandomAccessFile): PDFParser {
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
        val contentsList = ArrayList<PageContent>()
        val pageDic = pages[pageNum].resolve() as Dictionary
        val contents = pageDic["Contents"] ?: throw InvalidDocumentException("Missing Contents entry in Page object.")
        return if (contents is PDFArray) {
            contents.asSequence()
                .filterNotNull()
                .forEach { content ->
                    val pageContent = parseContent(content)
                    contentsList.addAll(pageContent)
                }
            contentsList
        } else {
            parseContent(contents)
        }
    }

    private fun parseContent(content: PDFObject): ArrayList<PageContent> {
        val fileReader = fileReader ?: throw NoDocumentException()
        val ref = content as Reference
        val objects = this.objects ?: throw NoDocumentException()
        val obj = objects["${ref.obj} ${ref.gen}"]
        obj?.pos?.let {
            val stream = fileReader.getStream(it)
            val data = stream.decodeEncodedStream()
            return ContentStreamParser().parse(String(data))
        }
        return ArrayList()
    }
}