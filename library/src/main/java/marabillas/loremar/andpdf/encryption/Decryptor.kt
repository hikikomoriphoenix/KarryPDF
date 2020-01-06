package marabillas.loremar.andpdf.encryption

import marabillas.loremar.andpdf.objects.Dictionary
import marabillas.loremar.andpdf.objects.Name
import marabillas.loremar.andpdf.objects.PDFArray
import marabillas.loremar.andpdf.objects.PDFString
import marabillas.loremar.andpdf.utils.exts.hexToBytes
import java.security.MessageDigest
import kotlin.math.min

internal class Decryptor(dictionary: Dictionary, idArray: PDFArray?, password: String) {
    private var encryptionKey: ByteArray
    private val securityHandler: SecurityHandler
    private val rc4 = RC4()
    private var useAES = false

    private val saltAES = byteArrayOf(0x73.toByte(), 0x41.toByte(), 0x6c.toByte(), 0x54.toByte())

    init {
        val filter = dictionary["Filter"] as Name
        if (filter.value == "Standard") {
            securityHandler = StandardSecurityHandler(dictionary, idArray, password)
        } else {
            TODO("Other security handlers not implemented")
        }
        encryptionKey = securityHandler.getEncryptionKey()
    }

    fun decrypt(encryptedData: ByteArray, obj: Int, gen: Int): ByteArray {
        val finalKey = finalKey(obj, gen)
        if (useAES) {
            TODO("AES algorithm not implemented")
        } else {
            return decryptRC4(encryptedData, finalKey)
        }
    }

    private fun decryptRC4(encryptedData: ByteArray, finalKey: ByteArray): ByteArray {
        rc4.reset(finalKey)
        return rc4.process(encryptedData)
    }

    private fun finalKey(obj: Int, gen: Int): ByteArray {
        val newKey = ByteArray(encryptionKey.size + 5)
        encryptionKey.copyInto(newKey)
        newKey[newKey.size - 5] = (obj and 0xff).toByte()
        newKey[newKey.size - 4] = (obj shr 8 and 0xff).toByte()
        newKey[newKey.size - 3] = (obj shr 16 and 0xff).toByte()
        newKey[newKey.size - 2] = (gen and 0xff).toByte()
        newKey[newKey.size - 1] = (gen shr 8 and 0xff).toByte()

        val md5 = MessageDigest.getInstance("MD5")
        md5.update(newKey)

        if (useAES) {
            md5.update(saltAES)
        }

        val digestedKey = md5.digest()
        val length = min(newKey.size, 16)
        val finalKey = ByteArray(length)
        digestedKey.copyInto(finalKey, 0, 0, length)
        return finalKey
    }

    fun decrypt(pdfString: PDFString, obj: Int, gen: Int): String? {
        return pdfString.original.run {
            if (startsWith('<')) {
                StringBuilder(substring(1, lastIndex)).hexToBytes()
            } else
                null
        }?.run {
            String(decrypt(this, obj, gen))
        }
    }
}