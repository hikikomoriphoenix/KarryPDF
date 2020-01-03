package marabillas.loremar.andpdf.utils

internal var showAndPDFLogs = false
internal var forceHideLogs = false
internal var filterErrorLogs = false

internal fun logd(message: String) {
    if (showAndPDFLogs && !filterErrorLogs) {
        println("${Thread.currentThread()} -> $message")
    }
}

internal fun loge(message: String, e: Exception? = null) {
    if (showAndPDFLogs) {
        System.err.println("${Thread.currentThread()} -> $message")
        e?.printStackTrace()
    }
}