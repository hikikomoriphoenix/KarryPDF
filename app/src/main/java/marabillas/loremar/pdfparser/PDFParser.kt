package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.exceptions.NoDocumentException
import marabillas.loremar.pdfparser.exceptions.UnsupportedPDFElementException
import marabillas.loremar.pdfparser.objects.*
import java.io.RandomAccessFile

class PDFParser : ReferenceResolver {
    private var fileReader: PDFFileReader? = null
    private var objects: HashMap<String, XRefEntry>? = null

    fun loadDocument(file: RandomAccessFile) {
        val fileReader = PDFFileReader(file)
        this.fileReader = fileReader
        ObjectIdentifier.setReferenceResolver(this)

        objects = fileReader.getLastXRefData()

        // Process trailer
        val trailerEntries = fileReader.getTrailerEntries()
        size = (trailerEntries["Size"] as Numeric).value.toInt()
        documentCatalog = trailerEntries["Root"] as Dictionary
        info = (trailerEntries["Info"] as Dictionary?)
        if (trailerEntries["Encrypt"] != null) throw UnsupportedPDFElementException(
            "PDFParser library does not support encrypted pdf files yet."
        )
    }

    var size: Int? = null
        get() = field ?: throw NoDocumentException()
        private set

    var documentCatalog: Dictionary? = null
        get() = field ?: throw NoDocumentException()
        private set

    var info: Dictionary? = null
        private set

    override fun resolveReference(reference: Reference): PDFObject? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}