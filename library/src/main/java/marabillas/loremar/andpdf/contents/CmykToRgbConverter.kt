package marabillas.loremar.andpdf.contents

internal object CmykToRgbConverter {
    fun convert(cmyk: FloatArray): FloatArray {
        return floatArrayOf(
            (1 - cmyk[0]) * (1 - cmyk[3]),
            (1 - cmyk[1]) * (1 - cmyk[3]),
            (1 - cmyk[2]) * (1 - cmyk[3])
        )
    }
}