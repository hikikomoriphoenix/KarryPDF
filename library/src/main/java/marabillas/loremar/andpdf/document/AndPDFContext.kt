package marabillas.loremar.andpdf.document

import marabillas.loremar.andpdf.encryption.Decryptor
import marabillas.loremar.andpdf.objects.ReferenceResolver

internal class AndPDFContext {
    var fileReader: PDFFileReader? = null
    var referenceResolver: ReferenceResolver? = null
    var objects: HashMap<String, XRefEntry>? = null
    var decryptor: Decryptor? = null
}