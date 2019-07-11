package marabillas.loremar.andpdf.utils

import android.util.Log

internal var showAndPDFLogs = false
internal const val andPDFLogsTag = "AndPDFLogs"

internal fun logd(message: String) {
    if (showAndPDFLogs) {
        Log.d(andPDFLogsTag, message)
    }
}

internal fun loge(message: String, e: Exception? = null) {
    if (showAndPDFLogs) {
        Log.e(andPDFLogsTag, message, e)
    }
}