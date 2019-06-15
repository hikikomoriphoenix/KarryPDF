package marabillas.loremar.andpdf.contents

import marabillas.loremar.andpdf.objects.Name

internal class XObject(private val tx: Float, private val ty: Float, val resourceName: Name) : PageObject {
    override fun getY(): Float {
        return ty
    }

    override fun getX(): Float {
        return tx
    }
}