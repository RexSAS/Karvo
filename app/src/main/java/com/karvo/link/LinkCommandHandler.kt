package com.karvo.link

import com.karvo.data.Lesson
import com.karvo.data.LessonRepository
import com.karvo.data.SettingsRepository
import com.karvo.domain.LessonError
import com.karvo.domain.planImport
import com.karvo.domain.validateLesson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

/** Why the phone refused a PC command (shown on the PC in the phone's language). */
enum class Refusal { LessonGone, PhoneNotEmpty, InvalidRate, InvalidMonth, InvalidData, UnknownCommand }

class CommandRefused(message: String) : Exception(message)

fun Lesson.toJson(): JSONObject = JSONObject()
    .put("id", id).put("date", date.toString()).put("durationMinutes", durationMinutes).put("student", student)
    .put("notes", notes ?: JSONObject.NULL).put("paidOn", paidOn?.toString() ?: JSONObject.NULL)

/**
 * Applies PC commands (spec "Messages") with the same rules as the app's own screens, and produces the
 * snapshots the PC shows. Every refusal throws [CommandRefused] with a message for the PC.
 */
class LinkCommandHandler(
    private val lessons: LessonRepository,
    private val settings: SettingsRepository,
    private val messages: (LessonError) -> String,
    private val refusal: (Refusal) -> String,
) {
    private fun refuse(r: Refusal): Nothing = throw CommandRefused(refusal(r))

    private fun lessonFrom(o: JSONObject?): Lesson {
        val l = try {
            o!!
            Lesson(
                id = o.optLong("id", 0),
                date = LocalDate.parse(o.getString("date")),
                durationMinutes = o.getInt("durationMinutes"),
                student = o.getString("student"),
                notes = if (o.isNull("notes")) null else o.getString("notes"),
                paidOn = if (o.isNull("paidOn")) null else LocalDate.parse(o.getString("paidOn")),
            )
        } catch (e: Exception) { refuse(Refusal.InvalidData) }
        if (l.durationMinutes < 0) refuse(Refusal.InvalidData)
        val errors = validateLesson(l.student, l.durationMinutes / 60, l.durationMinutes % 60)
        if (errors.isNotEmpty()) throw CommandRefused(errors.joinToString(" · ") { messages(it) })
        return l
    }

    private fun lessonsFrom(a: JSONArray?): List<Lesson> =
        (0 until (a ?: refuse(Refusal.InvalidData)).length()).map { lessonFrom(a.optJSONObject(it)) }

    private fun month(s: String?): YearMonth = runCatching { YearMonth.parse(s) }.getOrElse { refuse(Refusal.InvalidData) }

    /** Returns the ack's `result` (only importLessons has one). */
    suspend fun handle(op: String, args: JSONObject): JSONObject? {
        when (op) {
            "addLesson" -> lessons.save(lessonFrom(args.optJSONObject("lesson")).copy(id = 0))
            "updateLesson" -> {
                val l = lessonFrom(args.optJSONObject("lesson"))
                if (l.id == 0L || lessons.get(l.id) == null) refuse(Refusal.LessonGone)
                lessons.save(l)
            }
            "deleteLesson" -> lessons.delete(lessons.get(args.optLong("id", -1)) ?: refuse(Refusal.LessonGone))
            "markPaid" -> lessons.markMonthPaid(month(args.optString("month")),
                runCatching { LocalDate.parse(args.optString("paidOn")) }.getOrElse { refuse(Refusal.InvalidData) })
            "markUnpaid" -> lessons.markMonthUnpaid(month(args.optString("month")))
            "setRate" -> {
                val cents = if (args.isNull("rateCents")) null else args.optLong("rateCents", 0)
                if (cents != null && cents <= 0) refuse(Refusal.InvalidRate)
                settings.setHourlyRateCents(cents)
            }
            "setSeasonStart" -> {
                val m = args.optInt("month", 0)
                if (m !in 1..12) refuse(Refusal.InvalidMonth)
                settings.setSeasonStartMonth(m)
            }
            "importLessons" -> {
                val plan = planImport(lessons.allLessons(), lessonsFrom(args.optJSONArray("lessons")))
                lessons.addAll(plan.toAdd)
                return JSONObject().put("added", plan.toAdd.size).put("skipped", plan.skipped)
            }
            "restoreAll" -> {
                val incoming = lessonsFrom(args.optJSONArray("lessons"))
                if (lessons.allLessons().isNotEmpty()) refuse(Refusal.PhoneNotEmpty)
                lessons.addAll(incoming)
            }
            else -> refuse(Refusal.UnknownCommand)
        }
        return null
    }

    /** The full data the PC shows, again after every change from either side. */
    fun snapshots(): Flow<JSONObject> = combine(
        lessons.observeLessons(), settings.hourlyRateCents, settings.seasonStartMonth, settings.timeZoneId,
    ) { all, rate, seasonStart, zone ->
        JSONObject().put("lessons", JSONArray(all.map { it.toJson() })).put("rateCents", rate ?: JSONObject.NULL)
            .put("seasonStartMonth", seasonStart).put("timeZoneId", zone)
    }
}
