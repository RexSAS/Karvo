package com.karvo.testing

import com.karvo.data.Rate
import com.karvo.domain.Pay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** One default rate of [cents] (id 1), or no rates for null: what a single hourly rate used to be. */
fun ratesOf(cents: Long?): List<Rate> = listOfNotNull(cents?.let { Rate(id = 1, title = "Standard", cents = it, color = 0, isDefault = true) })

fun payOf(cents: Long?): Pay = Pay(ratesOf(cents))

fun rateFlow(cents: Long?): Flow<List<Rate>> = MutableStateFlow(ratesOf(cents))
