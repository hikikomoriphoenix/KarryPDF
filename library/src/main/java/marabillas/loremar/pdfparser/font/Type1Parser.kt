package marabillas.loremar.pdfparser.font

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.font.encoding.MacExpertEncoding
import marabillas.loremar.pdfparser.font.encoding.MacRomanEncoding
import marabillas.loremar.pdfparser.font.encoding.StandardEncoding
import marabillas.loremar.pdfparser.font.encoding.WinAnsiEncoding
import marabillas.loremar.pdfparser.utils.exts.set
import marabillas.loremar.pdfparser.utils.exts.toInt
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class Type1Parser(val data: ByteArray) {
    private var encryptedSection: ByteArray? = null
    private val charStrings = HashMap<String, ByteArray>()
    private val charCommands = HashMap<String, HashMap<CharCommand, IntArray>>()
    private val stringBuilder = StringBuilder()

    init {
        encryptedSection = decryptEncryptedSection()
        val lenIV = getLenIV(encryptedSection as ByteArray)
        charStrings.forEach {
            charCommands[it.key] = decodeCharString(it.value, lenIV)
        }
    }

    private fun decryptEncryptedSection(): ByteArray {
        val start = locateEncryptedSection()
        val decrypted = decryptEexec(start)
        decryptCharStrings(decrypted)
        return decrypted
    }

    private fun locateEncryptedSection(): Int {
        val eexec = charArrayOf('e', 'e', 'x', 'e', 'c')
        val chars = CharArray(5)
        var pos = 0
        while (pos + 4 < data.size) {
            for (i in 0..4) {
                chars[i] = data[pos + i].toChar()
            }
            if (chars.contentEquals(eexec)) {
                pos += 5
                while (data[pos].toChar() == '\n' || data[pos].toChar() == '\r' || data[pos].toChar() == ' ') {
                    pos++
                }
                return pos
            }
            pos++
        }
        return pos
    }

    private fun decryptEexec(start: Int): ByteArray {
        var r = 55665
        val c1 = 52845
        val c2 = 22719
        var pos = start
        val plainText = ByteArrayOutputStream()
        while (pos < data.size) {
            val plain = (data[pos].toInt() and 0xff) xor (r ushr 8)
            plainText.write(plain)
            r = ((data[pos].toInt() and 0xff) + r) * c1 + c2
            pos++
        }
        return plainText.toByteArray()
    }

    private fun decryptCharStrings(data: ByteArray) {
        var pos = locateCharStrings(data)
        charStrings.clear()
        while (pos < data.size) {
            if (data[pos].toChar() == 'e' && data[pos + 1].toChar() == 'n' && data[pos + 2].toChar() == 'd')
                break

            if (data[pos].toChar() == '/') {
                // Extract charname
                pos++
                stringBuilder.clear()
                while (data[pos].toChar() != ' ') {
                    stringBuilder.append(data[pos].toChar())
                    pos++
                }

                // Extract length of charstring
                pos++
                var length = 0
                while (data[pos].toChar() != ' ') {
                    val num = Character.getNumericValue(data[pos].toChar())
                    if (num < 0) throw NumberFormatException(
                        "Attempting to get length of charstring but can not" +
                                " evaluate to a number"
                    )
                    length = length * 10 + num
                    pos++
                }

                // Extract charstring and decrypt
                while (data[pos].toChar() != '/') {
                    if (
                        (data[pos].toChar() == 'R' && data[pos + 1].toChar() == 'D')
                        || (data[pos].toChar() == '-' && data[pos + 1].toChar() == '|')
                    ) {
                        // Skip RD or -| and a space
                        pos += 3
                        decryptCharString(data, pos, length)
                        pos += length
                        break
                    }
                    pos++
                }
            }
            pos++
        }
    }

    private fun decryptCharString(data: ByteArray, start: Int, length: Int) {
        var r = 4330
        val c1 = 52845
        val c2 = 22719
        var pos = start
        val plainText = ByteArrayOutputStream()
        repeat(length) {
            val plain = (data[pos].toInt() and 0xff) xor (r ushr 8)
            plainText.write(plain)
            r = ((data[pos].toInt() and 0xff) + r) * c1 + c2
            pos++
        }
        charStrings[stringBuilder.toString()] = plainText.toByteArray()
    }

    private fun locateCharStrings(data: ByteArray): Int {
        val charStrings = charArrayOf('/', 'C', 'h', 'a', 'r', 'S', 't', 'r', 'i', 'n', 'g', 's')
        val ch = charArrayOf('/', 'C', 'h')
        val chars = CharArray(3)
        val bigChars = CharArray(12)
        var pos = 0
        while (pos + 11 < data.size) {
            for (i in 0..2) {
                chars[i] = data[pos + i].toChar()
            }
            if (chars.contentEquals(ch)) {
                for (i in 0..11) {
                    bigChars[i] = data[pos + i].toChar()
                }
                if (bigChars.contentEquals(charStrings))
                    return pos + 12
            }
            pos++
        }
        return pos
    }

    private fun decodeCharString(charString: ByteArray, skipBytes: Int = 4): HashMap<CharCommand, IntArray> {
        var i = skipBytes
        val operands = mutableListOf<Int>()
        val commands = HashMap<CharCommand, IntArray>()
        while (i < charString.size) {
            when (val byte = charString[i].toInt() and 0xFF) {
                in 32..246 -> operands.add(byte - 139)
                in 247..250 -> {
                    val w = charString[++i].toInt() and 0xFF
                    operands.add((byte - 247) * 256 + w + 108)
                }
                in 251..254 -> {
                    val w = charString[++i].toInt() and 0xFF
                    operands.add(-(byte - 247) * 256 - w - 108)
                }
                255 -> {
                    val buffer = ByteBuffer.allocate(4)
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    repeat(4) {
                        buffer.put((charString[++i].toInt() and 0xFF).toByte())
                    }
                    buffer.flip()
                    operands.add(buffer.int)
                }
                in 0..31 -> {
                    if (byte != 12) {
                        saveCommand(byte, operands, commands)
                    } else {
                        val commandPlus = charString[++i].toInt() and 0xFF
                        saveCommandPlus(commandPlus, operands, commands)
                    }
                }
            }
            i++
        }
        return commands
    }

    private fun saveCommand(byte: Int, operands: MutableList<Int>, commands: HashMap<CharCommand, IntArray>) {
        when (byte) {
            1 -> saveCommandAndClear(commands, CharCommand.HSTEM, operands, 1)
            3 -> saveCommandAndClear(commands, CharCommand.VSTEM, operands, 2)
            4 -> saveCommandAndClear(commands, CharCommand.VMOVETO, operands, 1)
            5 -> saveCommandAndClear(commands, CharCommand.RLINETO, operands, 2)
            6 -> saveCommandAndClear(commands, CharCommand.HLINETO, operands, 1)
            7 -> saveCommandAndClear(commands, CharCommand.VLINETO, operands, 1)
            8 -> saveCommandAndClear(commands, CharCommand.RRCURVETO, operands, 6)
            9 -> saveCommandAndClear(commands, CharCommand.CLOSEPATH, operands, 0)
            10 -> commands[CharCommand.CALLSUBR] = intArrayOf(operands.last())
            11 -> commands[CharCommand.RETURN] =
                intArrayOf() // TODO return needs to get result from callsubr and add to operands.
            13 -> saveCommandAndClear(commands, CharCommand.HSBW, operands, 2)
            14 -> saveCommandAndClear(commands, CharCommand.ENDCHAR, operands, 0)
            21 -> saveCommandAndClear(commands, CharCommand.RMOVETO, operands, 2)
            22 -> saveCommandAndClear(commands, CharCommand.HMOVETO, operands, 1)
            30 -> saveCommandAndClear(commands, CharCommand.VHCURVETO, operands, 4)
            31 -> saveCommandAndClear(commands, CharCommand.HVCURVETO, operands, 4)

        }
    }

    private fun saveCommandPlus(byte: Int, operands: MutableList<Int>, commands: HashMap<CharCommand, IntArray>) {
        when (byte) {
            0 -> saveCommandAndClear(commands, CharCommand.DOTSECTION, operands, 0)
            1 -> saveCommandAndClear(commands, CharCommand.VSTEM3, operands, 6)
            2 -> saveCommandAndClear(commands, CharCommand.HSTEM3, operands, 6)
            6 -> saveCommandAndClear(commands, CharCommand.SEAC, operands, 5)
            7 -> saveCommandAndClear(commands, CharCommand.SBW, operands, 4)
            12 -> {
                // Perform DIV
                val dividend = operands[operands.lastIndex - 1]
                val divisor = operands.last()
                val quotient = Math.round(dividend.toFloat() / divisor.toFloat())
                operands.remove(dividend)
                operands.remove(divisor)
                operands.add(quotient)
            }
            16 -> commands[CharCommand.CALLOTHERSUBR] = operands.toIntArray()
            17 -> commands[CharCommand.POP] =
                intArrayOf() // TODO pop needs to get result from callothersubr and add to operands.
            33 -> saveCommandAndClear(commands, CharCommand.SETCURRENTPOINT, operands, 2)
        }
    }

    private fun saveCommandAndClear(
        commands: HashMap<CharCommand, IntArray>,
        command: CharCommand,
        operands: MutableList<Int>,
        numOperands: Int
    ) {
        val input = IntArray(numOperands)
        if (numOperands > 0) {
            for (num in numOperands downTo 1) {
                input[numOperands - num] = operands[operands.count() - num]
            }
        }
        commands[command] = input
        operands.clear()
    }

    internal enum class CharCommand {
        ENDCHAR,
        HSBW,
        SEAC,
        SBW,
        CLOSEPATH,
        HLINETO,
        HMOVETO,
        HVCURVETO,
        RLINETO,
        RMOVETO,
        RRCURVETO,
        VHCURVETO,
        VLINETO,
        VMOVETO,
        DOTSECTION,
        HSTEM,
        HSTEM3,
        VSTEM,
        VSTEM3,
        DIV,
        CALLOTHERSUBR,
        CALLSUBR,
        POP,
        RETURN,
        SETCURRENTPOINT
    }

    private fun getLenIV(data: ByteArray): Int {
        var pos = 0
        while (pos + 5 < data.size) {
            if (
                data[pos].toChar() == '/'
                && data[pos + 1].toChar() == 'l'
                && data[pos + 2].toChar() == 'e'
                && data[pos + 3].toChar() == 'n'
                && data[pos + 4].toChar() == 'I'
                && data[pos + 5].toChar() == 'V'
            ) {
                // Move to position after next space
                pos += 7
                var lenIV = 0
                while (data[pos].toChar() != ' ') {
                    val num = Character.getNumericValue(data[pos].toChar())
                    if (num < 0) throw java.lang.NumberFormatException(
                        "Attempting to get value for lenIV but can not " +
                                "evaluate to a number"
                    )
                    lenIV = lenIV * 10 + num
                    pos++
                }
                return lenIV
            } else if (
                pos + 11 < data.size
                && data[pos].toChar() == '/'
                && data[pos + 1].toChar() == 'C'
                && data[pos + 2].toChar() == 'h'
                && data[pos + 3].toChar() == 'a'
                && data[pos + 4].toChar() == 'r'
                && data[pos + 5].toChar() == 'S'
                && data[pos + 6].toChar() == 't'
                && data[pos + 7].toChar() == 'r'
                && data[pos + 8].toChar() == 'i'
                && data[pos + 9].toChar() == 'n'
                && data[pos + 10].toChar() == 'g'
                && data[pos + 11].toChar() == 's'
            ) {
                break
            }
            pos++
        }

        return 4 // Return default number of random bytes in charstring
    }

    fun getCharacterWidths(diffArray: SparseArrayCompat<String>): SparseArrayCompat<Float> {
        // For StandardEncoding and other predefined encoding:
        // Convert numbers from 0 to 255 to octal.
        // Get character name from SparseArrayCompat.
        // Get character widths using character names.

        val characterWidths = SparseArrayCompat<Float>()

        val encodingPos = getEncodingLocation() + 10
        when {
            isStandardEncoding(encodingPos) -> {
                println("Using StandardEncoding as base encoding")
                for (i in 0..255) {
                    var charName = diffArray[i]
                    if (charName == null) {
                        val octal = decimalToOctal(i)
                        charName = Encoding.standard[octal]
                    }
                    val command = charCommands[charName]
                    val hsbw = command?.get(CharCommand.HSBW)
                    val width = hsbw?.get(1)
                    width?.let {
                        characterWidths[i] = width.toFloat()
                    }
                }
            }
            isMacRomanEncoding(encodingPos) -> TODO("Needs to use MacRomanEncoding to get widths")
            isMacExpertEncoding(encodingPos) -> TODO("Needs to use MacExpertEncoding to get widths")
            isWinAnsiEncoding(encodingPos) -> TODO("Needs to use WinAnsiEncoding to get widths")
            else -> {
                println("Using built-in encoding as base encoding")
                useBuiltInEncodingToGetWidths(encodingPos, characterWidths, diffArray)
            }
        }

        // Assign width for missing character
        val command = charCommands[".notdef"]
        val hsbw = command?.get(CharCommand.HSBW)
        val width = hsbw?.get(1)
        width?.let {
            characterWidths[-1] = width.toFloat()
        }

        println("${characterWidths.size()} widths obtained from Type1 font")
        return characterWidths
    }

    private fun decimalToOctal(num: Int): Int {
        var octal = 0
        var q = num
        var r: Int
        var factor = 1
        while (true) {
            r = q % 8
            q /= 8
            octal += r * factor
            factor *= 10

            if (q == 0) break
        }
        return octal
    }

    private fun getEncodingLocation(): Int {
        var pos = 0
        while (pos + 8 < data.size) {
            if (
                data[pos].toChar() == '/'
                && data[pos + 1].toChar() == 'E'
                && data[pos + 2].toChar() == 'n'
                && data[pos + 3].toChar() == 'c'
                && data[pos + 4].toChar() == 'o'
                && data[pos + 5].toChar() == 'd'
                && data[pos + 6].toChar() == 'i'
                && data[pos + 7].toChar() == 'n'
                && data[pos + 8].toChar() == 'g'
            ) {
                return pos
            }
            pos++
        }
        return pos
    }

    private fun isStandardEncoding(pos: Int): Boolean {
        return (
                data[pos].toChar() == 'S'
                        && data[pos + 1].toChar() == 't'
                        && data[pos + 2].toChar() == 'a'
                        && data[pos + 3].toChar() == 'n'
                        && data[pos + 4].toChar() == 'd'
                        && data[pos + 5].toChar() == 'a'
                        && data[pos + 6].toChar() == 'r'
                        && data[pos + 7].toChar() == 'd'
                )
    }

    private fun isMacRomanEncoding(pos: Int): Boolean {
        return (
                data[pos].toChar() == 'M'
                        && data[pos + 1].toChar() == 'a'
                        && data[pos + 2].toChar() == 'c'
                        && data[pos + 3].toChar() == 'R'
                        && data[pos + 4].toChar() == 'o'
                        && data[pos + 5].toChar() == 'm'
                        && data[pos + 6].toChar() == 'a'
                        && data[pos + 7].toChar() == 'n'
                )
    }

    private fun isMacExpertEncoding(pos: Int): Boolean {
        return (
                data[pos].toChar() == 'M'
                        && data[pos + 1].toChar() == 'a'
                        && data[pos + 2].toChar() == 'c'
                        && data[pos + 3].toChar() == 'E'
                        && data[pos + 4].toChar() == 'x'
                        && data[pos + 5].toChar() == 'p'
                        && data[pos + 6].toChar() == 'e'
                        && data[pos + 7].toChar() == 'r'
                        && data[pos + 8].toChar() == 't'
                )
    }

    private fun isWinAnsiEncoding(pos: Int): Boolean {
        return (
                data[pos].toChar() == 'W'
                        && data[pos + 1].toChar() == 'i'
                        && data[pos + 2].toChar() == 'n'
                        && data[pos + 3].toChar() == 'A'
                        && data[pos + 4].toChar() == 'n'
                        && data[pos + 5].toChar() == 's'
                        && data[pos + 6].toChar() == 'i'
                )
    }

    object Encoding {
        val standard = SparseArrayCompat<String>()

        init {
            standard[101] = "A"
            standard[341] = "AE"
            standard[102] = "B"
            standard[103] = "C"
            standard[104] = "D"
            standard[105] = "E"
            standard[106] = "F"
            standard[107] = "G"
            standard[110] = "H"
            standard[111] = "I"
            standard[112] = "J"
            standard[113] = "K"
            standard[114] = "L"
            standard[350] = "LSlash"
            standard[115] = "M"
            standard[116] = "N"
            standard[117] = "O"
            standard[352] = "OE"
            standard[351] = "OSlash"
            standard[120] = "P"
            standard[121] = "Q"
            standard[122] = "R"
            standard[123] = "S"
            standard[124] = "T"
            standard[125] = "U"
            standard[126] = "V"
            standard[127] = "W"
            standard[130] = "X"
            standard[131] = "Y"
            standard[132] = "Z"
            standard[141] = "a"
            standard[302] = "acute"
            standard[361] = "ae"
            standard[46] = "ampersand"
            standard[136] = "asciicircum"
            standard[176] = "asciitilde"
            standard[52] = "asterisk"
            standard[100] = "at"
            standard[142] = "b"
            standard[134] = "backslash"
            standard[174] = "bar"
            standard[173] = "braceleft"
            standard[175] = "braceright"
            standard[133] = "bracketleft"
            standard[135] = "bracketright"
            standard[306] = "breve"
            standard[267] = "bullet"
            standard[143] = "c"
            standard[317] = "caron"
            standard[313] = "cedilla"
            standard[242] = "cent"
            standard[303] = "circumflex"
            standard[72] = "colon"
            standard[54] = "comma"
            standard[250] = "currency"
            standard[144] = "d"
            standard[262] = "dagger"
            standard[263] = "daggerdbl"
            standard[310] = "dieresis"
            standard[44] = "dollar"
            standard[307] = "dotaccent"
            standard[365] = "dotlessi"
            standard[145] = "e"
            standard[70] = "eight"
            standard[274] = "ellipsis"
            standard[320] = "emdash"
            standard[261] = "endash"
            standard[75] = "equal"
            standard[41] = "exclam"
            standard[241] = "exclamdown"
            standard[146] = "f"
            standard[256] = "fi"
            standard[65] = "five"
            standard[257] = "fl"
            standard[246] = "florin"
            standard[64] = "four"
            standard[244] = "fraction"
            standard[147] = "g"
            standard[373] = "germandbls"
            standard[301] = "grave"
            standard[76] = "greater"
            standard[253] = "guillemotleft"
            standard[273] = "guillemotright"
            standard[254] = "guilsinglleft"
            standard[255] = "guilsinglright"
            standard[150] = "h"
            standard[315] = "hungarumlaut"
            standard[55] = "hyphen"
            standard[151] = "i"
            standard[152] = "j"
            standard[153] = "k"
            standard[154] = "l"
            standard[74] = "less"
            standard[370] = "lslash"
            standard[155] = "m"
            standard[305] = "macron"
            standard[156] = "n"
            standard[71] = "nine"
            standard[43] = "numbersign"
            standard[157] = "o"
            standard[372] = "oe"
            standard[316] = "ogonek"
            standard[61] = "one"
            standard[343] = "ordfeminine"
            standard[353] = "ordmasculine"
            standard[371] = "oslash"
            standard[160] = "p"
            standard[266] = "paragraph"
            standard[50] = "parenleft"
            standard[51] = "parenright"
            standard[45] = "percent"
            standard[56] = "period"
            standard[264] = "periodcentered"
            standard[275] = "perthousand"
            standard[53] = "plus"
            standard[161] = "q"
            standard[77] = "question"
            standard[277] = "questiondown"
            standard[42] = "quotedbl"
            standard[271] = "quotedblbase"
            standard[252] = "quotedblleft"
            standard[272] = "quotedblright"
            standard[140] = "quoteleft"
            standard[47] = "quoteright"
            standard[270] = "quotesinglbase"
            standard[251] = "quotesingle"
            standard[162] = "r"
            standard[312] = "ring"
            standard[163] = "s"
            standard[247] = "section"
            standard[73] = "semicolon"
            standard[67] = "seven"
            standard[66] = "six"
            standard[57] = "slash"
            standard[40] = "space"
            standard[243] = "sterling"
            standard[164] = "t"
            standard[63] = "three"
            standard[304] = "tilde"
            standard[62] = "two"
            standard[165] = "u"
            standard[137] = "underscore"
            standard[166] = "v"
            standard[167] = "w"
            standard[170] = "x"
            standard[171] = "y"
            standard[245] = "yen"
            standard[172] = "z"
            standard[160] = "zero"
        }
    }

    private fun useBuiltInEncodingToGetWidths(
        start: Int,
        characterWidths: SparseArrayCompat<Float>,
        diffArray: SparseArrayCompat<String>
    ) {
        val getCharacterWidth = { charCode: Int, charName: String ->
            val command = charCommands[charName]
            val hsbw = command?.get(CharCommand.HSBW)
            val width = hsbw?.get(1)
            width?.let {
                characterWidths[charCode] = width.toFloat()
            } ?: Unit
        }

        parseAndProcessEncoding(start, getCharacterWidth)

        // Override with diffArray
        for (k in 0 until diffArray.size()) {
            val charName = diffArray.valueAt(k)
            val command = charCommands[charName]
            val hsbw = command?.get(CharCommand.HSBW)
            val width = hsbw?.get(1)
            width?.let {
                characterWidths[k] = width.toFloat()
            }
        }
    }

    private fun parseAndProcessEncoding(start: Int, action: (charCode: Int, charName: String) -> Unit) {
        var i = start
        while (i + 2 < data.size) {
            if (data[i].toChar() == 'p' && data[i + 1].toChar() == 'u' && data[i + 2].toChar() == 't') {
                // Locate character name beginning
                var p = i
                while (data[p].toChar() != '/')
                    p--

                // Extract and save character name
                var c = 1
                stringBuilder.clear()
                while (data[p + c].toChar() != ' ')
                    stringBuilder.append(data[p + c++].toChar())
                val charName = stringBuilder.toString()

                // Locate character code beginning
                p -= 2 // Skip space
                while (data[p].toChar() != ' ')
                    p--

                // Extract and save character code
                c = 1
                stringBuilder.clear()
                while (data[p + c].toChar() != ' ')
                    stringBuilder.append(data[p + c++].toChar())
                if (stringBuilder[0].isLetter()) {
                    // Skip .notdef
                    i += 2
                    continue
                }
                val charCode = stringBuilder.toInt()

                // Process encoding
                action(charCode, charName)

                // Move to end of 'put' operator
                i += 2
            } else if (
                data[i].toChar() == ' '
                && data[i + 1].toChar() == 'd'
                && data[i + 2].toChar() == 'e'
                && data[i + 3].toChar() == 'f'
            ) {
                break
            }
            i++
        }
    }

    fun getBuiltInEncoding(encodingArray: SparseArrayCompat<String>) {
        println("Getting built-in encoding from Type1 font")
        val encodingPos = getEncodingLocation() + 10
        when {
            isMacRomanEncoding(encodingPos) -> {
                println("Type1 Encoding = MacRomanEncoding")
                MacRomanEncoding.putAllTo(encodingArray)
            }
            isWinAnsiEncoding(encodingPos) -> {
                println("Type1 Encoding = WinAnsiEncoding")
                WinAnsiEncoding.putAllTo(encodingArray)
            }
            isMacExpertEncoding(encodingPos) -> {
                println("Type1 Encoding = MacExpertEncoding")
                MacExpertEncoding.putAllTo(encodingArray)
            }
            isStandardEncoding(encodingPos) -> {
                println("Type1 Encoding = StandardEncoding")
                StandardEncoding.putAllTo(encodingArray)
            }
            else -> {
                val getEncoding = { charCode: Int, charName: String ->
                    encodingArray[charCode] = charName
                }
                val initial = encodingArray.size()
                parseAndProcessEncoding(encodingPos, getEncoding)
                println("Obtained ${encodingArray.size() - initial} character names from Encoding array")
            }
        }
    }
}
