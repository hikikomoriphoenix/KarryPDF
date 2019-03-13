package marabillas.loremar.pdfparser.exceptions

class NoDocumentException : Exception("No document to process. Must call loadDocument() on PDFParser first.")