package marabillas.loremar.pdfparser

import java.util.*

class CaseInsensitiveMap : HashMap<String, Any>() {
    override fun put(key: String, value: Any): Any? {
        return super.put(key.toLowerCase(), value)
    }

    override fun get(key: String): Any? {
        return super.get(key.toLowerCase())
    }
}