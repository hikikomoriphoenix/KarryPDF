package marabillas.loremar.karrypdf.document

internal class GetPageContentsContext(private val context: KarryPDFContext) : KarryPDFContext() {

    override var topDownReferences: HashMap<String, XRefEntry>? = null
        get() {
            if (field != null) return field

            return fileReader.file.let {
                synchronized(it) {
                    when {
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
        if (context.topDownReferencesAvailable) topDownReferences = context.topDownReferences
    }
}