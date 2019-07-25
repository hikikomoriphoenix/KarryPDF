package marabillas.loremar.andpdf.objects

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