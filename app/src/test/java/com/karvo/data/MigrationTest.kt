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

    @Test fun v2LessonsSurviveAndDayNotesAppear() {
        helper.createDatabase("m3.db", 2).apply {
            execSQL("INSERT INTO lessons (id, date, durationMinutes, student, notes, paidOn) VALUES (1, ${LocalDate.of(2026, 12, 3).toEpochDay()}, 90, 'Rossi', NULL, NULL)")
            close()
        }
        val db = helper.runMigrationsAndValidate("m3.db", 3, true, MIGRATION_2_3)
        db.query("SELECT student FROM lessons").use { c -> assertTrue(c.moveToFirst()); assertEquals("Rossi", c.getString(0)) }
        db.execSQL("INSERT INTO day_notes (date, text) VALUES (${LocalDate.of(2026, 12, 4).toEpochDay()}, 'Meeting')")
        db.query("SELECT text FROM day_notes").use { c -> assertTrue(c.moveToFirst()); assertEquals("Meeting", c.getString(0)) }
    }

    @Test fun v3NotesBecomeEventsAnd3_1DatabasesGoStraightToEvents() {
        helper.createDatabase("m4.db", 3).apply {
            execSQL("INSERT INTO lessons (id, date, durationMinutes, student, notes, paidOn) VALUES (1, ${LocalDate.of(2026, 12, 3).toEpochDay()}, 90, 'Rossi', NULL, NULL)")
            execSQL("INSERT INTO day_notes (date, text) VALUES (${LocalDate.of(2026, 12, 4).toEpochDay()}, 'Meeting')")
            close()
        }
        val db = helper.runMigrationsAndValidate("m4.db", 4, true, MIGRATION_3_4)
        db.query("SELECT date, text FROM events").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(LocalDate.of(2026, 12, 4).toEpochDay(), c.getLong(0)); assertEquals("Meeting", c.getString(1))
        }
        db.query("SELECT student FROM lessons").use { c -> assertTrue(c.moveToFirst()); assertEquals("Rossi", c.getString(0)) }
        helper.createDatabase("m24.db", 2).close()
        helper.runMigrationsAndValidate("m24.db", 4, true, MIGRATION_2_3, MIGRATION_3_4)
    }

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
