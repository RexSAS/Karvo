package com.masterski.testing

import com.masterski.data.DEFAULT_TIME_ZONE
import com.masterski.data.SettingsRepository
import com.masterski.data.isValidZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initial: Long? = null,
    zone: String = DEFAULT_TIME_ZONE,
) : SettingsRepository {
    private val rate = MutableStateFlow(initial)
    private val zoneId = MutableStateFlow(zone)

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
}
