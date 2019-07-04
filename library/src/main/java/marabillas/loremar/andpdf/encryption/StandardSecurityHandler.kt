package marabillas.loremar.andpdf.encryption

import marabillas.loremar.andpdf.exceptions.UnsupportedPDFElementException
import marabillas.loremar.andpdf.exceptions.ValidPasswordRequiredException
import marabillas.loremar.andpdf.objects.*
import marabillas.loremar.andpdf.objects.Dictionary
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.*
import kotlin.math.min

internal class StandardSecurityHandler(encryptionDictionary: Dictionary, idArray: PDFArray?, password: String) :
    SecurityHandler(encryptionDictionary) {
    private val version = (encryptionDictionary["V"] as Numeric?)?.value?.toInt()
    private val revision = (encryptionDictionary["R"] as Numeric).value.toInt()
    private val oPassword = (encryptionDictionary["O"] as PDFString).value.toByteArray(Charsets.ISO_8859_1)
    private val uPassword = (encryptionDictionary["U"] as PDFString).value.toByteArray(Charsets.ISO_8859_1)
    private val permissions = (encryptionDictionary["P"] as Numeric).value.toInt()
    private val isEncryptMetadata = (encryptionDictionary["EncryptMetadata"] as PDFBoolean?)?.value ?: true
    private val length = ((encryptionDictionary["Length"] as Numeric?)?.value?.toInt()?.div(8)) ?: 5
    private var idBytes: ByteArray? = null

    //private val md5 = MessageDigest.getInstance("MD5")
    private val rc4 = RC4()
    private var encryptionKey: ByteArray = byteArrayOf()

    private val passwordPadding = byteArrayOf(
        0x28.toByte(), 0xBF.toByte(), 0x4E.toByte(), 0x5E.toByte(),
        0x4E.toByte(), 0x75.toByte(), 0x8A.toByte(), 0x41.toByte(),
        0x64.toByte(), 0x00.toByte(), 0x4E.toByte(), 0x56.toByte(),
        0xFF.toByte(), 0xFA.toByte(), 0x01.toByte(), 0x08.toByte(),
        0x2E.toByte(), 0x2E.toByte(), 0x00.toByte(), 0xB6.toByte(),
        0xD0.toByte(), 0x68.toByte(), 0x3E.toByte(), 0x80.toByte(),
        0x2F.toByte(), 0x0C.toByte(), 0xA9.toByte(), 0xFE.toByte(),
        0x64.toByte(), 0x53.toByte(), 0x69.toByte(), 0x7A.toByte()
    )

    init {
        checkForUnsupportedConditions()
        getDocumentIdBytes(idArray)
        val passwordBytes = password.toByteArray(Charsets.ISO_8859_1)
        try {
            authenticateOwnerPassword(passwordBytes)
        } catch (e: ValidPasswordRequiredException) {
            authenticateUserPassword(passwordBytes)
        }

        if ((version != null && version >= 4) || revision >= 4) {
            TODO("Determine from crypt filter if AES algorithm is to be used")
        }
    }

    private fun checkForUnsupportedConditions() {
        if (revision >= 5) {
            throw UnsupportedPDFElementException("Security handler of revision 5 and above is not yet supported.")
        }
    }

    private fun getDocumentIdBytes(idArray: PDFArray?) {
        if (idArray is PDFArray && idArray.count() > 0) {
            idBytes = (idArray[0] as PDFString).value.toByteArray(Charsets.ISO_8859_1)
        }
    }

    private fun authenticateOwnerPassword(passwordBytes: ByteArray) {
        val userPassword = getUserPassword(passwordBytes)
        authenticateUserPassword(userPassword)
    }

    private fun getUserPassword(passwordBytes: ByteArray): ByteArray {
        val result = ByteArrayOutputStream()
        val rc4Key = computeRC4Key(passwordBytes)

        if (revision == 2) {
            TODO("getUserPassword for revision 2 not implemented")
        } else {
            val iterationKey = ByteArray(rc4Key.size)
            var otemp = ByteArray(oPassword.size)
            oPassword.copyInto(otemp)

            for (i in 19 downTo 0) {
                rc4Key.copyInto(iterationKey)
                for (j in 0 until iterationKey.size) {
                    iterationKey[j] = (iterationKey[j].toInt() xor i).toByte()
                }
                result.reset()
                rc4.reset(iterationKey)
                rc4.process(otemp, result)
                otemp = result.toByteArray()
            }
        }

        return result.toByteArray()
    }

    private fun computeRC4Key(passwordBytes: ByteArray): ByteArray {
        val md5 = MessageDigest.getInstance("MD5")
        var digest = md5.digest(truncateOrPad(passwordBytes))

        if (revision >= 3) {
            repeat(50) {
                md5.update(digest, 0, length)
                digest = md5.digest()
            }
        }

        val rc4Key = ByteArray(length)
        digest.copyInto(rc4Key, 0, 0, length)
        return rc4Key
    }

    private fun authenticateUserPassword(passwordBytes: ByteArray) {
        if (revision == 2) {
            TODO("Password authentication for revision 2 is not yet implemented")
        } else {
            val computedPassword = computeUserPasswordRev3Up(passwordBytes)
            if (!Arrays.equals(uPassword.copyOf(16), computedPassword.copyOf(16))) {
                throw ValidPasswordRequiredException()
            }
        }
    }

    private fun computeUserPasswordRev3Up(passwordBytes: ByteArray): ByteArray {
        val result = ByteArrayOutputStream()

        encryptionKey = computeEncryptionKey(passwordBytes)

        if (revision == 2) {
            TODO("Password computation for revision 2 not implemented")
        } else {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(passwordPadding)
            md5.update(idBytes)
            val hash = md5.digest()

            result.write(hash)

            val iterationKey = ByteArray(encryptionKey.size)
            for (i in 0 until 20) {
                encryptionKey.copyInto(iterationKey, 0, 0, iterationKey.size)
                for (j in 0 until iterationKey.size) {
                    iterationKey[j] = (iterationKey[j].toInt() xor i).toByte()
                }
                val input = result.toByteArray()
                result.reset()
                rc4.reset(iterationKey)
                rc4.process(input, result)
            }

            val finalResult = ByteArray(32)
            result.toByteArray().copyInto(finalResult, 0, 0, 16)
            result.reset()
            result.write(finalResult)
        }

        return result.toByteArray()
    }

    override fun getEncryptionKey(): ByteArray {
        return encryptionKey
    }

    private fun computeEncryptionKey(passwordBytes: ByteArray): ByteArray {
        val md5 = MessageDigest.getInstance("MD5")
        val padded = truncateOrPad(passwordBytes)
        md5.update(padded)
        md5.update(oPassword)
        md5.update(permissions.toByte())
        md5.update((permissions ushr 8).toByte())
        md5.update((permissions ushr 16).toByte())
        md5.update((permissions ushr 24).toByte())
        md5.update(idBytes)

        if (revision >= 4 && !isEncryptMetadata) {
            md5.update(byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte()))
        }

        var digest = md5.digest()

        if (revision >= 3) {
            repeat(50) {
                md5.update(digest, 0, length)
                digest = md5.digest()
            }
        }

        return if (revision == 2) {
            digest.copyOf(5)
        } else {
            val result = ByteArray(length)
            digest.copyInto(result, 0, 0, length)
            result
        }
    }

    private fun truncateOrPad(passwordBytes: ByteArray): ByteArray {
        val padded = ByteArray(passwordPadding.size)
        val bytesBeforePad = min(passwordBytes.size, padded.size)
        passwordBytes.copyInto(padded, 0, 0, bytesBeforePad)
        passwordPadding.copyInto(padded, bytesBeforePad, 0, passwordPadding.size - bytesBeforePad)
        return padded
    }
}