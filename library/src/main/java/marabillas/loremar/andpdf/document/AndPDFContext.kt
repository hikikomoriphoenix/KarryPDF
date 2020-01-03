package marabillas.loremar.andpdf.document

import marabillas.loremar.andpdf.encryption.Decryptor
import marabillas.loremar.andpdf.exceptions.IndirectObjectMismatchException
import marabillas.loremar.andpdf.exceptions.NoDocumentException
import marabillas.loremar.andpdf.objects.*
import marabillas.loremar.andpdf.utils.exts.appendBytes
import marabillas.loremar.andpdf.utils.exts.containedEqualsWith

internal open class AndPDFContext : ReferenceResolver {
    var fileReader: PDFFileReader? = null
    var objects: HashMap<String, XRefEntry>? = null
    var decryptor: Decryptor? = null
    var session = object : Session() {}
        protected set

    init {
        if (javaClass.superclass != AndPDFContext::class.java) {
            PDFObjectAdapter.notifyNewSession(session)
            PDFFileReader.notifyNewSession(session)
        }
    }

    private val stringBuilder = StringBuilder()

    open var topDownReferences: HashMap<String, XRefEntry>?
        get() {
            if (topDownReferencesAvailable)
                return _topDownReferences

            return fileReader?.file?.let {
                synchronized(it) {
                    if (_topDownReferences == null) {
                        _topDownReferences =
                            TopDownParser(this, it).parseObjects()
                        _topDownReferences
                    } else {
                        _topDownReferences
                    }
                }
            }
        }
        set(value) {
            _topDownReferences = value
        }

    val topDownReferencesAvailable: Boolean get() = _topDownReferences != null

    private var _topDownReferences: HashMap<String, XRefEntry>? = null

    override fun resolveReference(
        reference: Reference,
        checkTopDownReferences: Boolean
    ): PDFObject? {
        val fileReader = fileReader ?: throw NoDocumentException()
        val objects = objects ?: throw NoDocumentException()
        var objEntry = objects["${reference.obj} ${reference.gen}"] ?: return null

        if (objEntry.compressed) {
            var objStmEntry = objects["${objEntry.objStm} 0"]
            if ((objStmEntry == null || objStmEntry.pos < 0L) && checkTopDownReferences) {
                objStmEntry = topDownReferences?.get("${objEntry.objStm} 0")
            }

            return if (objStmEntry != null) {
                val objStm = try {
                    fileReader.getObjectStream(
                        this,
                        objStmEntry.pos,
                        Reference(this, objEntry.objStm, 0)
                    )
                } catch (e: IndirectObjectMismatchException) {
                    if (!checkTopDownReferences) return null
                    val pos = topDownReferences?.get("${objEntry.objStm} 0")?.pos
                    if (pos != null) {
                        fileReader.getObjectStream(this, pos)
                    } else {
                        return null
                    }
                }

                if (objEntry.index != -1) {
                    val objBytes = objStm.extractObjectBytes(objEntry.index)
                    stringBuilder.clear().appendBytes(objBytes ?: byteArrayOf())
                        .toPDFObject(this, reference.obj, reference.gen)
                } else {
                    val objBytes = objStm.extractObjectBytesGivenObjectNum(reference.obj)
                    stringBuilder.clear().appendBytes(objBytes ?: byteArrayOf())
                        .toPDFObject(this, reference.obj, reference.gen)
                }
            } else {
                null
            }
        } else {
            if (objEntry.pos < 0L && checkTopDownReferences) {
                objEntry = topDownReferences?.get("${objEntry.obj} ${objEntry.gen}") ?: return null
            }

            val content = try {
                fileReader.getIndirectObject(this, objEntry.pos, reference).extractContent()
            } catch (e: IndirectObjectMismatchException) {
                if (checkTopDownReferences) {

                    val pos = topDownReferences?.get("${objEntry.obj} ${objEntry.gen}")?.pos
                    if (pos != null) {
                        fileReader.getIndirectObject(this, pos, reference).extractContent()
                    } else {
                        StringBuilder()
                    }
                } else {
                    StringBuilder()
                }
            }
            if (content.isEmpty() || content.containedEqualsWith('n', 'u', 'l', 'l')) return null
            return content.toPDFObject(this, reference.obj, reference.gen, false) ?: reference
        }
    }

    override fun resolveReferenceToStream(reference: Reference): Stream? {
        val fileReader = fileReader ?: throw NoDocumentException()
        val objects = objects ?: throw NoDocumentException()
        val obj = objects["${reference.obj} ${reference.gen}"]
        return if (obj != null) {
            if (obj.pos < 0) {
                val objEntry = topDownReferences?.get("${reference.obj} ${reference.gen}")
                objEntry?.pos?.let { fileReader.getStream(this, it, reference) }
            } else {
                try {
                    fileReader.getStream(this, obj.pos, reference)
                } catch (e: IndirectObjectMismatchException) {
                    val pos = topDownReferences?.get("${reference.obj} ${reference.gen}")?.pos
                    if (pos != null) {
                        fileReader.getStream(this, pos)
                    } else {
                        return null
                    }
                }
            }
        } else {
            null
        }
    }

    abstract inner class Session
}