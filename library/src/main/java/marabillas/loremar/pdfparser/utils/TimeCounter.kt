package marabillas.loremar.pdfparser.utils

import java.util.concurrent.TimeUnit

internal class TimeCounter {
    init {
        reset()
    }

    companion object {
        private var t0 = System.nanoTime()

        fun reset() {
            t0 = System.nanoTime()
        }

        fun getTimeElapsed(): Long {
            val t1 = System.nanoTime()
            val dt = t1 - t0
            return TimeUnit.NANOSECONDS.toMillis(dt)
        }
    }

    fun reset() {
        Companion.reset()
    }

    fun getTimeElapsed(): Long {
        return Companion.getTimeElapsed()
    }
}