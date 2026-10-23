package com.karvo.domain

import java.security.MessageDigest
import java.util.Locale

/**
 * SHA-256 of the Pro code in [canonicalProCode] form; the repo is public, so the code itself is never stored.
 */
const val PRO_CODE_SHA256 = "30c0fbd2e59b2e5458a9232edd9d591066baf4f0c0d0ee5c492256c8b506bd64"

/** Codes look like KARVO-XXXX-XXXX-XXXX: a 5-letter prefix, then groups of 4. */
private const val PREFIX_LENGTH = 5
private const val GROUP_LENGTH = 4
private const val CODE_LENGTH = PREFIX_LENGTH + 3 * GROUP_LENGTH

/** Upper case letters and digits only, with look-alikes merged (0→O, 1→I, 2→Z, 5→S, 8→B) so typos still match. */
fun canonicalProCode(input: String): String =
    input.uppercase(Locale.ROOT).filter { it in 'A'..'Z' || it in '0'..'9' }.map {
        when (it) {
            '0' -> 'O'
            '1' -> 'I'
            '2' -> 'Z'
            '5' -> 'S'
            '8' -> 'B'
            else -> it
        }
    }.joinToString("")

/** True if [input] is the code whose canonical hash is [expectedSha256]; case, spaces and dashes are ignored. */
fun isProCode(input: String, expectedSha256: String = PRO_CODE_SHA256): Boolean {
    val canonical = canonicalProCode(input)
    return canonical.isNotEmpty() && sha256Hex(canonical) == expectedSha256
}

/**
 * Formats typed text as KARVO-XXXX-XXXX-XXXX: upper case, dashes added automatically, extra characters dropped.
 * [previous] is the text before the edit: deleting just a dash deletes the character before it instead.
 */
fun formatProCodeInput(input: String, previous: String = ""): String {
    fun chars(text: String) = text.uppercase(Locale.ROOT).filter { it in 'A'..'Z' || it in '0'..'9' }
    var raw = chars(input).take(CODE_LENGTH)
    if (input.length < previous.length && raw == chars(previous)) raw = raw.dropLast(1)
    return buildString {
        raw.forEachIndexed { i, c ->
            if (i == PREFIX_LENGTH || (i > PREFIX_LENGTH && (i - PREFIX_LENGTH) % GROUP_LENGTH == 0)) append('-')
            append(c)
        }
    }
}

internal fun sha256Hex(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
