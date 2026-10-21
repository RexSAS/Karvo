package com.karvo.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material3 DatePicker works in UTC-midnight millis. */
fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun pickerMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
