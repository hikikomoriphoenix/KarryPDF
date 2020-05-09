package marabillas.loremar.karrypdf.encryption

import marabillas.loremar.karrypdf.objects.Dictionary

internal abstract class SecurityHandler(encryptionDictionary: Dictionary) {
    abstract fun getEncryptionKey(): ByteArray
}