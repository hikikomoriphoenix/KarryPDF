package marabillas.loremar.karrypdf.filters

internal class BitArray(size: Int) {
    private val array = BooleanArray(size)

    operator fun get(i: Int): Boolean {
        return array[i]
    }

    operator fun set(i: Int, value: Boolean) {
        array[i] = value
    }

    override fun hashCode(): Int {
        return 1
    }

    override operator fun equals(other: Any?): Boolean {
        var equals = false
        if (other is BitArray) {
            if (array.contentEquals(other.array))
                equals = true
        }
        return equals
    }

    override fun toString(): String {
        val charArray = CharArray(array.size)
        array.forEachIndexed { i, bit ->
            if (bit) {
                charArray[i] = '1'
            } else {
                charArray[i] = '0'
            }
        }
        return String(charArray)
    }

    fun copyInto(target: BitArray) {
        var j = target.lastIndex()
        for (i in array.lastIndex downTo 0) {
            target[j] = array[i]
            j--
        }
    }

    fun next(): BitArray {
        val bitArray = BitArray(array.size)
        this.copyInto(bitArray)
        var i = bitArray.lastIndex()
        while (i >= 0) {
            if (!bitArray[i]) {
                bitArray[i] = true
                for (j in i + 1..bitArray.lastIndex()) {
                    bitArray[j] = false
                }
                break
            } else {
                i--
            }
        }

        return if (i < 0) {
            val bigger = BitArray(array.size + 1)
            bigger[0] = true
            bigger
        } else {
            bitArray
        }
    }

    fun previous(): BitArray {
        val bitArray = BitArray(array.size)
        this.copyInto(bitArray)
        var i = bitArray.lastIndex()
        while (i >= 0) {
            if (bitArray[i]) {
                bitArray[i] = false
                for (j in i + 1..bitArray.lastIndex()) {
                    bitArray[j] = true
                }
                break
            } else {
                i--
            }
        }

        return if (i == 0) {
            val smaller = BitArray(array.size - 1)
            for (j in 0..smaller.lastIndex()) {
                smaller[j] = true
            }
            smaller
        } else {
            bitArray
        }
    }

    fun size(): Int {
        return array.size
    }

    fun lastIndex(): Int {
        return array.size - 1
    }

    fun subArray(start: Int, end: Int): BitArray {
        val bitArray = BitArray(end - start)
        for ((j, i) in (start until end).withIndex()) {
            bitArray[j] = array[i]
        }
        return bitArray
    }
}

internal fun ByteArray.toBitArray(): BitArray {
    val bitArray = BitArray(this.size * 8)

    var offset = 0
    this.forEach { byte ->
        for (i in 0 until 8) {
            bitArray[offset + 7 - i] = ((byte.toInt() shr i) and 0x1) == 1
        }
        offset += 8
    }

    return bitArray
}

internal fun Byte.toBitArray(): BitArray {
    val bitArray = BitArray(9)

    for (i in 0 until 8) {
        bitArray[8 - i] = (((this.toInt() and 0xff) shr i) and 0x1) == 1
    }

    return bitArray
}

internal fun bitArrayOf(vararg bits: Boolean): BitArray {
    val bitArray = BitArray(bits.size)
    bits.forEachIndexed { i, bit ->
        bitArray[i] = bit
    }
    return bitArray
}
