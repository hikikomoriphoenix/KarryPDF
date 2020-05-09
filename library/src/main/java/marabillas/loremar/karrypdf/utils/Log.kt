package marabillas.loremar.karrypdf.utils

internal var showKarryPDFLogs = false
internal var forceHideLogs = false
internal var filterErrorLogs = false

internal fun logd(message: String) {
    if (showKarryPDFLogs && !filterErrorLogs) {
        println("${Thread.currentThread()} -> $message")
    }
}

internal fun loge(message: String, e: Exception? = null) {
    if (showKarryPDFLogs) {
        System.err.println("${Thread.currentThread()} -> $message")
        e?.printStackTrace()
    }
}