package marabillas.loremar.karrypdf.document

import marabillas.loremar.karrypdf.objects.PDFObjectAdapter

internal class GetPageContentsContext(private val context: KarryPDFContext) : KarryPDFContext() {

    override var topDownReferences: HashMap<String, XRefEntry>? = null
        get() {
            if (field != null) return field

            return fileReader?.file?.let {
                synchronized(it) {
                    return when {
                        context.topDownReferencesAvailable -> {
                            field = context.topDownReferences
                            field
                        }
                        else -> {
                            TopDownParser(
                                this,
                                it
                            ).parseObjects()
                            context.topDownReferences = field
                            field
                        }
                    }
                }
            }
        }

    init {
        fileReader = context.fileReader
        objects = context.objects
        decryptor = context.decryptor
        session = object : Session() {}
        if (context.topDownReferencesAvailable) topDownReferences = context.topDownReferences
        PDFObjectAdapter.notifyNewSession(session)
        PDFFileReader.notifyNewSession(
            session
        )
    }

    fun release() {
        PDFObjectAdapter.endSession(session)
        PDFFileReader.endSession(session)
    }
}