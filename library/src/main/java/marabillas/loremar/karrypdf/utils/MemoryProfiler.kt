package marabillas.loremar.karrypdf.utils

internal class MemoryProfiler {
    object Static {
        fun printCurrentMemory() {
            val memory = Runtime.getRuntime().totalMemory()
            println("memory=$memory")
        }
    }
}