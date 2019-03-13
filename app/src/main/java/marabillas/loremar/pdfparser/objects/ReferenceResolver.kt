package marabillas.loremar.pdfparser.objects

interface ReferenceResolver {
    /**
     * Get the indirect object corresponding to the given indirect reference
     */
    fun resolveReference(reference: Reference): PDFObject?
}