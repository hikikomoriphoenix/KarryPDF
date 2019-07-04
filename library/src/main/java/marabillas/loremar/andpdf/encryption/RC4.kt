package marabillas.loremar.andpdf.encryption

import java.io.OutputStream


/*
Implementations of this class is based from
https://github.com/apache/pdfbox/blob/trunk/pdfbox/src/main/java/org/apache/pdfbox/pdmodel/encryption/RC4Cipher.java
 */

internal class RC4 {
    private val salt = IntArray(256)
    private var b = 0
    private var c = 0

    fun reset(key: ByteArray) {
        b = 0
        c = 0

        if (key.isEmpty() || key.size > 32) {
            throw IllegalArgumentException("Number of bytes must be between 1 and 32")
        }

        repeat(salt.size) { i ->
            salt[i] = i
        }

        var keyIndex = 0
        var saltIndex = 0
        for (i in 0 until salt.size) {
            saltIndex = (fixByte(key[keyIndex]) + salt[i] + saltIndex) % 256
            swap(salt, i, saltIndex)
            keyIndex = (keyIndex + 1) % key.size
        }
    }

    private fun fixByte(aByte: Byte): Int {
        return if (aByte.toInt() < 0) 256 + aByte.toInt() else aByte.toInt()
    }

    private fun swap(data: IntArray, index1: Int, index2: Int) {
        val tmp = data[index1]
        data[index1] = data[index2]
        data[index2] = tmp
    }

    fun process(data: ByteArray): ByteArray {
        val output = ByteArray(data.size)
        data.forEachIndexed { i, byte ->
            b = (b + 1) % 256
            c = (salt[b] + c) % 256
            swap(salt, b, c)
            val saltIndex = (salt[b] + salt[c]) % 256
            output[i] = (
                    fixByte(byte) xor salt[saltIndex]
                    ).toByte()
        }
        return output
    }

    fun process(data: ByteArray, output: OutputStream) {
        for (aData in data) {
            process(aData, output)
        }
    }

    private fun process(aByte: Byte, output: OutputStream) {
        b = (b + 1) % 256
        c = (salt[b] + c) % 256
        swap(salt, b, c)
        val saltIndex = (salt[b] + salt[c]) % 256
        output.write(aByte.toInt() xor salt[saltIndex])
    }
}