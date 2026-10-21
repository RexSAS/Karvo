package com.karvo.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), KarvoDatabase::class.java)

    @Test fun v1LessonsSurviveAsUnpaid() {
        helper.createDatabase("m.db", 1).apply {
            execSQL(
                "INSERT INTO lessons (id, date, durationMinutes, student, notes) " +
                    "VALUES (1, ${LocalDate.of(2026, 12, 3).toEpochDay()}, 90, 'Rossi', NULL)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate("m.db", 2, true, MIGRATION_1_2)
        db.query("SELECT student, durationMinutes, paidOn FROM lessons").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Rossi", c.getString(0))
            assertEquals(90, c.getInt(1))
            assertTrue(c.isNull(2))
        }
    }
}
