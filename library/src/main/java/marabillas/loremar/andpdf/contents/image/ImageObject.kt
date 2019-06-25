package marabillas.loremar.andpdf.contents.image

import android.graphics.Bitmap
import marabillas.loremar.andpdf.contents.PageObject

internal class ImageObject(private val tx: Float, private val ty: Float) : PageObject {
    var bitmap: Bitmap? = null

    override fun getY(): Float {
        return ty
    }

    override fun getX(): Float {
        return tx
    }
}