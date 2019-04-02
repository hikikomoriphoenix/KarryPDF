package marabillas.loremar.pdfparser.font

import android.graphics.Typeface

import marabillas.loremar.pdfparser.font.FontName.*


internal class FontIdentifier {
    fun identifyFont(fontName: String): Typeface {
        return when {
            fontName.contains("Arial", true) -> FontMappings[ARIAL]
            fontName.contains("Baskerville", true) -> FontMappings[BASKERVILLE]
            fontName.contains("Casual", true) -> FontMappings[CASUAL]
            fontName.contains("Courier", true) -> {
                identifyWithStyle(
                    fontName,
                    FontMappings[COURIER_BOLD_OBLIQUE],
                    FontMappings[COURIER_BOLD],
                    FontMappings[COURIER_OBLIQUE],
                    FontMappings[COURIER]
                )
            }
            fontName.contains("Cursive", true) -> FontMappings[CURSIVE]
            fontName.contains("Fantasy", true) -> FontMappings[FANTASY]
            fontName.contains("Georgia", true) -> FontMappings[GEORGIA]
            fontName.contains("Goudy", true) -> FontMappings[GOUDY]
            fontName.contains("Helvetica", true) -> {
                identifyWithStyle(
                    fontName,
                    FontMappings[HELVETICA_BOLD_OBLIQUE],
                    FontMappings[HELVETICA_BOLD],
                    FontMappings[HELVETICA_OBLIQUE],
                    FontMappings[HELVETICA]
                )
            }
            fontName.contains("Stone", true) && fontName.contains("Serif", true) ->
                FontMappings[ITC_STONE_SERIF]
            fontName.contains("Palatino", true) -> FontMappings[PALATINO]
            fontName.contains("Monaco", true) -> FontMappings[MONACO]
            fontName.equals("Symbol", true) -> FontMappings[SYMBOL]
            fontName.contains("Tahoma", true) -> FontMappings[TAHOMA]
            fontName.contains("Times", true) && fontName.contains("New", true)
                    && fontName.contains("Roman", true) -> FontMappings[TIMES_NEW_ROMAN]
            fontName.contains("Times", true) -> {
                identifyWithStyle(
                    fontName,
                    FontMappings[TIMES_BOLD_ITALIC],
                    FontMappings[TIMES_BOLD],
                    FontMappings[TIMES_ITALIC],
                    FontMappings[TIMES_ROMAN]
                )
            }
            fontName.contains("Verdana", true) -> FontMappings[VERDANA]
            fontName.contains("Zapf", true) && fontName.contains("Dingbats", true) ->
                FontMappings[ZAPFDINGBATS]
            fontName.contains("Mono", true) -> FontMappings[MONOSPACE]
            fontName.contains("Sans", true) && fontName.contains("Serif", true) -> {
                identifyWithStyle(
                    fontName,
                    FontMappings[SANS_SERIF_BOLD_ITALIC],
                    FontMappings[SANS_SERIF_BOLD],
                    FontMappings[SANS_SERIF_ITALIC],
                    FontMappings[SANS_SERIF]
                )
            }
            fontName.contains("Serif", true) -> {
                identifyWithStyle(
                    fontName,
                    FontMappings[SERIF_BOLD_ITALIC],
                    FontMappings[SERIF_BOLD],
                    FontMappings[SERIF_ITALIC],
                    FontMappings[SERIF]
                )
            }
            containsBoldItalic(fontName) -> FontMappings[DEFAULT_BOLD_ITALIC]
            containsBold(fontName) -> FontMappings[DEFAULT_BOLD]
            containsItalic(fontName) -> FontMappings[DEFAULT_ITALIC]
            else -> FontMappings[DEFAULT]
        }
    }

    private fun identifyWithStyle(
        fontName: String,
        boldItalic: Typeface,
        bold: Typeface,
        italic: Typeface,
        normal: Typeface
    ): Typeface {
        return when {
            containsBoldItalic(fontName) -> boldItalic
            containsBold(fontName) -> bold
            containsItalic(fontName) -> italic
            else -> normal
        }
    }

    private fun containsBoldItalic(fontName: String): Boolean {
        return fontName.contains("Bold", true) && (fontName.contains("Italic", true)
                || fontName.contains("Oblique", true))
    }

    private fun containsBold(fontName: String): Boolean {
        return fontName.contains("Bold", true)
    }

    private fun containsItalic(fontName: String): Boolean {
        return fontName.contains("Italic", true) || fontName.contains("Oblique", true)
    }
}