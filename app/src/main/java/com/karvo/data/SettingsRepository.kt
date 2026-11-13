package com.karvo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DateTimeException
import java.time.ZoneId

const val DEFAULT_TIME_ZONE = "Europe/Rome"

/** Ski seasons start in December unless the user picks another month. */
const val DEFAULT_SEASON_START_MONTH = 12

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
    /** 1–12; stored values out of range read as [DEFAULT_SEASON_START_MONTH]. */
    val seasonStartMonth: Flow<Int>
    suspend fun setSeasonStartMonth(month: Int)
    /** Ask for fingerprint/PIN before showing earnings; on by default. */
    /** The app version last opened (null until 3.2 stores it): drives Welcome / What's new. */
    val lastSeenVersion: Flow<String?>
    suspend fun setLastSeenVersion(version: String)
    /** Start year of the last season the new-season card was shown for (or skipped in); null until first stored. */
    val welcomedSeason: Flow<Int?>
    suspend fun setWelcomedSeason(startYear: Int)

    val biometricReveal: Flow<Boolean>
    suspend fun setBiometricReveal(on: Boolean)
}

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    private val rateKey = longPreferencesKey("hourly_rate_cents")
    private val zoneKey = stringPreferencesKey("time_zone_id")
    private val themeKey = stringPreferencesKey("theme")
    private val seasonStartKey = intPreferencesKey("season_start_month")
    private val biometricKey = booleanPreferencesKey("biometric_reveal")

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

    override val seasonStartMonth: Flow<Int> = store.data.map { prefs ->
        prefs[seasonStartKey]?.takeIf { it in 1..12 } ?: DEFAULT_SEASON_START_MONTH
    }

    override suspend fun setSeasonStartMonth(month: Int) {
        require(month in 1..12) { "Invalid month: $month" }
        store.edit { it[seasonStartKey] = month }
    }

    private val lastSeenKey = stringPreferencesKey("last_seen_version")
    override val lastSeenVersion: Flow<String?> = store.data.map { it[lastSeenKey] }
    override suspend fun setLastSeenVersion(version: String) { store.edit { it[lastSeenKey] = version } }
    private val welcomedSeasonKey = intPreferencesKey("welcomed_season")
    override val welcomedSeason: Flow<Int?> = store.data.map { it[welcomedSeasonKey] }
    override suspend fun setWelcomedSeason(startYear: Int) { store.edit { it[welcomedSeasonKey] = startYear } }

    override val biometricReveal: Flow<Boolean> = store.data.map { it[biometricKey] ?: true }

    override suspend fun setBiometricReveal(on: Boolean) {
        store.edit { it[biometricKey] = on }
    }
}
