package com.karvo.link

import com.karvo.data.Lesson
import com.karvo.testing.FakeLessonRepository
import com.karvo.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class LinkCommandHandlerTest {
    private val anna = Lesson(id = 1, date = LocalDate.parse("2026-01-05"), durationMinutes = 60, student = "Anna")
    private val lessons = FakeLessonRepository(listOf(anna))
    private val settings = FakeSettingsRepository(initial = 3000L, zone = "Europe/Rome")
    private val notes = com.karvo.testing.FakeDayNoteRepository()
    private val handler = LinkCommandHandler(lessons, settings, notes, messages = { it.name }, refusal = { it.name })

    private fun lessonJson(id: Long = 0, date: String = "2026-01-06", minutes: Int = 90, student: String = "Bo", paidOn: String? = null) =
        JSONObject().put("id", id).put("date", date).put("durationMinutes", minutes).put("student", student)
            .put("notes", JSONObject.NULL).put("paidOn", paidOn ?: JSONObject.NULL)
    private suspend fun refused(op: String, args: JSONObject) =
        runCatching { handler.handle(op, args) }.exceptionOrNull().let { (it as CommandRefused).message }

    @Test fun `add, update, delete`() = runTest {
        handler.handle("addLesson", JSONObject().put("lesson", lessonJson()))
        val bo = lessons.allLessons().single { it.student == "Bo" }
        handler.handle("updateLesson", JSONObject().put("lesson", lessonJson(id = bo.id, minutes = 120)))
        assertEquals(120, lessons.get(bo.id)!!.durationMinutes)
        handler.handle("deleteLesson", JSONObject().put("id", bo.id))
        assertNull(lessons.get(bo.id))
    }

    @Test fun `invalid lessons are refused with the app's own messages`() = runTest {
        assertEquals("DurationZero · StudentBlank", refused("addLesson", JSONObject().put("lesson", lessonJson(minutes = 0, student = " "))))
        assertEquals("DurationTooLong", refused("addLesson", JSONObject().put("lesson", lessonJson(minutes = 1441))))
        assertEquals("InvalidData", refused("addLesson", JSONObject().put("lesson", lessonJson(date = "2026-13-01"))))
        assertEquals(1, lessons.allLessons().size)
    }

    @Test fun `a lesson that no longer exists is refused`() = runTest {
        assertEquals("LessonGone", refused("updateLesson", JSONObject().put("lesson", lessonJson(id = 99))))
        assertEquals("LessonGone", refused("deleteLesson", JSONObject().put("id", 99)))
    }

    @Test fun `months, rate and season start`() = runTest {
        handler.handle("markPaid", JSONObject().put("month", "2026-01").put("paidOn", "2026-02-01"))
        assertEquals(LocalDate.parse("2026-02-01"), lessons.get(1)!!.paidOn)
        handler.handle("markUnpaid", JSONObject().put("month", "2026-01"))
        assertNull(lessons.get(1)!!.paidOn)
        handler.handle("setRate", JSONObject().put("rateCents", 2750))
        assertEquals(2750L, settings.hourlyRateCents.first())
        handler.handle("setRate", JSONObject().put("rateCents", JSONObject.NULL))
        assertNull(settings.hourlyRateCents.first())
        assertEquals("InvalidRate", refused("setRate", JSONObject().put("rateCents", 0)))
        handler.handle("setSeasonStart", JSONObject().put("month", 12))
        assertEquals(12, settings.seasonStartMonth.first())
        assertEquals("InvalidMonth", refused("setSeasonStart", JSONObject().put("month", 13)))
    }

    @Test fun `import skips duplicates and reports counts`() = runTest {
        val args = JSONObject().put("lessons", JSONArray().put(lessonJson(date = "2026-01-05", minutes = 60, student = " anna")).put(lessonJson()))
        assertEquals("""{"added":1,"skipped":1}""", handler.handle("importLessons", args).toString())
        assertEquals("""{"added":0,"skipped":2}""", handler.handle("importLessons", args).toString())
    }

    @Test fun `restore only into an empty phone`() = runTest {
        val args = JSONObject().put("lessons", JSONArray().put(lessonJson()))
        assertEquals("PhoneNotEmpty", refused("restoreAll", args))
        val empty = FakeLessonRepository()
        LinkCommandHandler(empty, settings, com.karvo.testing.FakeDayNoteRepository(), { it.name }, { it.name }).handle("restoreAll", args)
        assertEquals(listOf("Bo"), empty.allLessons().map { it.student })
    }

    @Test fun `unknown commands are refused`() = runTest {
        assertEquals("UnknownCommand", refused("format", JSONObject()))
    }

    @Test fun `snapshots carry lessons, rate, season start and time zone, and follow changes`() = runTest {
        val first = handler.snapshots().first()
        assertEquals(1, first.getJSONArray("lessons").length())
        assertEquals(3000, first.getLong("rateCents"))
        assertEquals("Europe/Rome", first.getString("timeZoneId"))
        assertEquals(11, first.getInt("seasonStartMonth"))
        val l = first.getJSONArray("lessons").getJSONObject(0)
        assertEquals("2026-01-05", l.getString("date")); assertTrue(l.isNull("paidOn")); assertEquals(1L, l.getLong("id"))
    }

    @Test fun `a phone edit and a PC edit of the same lesson, the last write is the truth`() = runTest {
        lessons.save(anna.copy(durationMinutes = 30))
        handler.handle("updateLesson", JSONObject().put("lesson", lessonJson(id = 1, date = "2026-01-05", minutes = 45, student = "Anna")))
        assertEquals(45, handler.snapshots().first().getJSONArray("lessons").getJSONObject(0).getInt("durationMinutes"))
        lessons.save(lessons.get(1)!!.copy(durationMinutes = 50))
        assertEquals(50, handler.snapshots().first().getJSONArray("lessons").getJSONObject(0).getInt("durationMinutes"))
    }

    @Test fun `day notes are set, replaced, deleted and sent in the snapshot`() = runTest {
        handler.handle("setDayNote", JSONObject().put("date", "2026-11-10").put("text", "  Meeting 17:00 "))
        assertEquals("Meeting 17:00", notes.notes.value[LocalDate.parse("2026-11-10")])
        assertEquals("Meeting 17:00", handler.snapshots().first().getJSONObject("dayNotes").getString("2026-11-10"))
        handler.handle("setDayNote", JSONObject().put("date", "2026-11-10").put("text", JSONObject.NULL))
        assertTrue(notes.notes.value.isEmpty())
        assertEquals("InvalidData", refused("setDayNote", JSONObject().put("date", "2026-02-30").put("text", "x")))
        assertEquals("InvalidData", refused("setDayNote", JSONObject().put("date", "2026-11-10").put("text", "x".repeat(201))))
        assertEquals("InvalidData", refused("setDayNote", JSONObject().put("date", "2026-11-10").put("text", 5)))
    }
}
