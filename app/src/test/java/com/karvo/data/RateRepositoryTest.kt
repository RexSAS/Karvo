package com.karvo.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.karvo.domain.Pay
import com.karvo.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Against real Room SQL: the default rules, and lessons moving when the default changes or a rate goes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RateRepositoryTest {
    private lateinit var db: KarvoDatabase
    private lateinit var rates: RateRepository
    private lateinit var lessons: LessonDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), KarvoDatabase::class.java)
            .allowMainThreadQueries().build()
        rates = RateRepository(db.rateDao())
        lessons = db.lessonDao()
    }

    @After fun tearDown() = db.close()

    private suspend fun lesson(rateId: Long?) = lessons.insert(Lesson(date = LocalDate.of(2026, 12, 1), durationMinutes = 60, student = "A", rateId = rateId))
    private suspend fun rateOf(id: Long) = lessons.getById(id)!!.rateId
    private suspend fun id(title: String) = rates.all().single { it.title == title }.id

    @Test fun firstRateIsTheDefaultAndTheDefaultIsListedFirst() = runTest {
        assertTrue(rates.save(Rate(title = " Standard ", cents = 3000, color = 1)))
        rates.save(Rate(title = "Private", cents = 4500, color = 2))
        assertEquals(listOf("Standard" to true, "Private" to false), rates.observe().first().map { it.title to it.isDefault })
        assertFalse(rates.save(Rate(title = " ", cents = 100, color = 1)))
        assertFalse(rates.save(Rate(title = "Zero", cents = 0, color = 1)))
        assertFalse(rates.save(Rate(title = " private ", cents = 100, color = 1))) // the name is taken
        assertTrue(rates.save(rates.all().single { it.title == "Private" }.copy(title = "PRIVATE"))) // its own name
    }

    @Test fun changingTheDefaultKeepsWhatOldLessonsEarn() = runTest {
        rates.save(Rate(title = "Standard", cents = 3000, color = 1))
        rates.save(Rate(title = "Private", cents = 4500, color = 2))
        val old = lesson(null) // from before rates: on the default
        rates.setDefault(id("Private"))
        assertEquals(id("Standard"), rateOf(old))
        assertEquals("Private", rates.all().first().title)
        assertEquals(1, rates.all().count { it.isDefault })
    }

    @Test fun deletingARateMovesItsLessonsToTheDefault() = runTest {
        rates.save(Rate(title = "Standard", cents = 3000, color = 1))
        rates.save(Rate(title = "Private", cents = 4500, color = 2))
        val l = lesson(id("Private"))
        assertFalse(rates.delete(id("Standard"))) // the default, while another rate exists
        assertTrue(rates.delete(id("Private")))
        assertEquals(id("Standard"), rateOf(l))
        assertTrue(rates.delete(id("Standard"))) // the last one
        assertNull(rateOf(l))
        assertFalse(Pay(rates.all()).isSet)
    }

    @Test fun editKeepsTheDefaultFlag() = runTest {
        rates.save(Rate(title = "Standard", cents = 3000, color = 1))
        rates.save(rates.all().single().copy(cents = 3200, isDefault = false))
        assertEquals(3200L to true, rates.all().single().let { it.cents to it.isDefault })
    }

    @Test fun theOldSingleRateBecomesTheDefaultOnce() = runTest {
        val settings = FakeSettingsRepository(2750)
        rates.adoptLegacyRate(settings, "Standard")
        rates.adoptLegacyRate(settings, "Standard")
        assertEquals(listOf(Triple("Standard", 2750L, true)), rates.all().map { Triple(it.title, it.cents, it.isDefault) })
        assertNull(settings.hourlyRateCents.first())
    }
}
