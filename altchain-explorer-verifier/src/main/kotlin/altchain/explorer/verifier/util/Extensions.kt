package altchain.explorer.verifier.util

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

fun String.equalsIgnoreCase(other: String?) = equals(other, true)