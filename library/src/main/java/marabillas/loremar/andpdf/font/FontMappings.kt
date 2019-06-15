package marabillas.loremar.andpdf.font

import android.graphics.Typeface
import android.graphics.Typeface.*

internal class FontMappings {
    companion object {
        private var fontMappings = HashMap<FontName, Typeface>()

        init {
            // Generic fonts
            fontMappings[FontName.SANS_SERIF] = SANS_SERIF
            fontMappings[FontName.SANS_SERIF_BOLD] = create("sans-serif", BOLD)
            fontMappings[FontName.SANS_SERIF_ITALIC] = create("sans-serif", ITALIC)
            fontMappings[FontName.SANS_SERIF_BOLD_ITALIC] = create("sans-serif", BOLD_ITALIC)
            fontMappings[FontName.SERIF] = SERIF
            fontMappings[FontName.SERIF_BOLD] = create("serif", BOLD)
            fontMappings[FontName.SERIF_ITALIC] = create("serif", ITALIC)
            fontMappings[FontName.SERIF_BOLD_ITALIC] = create("serif", BOLD_ITALIC)
            fontMappings[FontName.MONOSPACE] = MONOSPACE
            fontMappings[FontName.SERIF_MONOSPACE] = create("serif-monospace", NORMAL)

            // Default font when required typeface don't match
            fontMappings[FontName.DEFAULT] = fontMappings[FontName.SANS_SERIF] ?: SANS_SERIF
            fontMappings[FontName.DEFAULT_BOLD] = fontMappings[FontName.SANS_SERIF_BOLD]
                ?: create("sans-serif", BOLD)
            fontMappings[FontName.DEFAULT_ITALIC] = fontMappings[FontName.SANS_SERIF_ITALIC]
                ?: create("sans-serif", ITALIC)
            fontMappings[FontName.DEFAULT_BOLD_ITALIC] = fontMappings[FontName.SANS_SERIF_BOLD_ITALIC]
                ?: create("sans-serif", BOLD_ITALIC)

            fun default(): Typeface {
                return fontMappings[FontName.DEFAULT] ?: fontMappings[FontName.SANS_SERIF] ?: SANS_SERIF
            }

            fun defaultBold(): Typeface {
                return fontMappings[FontName.DEFAULT_BOLD] ?: fontMappings[FontName.SANS_SERIF_BOLD]
                ?: create("sans-serif", BOLD)
            }

            fun defaultItalic(): Typeface {
                return fontMappings[FontName.DEFAULT_ITALIC] ?: fontMappings[FontName.SANS_SERIF_ITALIC]
                ?: create("sans-serif", ITALIC)
            }

            fun defaultBoldItalic(): Typeface {
                return fontMappings[FontName.DEFAULT_BOLD_ITALIC] ?: fontMappings[FontName.SANS_SERIF_BOLD_ITALIC]
                ?: create("sans-serif", BOLD_ITALIC)
            }

            fun sansSerif(): Typeface {
                return fontMappings[FontName.SANS_SERIF] ?: SANS_SERIF
            }

            fun sansSerifBold(): Typeface {
                return fontMappings[FontName.SANS_SERIF_BOLD] ?: create("sans-serif", BOLD)
            }

            fun sansSerifItalic(): Typeface {
                return fontMappings[FontName.SANS_SERIF_ITALIC] ?: create("sans-serif", ITALIC)
            }

            fun sansSerifBoldItalic(): Typeface {
                return fontMappings[FontName.SANS_SERIF_BOLD_ITALIC] ?: create("sans-serif", BOLD_ITALIC)
            }

            fun serif(): Typeface {
                return fontMappings[FontName.SERIF] ?: SERIF
            }

            fun serifBold(): Typeface {
                return fontMappings[FontName.SERIF_BOLD] ?: create("serif", BOLD)
            }

            fun serifItalic(): Typeface {
                return fontMappings[FontName.SERIF_ITALIC] ?: create("serif", ITALIC)
            }

            fun serifBoldItalic(): Typeface {
                return fontMappings[FontName.SERIF_BOLD_ITALIC] ?: create("serif", BOLD_ITALIC)
            }

            fun monospace(): Typeface {
                return fontMappings[FontName.MONOSPACE] ?: MONOSPACE
            }

            fun serifMonospace(): Typeface {
                return fontMappings[FontName.SERIF_MONOSPACE] ?: create("serif-monospace", NORMAL)
            }

            fontMappings[FontName.ARIAL] = sansSerif()
            fontMappings[FontName.BASKERVILLE] = serif()
            fontMappings[FontName.CASUAL] = create("casual", NORMAL)
            fontMappings[FontName.COURIER] = serifMonospace()
            fontMappings[FontName.COURIER_BOLD] = serifMonospace()
            fontMappings[FontName.COURIER_BOLD_OBLIQUE] = serifMonospace()
            fontMappings[FontName.COURIER_OBLIQUE] = serifMonospace()
            fontMappings[FontName.CURSIVE] = create("cursive", NORMAL)
            fontMappings[FontName.FANTASY] = serif()
            fontMappings[FontName.GEORGIA] = serif()
            fontMappings[FontName.GOUDY] = serif()
            fontMappings[FontName.HELVETICA] = sansSerif()
            fontMappings[FontName.HELVETICA_BOLD] = sansSerifBold()
            fontMappings[FontName.HELVETICA_BOLD_OBLIQUE] = sansSerifBoldItalic()
            fontMappings[FontName.HELVETICA_OBLIQUE] = sansSerifItalic()
            fontMappings[FontName.ITC_STONE_SERIF] = serif()
            fontMappings[FontName.MONACO] = monospace()
            fontMappings[FontName.PALATINO] = serif()
            fontMappings[FontName.SYMBOL] = DEFAULT
            fontMappings[FontName.TAHOMA] = sansSerif()
            fontMappings[FontName.TIMES_ROMAN] = serif()
            fontMappings[FontName.TIMES_BOLD] = serifBold()
            fontMappings[FontName.TIMES_ITALIC] = serifItalic()
            fontMappings[FontName.TIMES_BOLD_ITALIC] = serifBoldItalic()
            fontMappings[FontName.TIMES_NEW_ROMAN] = serif()
            fontMappings[FontName.VERDANA] = sansSerif()
            fontMappings[FontName.ZAPFDINGBATS] = DEFAULT
        }

        operator fun get(fontName: FontName): Typeface {
            return fontMappings[fontName] ?: fontMappings[FontName.DEFAULT] ?: fontMappings[FontName.SANS_SERIF]
            ?: SANS_SERIF
        }

        operator fun set(fontName: FontName, typeface: Typeface) {
            fontMappings[fontName] = typeface
        }
    }
}