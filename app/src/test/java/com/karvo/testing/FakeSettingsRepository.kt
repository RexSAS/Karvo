package com.karvo.testing

import com.karvo.data.DEFAULT_SEASON_START_MONTH
import com.karvo.data.DEFAULT_TIME_ZONE
import com.karvo.data.SettingsRepository
import com.karvo.data.ThemeChoice
import com.karvo.data.isValidZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initial: Long? = null,
    zone: String = DEFAULT_TIME_ZONE,
    theme: ThemeChoice = ThemeChoice.ALPINE_NIGHT,
    seasonStart: Int = DEFAULT_SEASON_START_MONTH,
) : SettingsRepository {
    private val seasonStartValue = MutableStateFlow(seasonStart)
    private val rate = MutableStateFlow(initial)
    private val zoneId = MutableStateFlow(zone)
    private val themeChoice = MutableStateFlow(theme)

    override val hourlyRateCents: Flow<Long?> = rate
    /** Number of setHourlyRateCents calls, to assert idempotent saves. */
    var rateWrites = 0
        private set

    override suspend fun setHourlyRateCents(cents: Long?) {
        rateWrites++
        rate.value = cents
    }

    override val timeZoneId: Flow<String> = zoneId
    override suspend fun setTimeZoneId(id: String) {
        require(isValidZoneId(id)) { "Invalid time zone: $id" }
        zoneId.value = id
    }

    override val theme: Flow<ThemeChoice> = themeChoice
    override suspend fun setTheme(choice: ThemeChoice) {
        themeChoice.value = choice
    }

    override val seasonStartMonth: Flow<Int> = seasonStartValue
    override suspend fun setSeasonStartMonth(month: Int) {
        require(month in 1..12) { "Invalid month: $month" }
        seasonStartValue.value = month
    }

    private val biometricValue = MutableStateFlow(true)
    override val biometricReveal: Flow<Boolean> = biometricValue
    override suspend fun setBiometricReveal(on: Boolean) {
        biometricValue.value = on
    }
}
