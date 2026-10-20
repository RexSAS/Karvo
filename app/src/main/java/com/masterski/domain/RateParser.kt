package com.masterski.domain

import java.math.BigDecimal

sealed interface RateParseResult {
    data object Cleared : RateParseResult
    data class Valid(val cents: Long) : RateParseResult
    data object Invalid : RateParseResult
}

private val RATE_PATTERN = Regex("""^\d{1,5}(\.\d{1,2})?$""")

fun normalizeRateInput(input: String): String = input.replace(',', '.')

fun formatRateText(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()

fun parseRateCents(input: String): RateParseResult {
    val text = normalizeRateInput(input.trim().removeSuffix("€").trim())
        .removeSuffix(".-")
    if (text.isEmpty()) return RateParseResult.Cleared
    if (!RATE_PATTERN.matches(text)) return RateParseResult.Invalid
    val cents = BigDecimal(text).movePointRight(2).toLong()
    return if (cents == 0L) RateParseResult.Invalid else RateParseResult.Valid(cents)
}
