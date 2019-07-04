package marabillas.loremar.andpdf.encryption

import marabillas.loremar.andpdf.objects.Dictionary

internal abstract class SecurityHandler(encryptionDictionary: Dictionary) {
    abstract fun getEncryptionKey(): ByteArray
}