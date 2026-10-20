package com.masterski.domain

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatDuration(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

fun formatEuro(cents: Long, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getCurrencyInstance(locale)
        .apply { currency = Currency.getInstance("EUR") }
        .format(BigDecimal.valueOf(cents, 2))

fun earningsLabel(cents: Long?, revealed: Boolean, locale: Locale = Locale.getDefault()): String = when {
    cents == null -> "— €"
    !revealed -> "**** €"
    else -> formatEuro(cents, locale)
}
