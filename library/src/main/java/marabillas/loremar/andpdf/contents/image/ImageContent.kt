package marabillas.loremar.andpdf.contents.image

import marabillas.loremar.andpdf.contents.PageContent

data class ImageContent(val content: ByteArray) : PageContent {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageContent

        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        return content.contentHashCode()
    }
}