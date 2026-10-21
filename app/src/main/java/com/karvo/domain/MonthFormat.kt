package com.karvo.domain

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "October 2026" / "Ottobre 2026": month names are capitalised in every locale. */
fun monthTitle(month: YearMonth, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMMM yyyy", locale).format(month).replaceFirstChar { it.titlecase(locale) }

/** Three-letter chart label: "Oct" / "Ott". */
fun shortMonthLabel(month: YearMonth, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMM", locale).format(month).removeSuffix(".").replaceFirstChar { it.titlecase(locale) }

/** Screen-header date: "Wednesday 21 October" / "Mercoledì 21 ottobre" (first letter capitalised). */
fun dayTitle(date: java.time.LocalDate, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("EEEE d MMMM", locale).format(date).replaceFirstChar { it.titlecase(locale) }
