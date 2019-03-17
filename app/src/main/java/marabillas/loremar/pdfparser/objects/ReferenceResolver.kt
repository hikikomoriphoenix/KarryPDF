package marabillas.loremar.pdfparser.objects

internal interface ReferenceResolver {
    /**
     * Get the indirect object corresponding to the given indirect reference
     */
    fun resolveReference(reference: Reference): PDFObject?
}