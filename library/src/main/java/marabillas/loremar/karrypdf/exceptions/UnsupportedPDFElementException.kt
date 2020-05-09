package marabillas.loremar.karrypdf.exceptions

class UnsupportedPDFElementException : Exception {
    constructor() : super("Can not process a PDF element. This element is not supported yet.")
    constructor(message: String) : super(message)
}