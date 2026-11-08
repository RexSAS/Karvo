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
    private val events = com.karvo.testing.FakeEventRepository()
    private val handler = LinkCommandHandler(lessons, settings, events, messages = { it.name }, refusal = { it.name })

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
        LinkCommandHandler(empty, settings, com.karvo.testing.FakeEventRepository(), { it.name }, { it.name }).handle("restoreAll", args)
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

    @Test fun `events are added, updated, deleted and sent in the snapshot`() = runTest {
        handler.handle("addEvent", JSONObject().put("event", JSONObject().put("date", "2026-11-10").put("text", "  Meeting 17:00 ")))
        handler.handle("addEvent", JSONObject().put("event", JSONObject().put("date", "2026-11-10").put("text", "Dinner")))
        assertEquals(listOf("Meeting 17:00", "Dinner"), events.events.value.map { it.text })
        val snap = handler.snapshots().first().getJSONArray("events")
        assertEquals(2, snap.length()); assertEquals("2026-11-10", snap.getJSONObject(0).getString("date"))
        handler.handle("updateEvent", JSONObject().put("event", JSONObject().put("id", 2).put("date", "2026-11-11").put("text", "Dinner moved")))
        assertEquals(LocalDate.parse("2026-11-11"), events.events.value[1].date)
        handler.handle("deleteEvent", JSONObject().put("id", 1))
        assertEquals(listOf(2L), events.events.value.map { it.id })
        assertEquals("EventGone", refused("deleteEvent", JSONObject().put("id", 9)))
        assertEquals("EventGone", refused("updateEvent", JSONObject().put("event", JSONObject().put("id", 9).put("date", "2026-11-11").put("text", "x"))))
        assertEquals("InvalidData", refused("addEvent", JSONObject().put("event", JSONObject().put("date", "2026-02-30").put("text", "x"))))
        assertEquals("InvalidData", refused("addEvent", JSONObject().put("event", JSONObject().put("date", "2026-11-10").put("text", " "))))
        assertEquals("InvalidData", refused("addEvent", JSONObject().put("event", JSONObject().put("date", "2026-11-10").put("text", "x".repeat(201)))))
    }

    @Test fun `restore into an empty phone also brings the PC events, without duplicates`() = runTest {
        val empty = FakeLessonRepository()
        val ev = com.karvo.testing.FakeEventRepository(listOf(com.karvo.data.Event(1, LocalDate.parse("2026-11-12"), "phone")))
        val h = LinkCommandHandler(empty, settings, ev, { it.name }, { it.name })
        h.handle("restoreAll", JSONObject().put("lessons", JSONArray().put(lessonJson())).put("events", JSONArray()
            .put(JSONObject().put("date", "2026-11-10").put("text", "Meeting")).put(JSONObject().put("date", "2026-11-12").put("text", "phone"))))
        assertEquals(listOf("phone", "Meeting"), ev.events.value.map { it.text })
        assertEquals("InvalidData", runCatching { h.handle("restoreAll", JSONObject().put("lessons", JSONArray()).put("events", JSONArray().put(JSONObject().put("date", "nope").put("text", "x")))) }.exceptionOrNull()?.message)
    }
}
