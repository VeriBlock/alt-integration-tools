package altchain.network.monitor.tool.util

import java.util.*

inline fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    for ((index, item) in this.withIndex()) {
        if (predicate(item)) {
            return index
        }
    }
    return null
}

fun String.toBase64(): String = Base64.getEncoder().encodeToString(this.toByteArray())

fun String.equalsIgnoreCase(other: String?): Boolean = equals(other, true)

fun Set<String>.toLowerCase(): Set<String> = asSequence().map {
    it.lowercase()
}.toSet()

fun Boolean?.toInt(): Int = if (this == true) 1 else 0
