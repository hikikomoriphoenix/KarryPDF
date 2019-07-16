package marabillas.loremar.andpdf.utils

internal class MemoryProfiler {
    object Static {
        fun printCurrentMemory() {
            val memory = Runtime.getRuntime().totalMemory()
            println("memory=$memory")
        }
    }
}