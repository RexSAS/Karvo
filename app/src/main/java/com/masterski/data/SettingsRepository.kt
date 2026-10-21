package com.masterski.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DateTimeException
import java.time.ZoneId

const val DEFAULT_TIME_ZONE = "Europe/Rome"

/** True if [id] is a valid [ZoneId]; shared by all settings implementations. */
fun isValidZoneId(id: String): Boolean =
    try {
        ZoneId.of(id)
        true
    } catch (_: DateTimeException) {
        false
    }

interface SettingsRepository {
    val hourlyRateCents: Flow<Long?>
    suspend fun setHourlyRateCents(cents: Long?)
    val timeZoneId: Flow<String>
    suspend fun setTimeZoneId(id: String)
    val theme: Flow<ThemeChoice>
    suspend fun setTheme(choice: ThemeChoice)
}

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    private val rateKey = longPreferencesKey("hourly_rate_cents")
    private val zoneKey = stringPreferencesKey("time_zone_id")
    private val themeKey = stringPreferencesKey("theme")

    override val hourlyRateCents: Flow<Long?> = store.data.map { it[rateKey] }

    override suspend fun setHourlyRateCents(cents: Long?) {
        store.edit { if (cents == null) it.remove(rateKey) else it[rateKey] = cents }
    }

    override val timeZoneId: Flow<String> = store.data.map { prefs ->
        prefs[zoneKey]?.takeIf { isValidZoneId(it) } ?: DEFAULT_TIME_ZONE
    }

    override suspend fun setTimeZoneId(id: String) {
        require(isValidZoneId(id)) { "Invalid time zone: $id" }
        store.edit { it[zoneKey] = id }
    }

    override val theme: Flow<ThemeChoice> = store.data.map { themeChoiceOf(it[themeKey]) }

    override suspend fun setTheme(choice: ThemeChoice) {
        store.edit { it[themeKey] = choice.name }
    }
}
