package com.masterski.domain

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "October 2026" / "Ottobre 2026": month names are capitalised in every locale. */
fun monthTitle(month: YearMonth, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMMM yyyy", locale).format(month).replaceFirstChar { it.titlecase(locale) }

/** Three-letter chart label: "Oct" / "Ott". */
fun shortMonthLabel(month: YearMonth, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMM", locale).format(month).removeSuffix(".").replaceFirstChar { it.titlecase(locale) }
