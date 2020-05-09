package marabillas.loremar.karrypdf.document

import marabillas.loremar.karrypdf.objects.PDFObject
import marabillas.loremar.karrypdf.objects.Reference
import marabillas.loremar.karrypdf.objects.Stream

internal interface ReferenceResolver {
    /**
     * Get the indirect object corresponding to the given indirect reference
     */
    fun resolveReference(reference: Reference, checkTopDownReferences: Boolean = true): PDFObject?

    /**
     * Get the stream object pointed to by the given indirect reference
     */
    fun resolveReferenceToStream(reference: Reference): Stream?
}