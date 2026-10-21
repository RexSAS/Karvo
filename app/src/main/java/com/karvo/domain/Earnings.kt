package com.karvo.domain

/** Half-up rounded earnings in cents, computed from summed minutes. */
fun earningsCents(totalMinutes: Long, hourlyRateCents: Long): Long =
    (totalMinutes * hourlyRateCents + 30) / 60
