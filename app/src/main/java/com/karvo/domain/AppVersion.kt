package com.karvo.domain

/** A dotted numeric version such as 1.2.0; missing parts compare as zero, so 1.2 == 1.2.0. */
data class AppVersion(val parts: List<Int>) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val diff = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    override fun toString(): String = parts.joinToString(".")
}

private val VERSION = Regex("""v?(\d+(?:\.\d+){0,3})""")

/** Parses "v1.2.0" / "1.2"; anything else (e.g. "1.2-beta") is null. */
fun parseVersion(text: String): AppVersion? =
    VERSION.matchEntire(text.trim())?.groupValues?.get(1)?.split('.')?.map { it.toIntOrNull() ?: return null }?.let(::AppVersion)
