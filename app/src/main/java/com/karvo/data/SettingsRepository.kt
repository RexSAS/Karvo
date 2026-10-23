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

/** Ski seasons start in November unless the user picks another month. */
const val DEFAULT_SEASON_START_MONTH = 11

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
    val biometricReveal: Flow<Boolean>
    suspend fun setBiometricReveal(on: Boolean)
    /** Unlocked by a redeem code; off by default. */
    val pro: Flow<Boolean>
    suspend fun setPro(on: Boolean)
}

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    private val rateKey = longPreferencesKey("hourly_rate_cents")
    private val zoneKey = stringPreferencesKey("time_zone_id")
    private val themeKey = stringPreferencesKey("theme")
    private val seasonStartKey = intPreferencesKey("season_start_month")
    private val biometricKey = booleanPreferencesKey("biometric_reveal")
    private val proKey = booleanPreferencesKey("pro_unlocked")

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

    override val biometricReveal: Flow<Boolean> = store.data.map { it[biometricKey] ?: true }

    override suspend fun setBiometricReveal(on: Boolean) {
        store.edit { it[biometricKey] = on }
    }

    override val pro: Flow<Boolean> = store.data.map { it[proKey] ?: false }

    override suspend fun setPro(on: Boolean) {
        store.edit { it[proKey] = on }
    }
}
