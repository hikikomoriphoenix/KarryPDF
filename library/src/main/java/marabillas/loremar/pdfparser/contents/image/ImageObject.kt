package marabillas.loremar.pdfparser.contents.image

import marabillas.loremar.pdfparser.contents.PageObject

internal class ImageObject(private val tx: Float, private val ty: Float) : PageObject {
    var imageData = byteArrayOf()

    override fun getY(): Float {
        return ty
    }

    override fun getX(): Float {
        return tx
    }
}