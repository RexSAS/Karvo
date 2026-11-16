package com.karvo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.widget.RemoteViews
import com.karvo.KarvoApp
import com.karvo.MainActivity
import com.karvo.R
import com.karvo.data.Lesson
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View
import com.karvo.domain.weekRunningTotals
import java.time.DayOfWeek
import com.karvo.domain.chartDurationLabel
import com.karvo.domain.formatDuration
import com.karvo.domain.monthSoFar
import com.karvo.domain.seasonOf
import com.karvo.domain.totalBetween
import com.karvo.domain.weekSoFar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/** 2×2 home-screen widget: this week's hours and lessons (Monday to today), and a + that opens a new lesson. */
class WeekWidget : LessonWidget(Period.WEEK)

/** The same widget for this month (the 1st to today), labelled with the month's name. */
class MonthWidget : LessonWidget(Period.MONTH)

/** 4×2 quick overview: this week big; the month, today and the season small; the next event; the same +. */
class OverviewWidget : LessonWidget(Period.OVERVIEW)

/** 4×2 graph: this week's hours added up day by day against last week's, and how far ahead or behind. */
class CompareWidget : LessonWidget(Period.COMPARE)

enum class Period { WEEK, MONTH, OVERVIEW, COMPARE }

abstract class LessonWidget(private val period: Period) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { render(context, manager, ids, period) } finally { pending.finish() }
        }
    }

    companion object {
        const val EXTRA_ADD_LESSON = "com.karvo.ADD_LESSON"

        /** Debug builds' demo switch (DebugLinkReceiver "widgetDay"): draw as if today were this day. Always null in release. */
        @Volatile var pretendToday: LocalDate? = null

        /** Redraws every placed widget, week, month and overview; called whenever the lessons change. */
        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            for ((provider, period) in listOf(WeekWidget::class.java to Period.WEEK, MonthWidget::class.java to Period.MONTH,
                    OverviewWidget::class.java to Period.OVERVIEW, CompareWidget::class.java to Period.COMPARE)) {
                val ids = manager.getAppWidgetIds(ComponentName(context, provider))
                if (ids.isNotEmpty()) render(context, manager, ids, period)
            }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray, period: Period) {
            val container = (context.applicationContext as KarvoApp).container
            val zone = runCatching { ZoneId.of(container.settingsRepository.timeZoneId.first()) }.getOrDefault(ZoneId.systemDefault())
            val today = pretendToday ?: LocalDate.now(zone)
            val lessons: List<Lesson> = container.lessonRepository.allLessons()
            val weekLabel = context.getString(R.string.widget_week_short)
            val monthLabel = today.month.getDisplayName(TextStyle.FULL_STANDALONE, context.resources.configuration.locales[0])
            val layout = when (period) {
                Period.WEEK -> R.layout.widget_week
                Period.MONTH -> R.layout.widget_month
                Period.OVERVIEW -> R.layout.widget_overview
                Period.COMPARE -> R.layout.widget_compare
            }
            val views = RemoteViews(context.packageName, layout).apply {
                // The overview's week column uses the single widgets' ids; its month column has its own.
                fun fill(labelId: Int, hoursId: Int, subId: Int, label: String, totals: Pair<Long, Int>) {
                    setTextViewText(labelId, label)
                    setTextViewText(hoursId, hoursText(context, totals.first))
                    setTextViewText(subId, context.resources.getQuantityString(R.plurals.lesson_count, totals.second, totals.second))
                }
                if (period == Period.WEEK || period == Period.OVERVIEW) fill(R.id.widget_label, R.id.widget_hours, R.id.widget_sub, weekLabel, weekSoFar(lessons, today))
                when (period) {
                    Period.MONTH -> fill(R.id.widget_label, R.id.widget_hours, R.id.widget_sub, monthLabel, monthSoFar(lessons, today))
                    Period.OVERVIEW -> {
                        val season = seasonOf(today, container.settingsRepository.seasonStartMonth.first()).range().start
                        setTextViewText(R.id.widget_month_label, monthLabel)
                        setTextViewText(R.id.widget_month_hours, chartDurationLabel(monthSoFar(lessons, today).first))
                        setTextViewText(R.id.widget_today_hours, chartDurationLabel(totalBetween(lessons, today, today).first))
                        setTextViewText(R.id.widget_season_hours, chartDurationLabel(totalBetween(lessons, season, today).first))
                        val ahead = container.eventRepository.all().filter { it.date >= today }
                        val next = ahead.firstOrNull()
                        val sameDay = ahead.count { it.date == next?.date }
                        // Several on the next day: "2 events" and the day, rather than one name hiding the others.
                        setTextViewText(R.id.widget_event_text, when {
                            next == null -> context.getString(R.string.widget_no_events)
                            sameDay > 1 -> context.resources.getQuantityString(R.plurals.widget_event_count, sameDay, sameDay)
                            else -> next.text
                        })
                        setTextViewText(R.id.widget_event_date, next?.let { dayLabel(context, it.date, today) } ?: "")
                        setViewVisibility(R.id.widget_event_bar, if (next != null) View.VISIBLE else View.GONE)
                    }
                    Period.COMPARE -> {
                        val now = weekRunningTotals(lessons, today)
                        val last = weekRunningTotals(lessons, today.minusWeeks(1))
                        val todayIndex = today.dayOfWeek.value - 1
                        val diff = now[todayIndex] - last[todayIndex] // against last week by the same weekday
                        setTextViewText(R.id.widget_label, weekLabel)
                        setTextViewText(R.id.widget_hours, hoursText(context, now[todayIndex]))
                        setTextViewText(R.id.widget_delta, when {
                            diff > 0 -> "+" + formatDuration(diff)
                            diff < 0 -> "−" + formatDuration(-diff)
                            else -> "="
                        })
                        setTextColor(R.id.widget_delta, context.getColor(if (diff >= 0) R.color.widget_gold else R.color.widget_mist))
                        // Drawn at the box's real size (portrait: min width × max height, less the header,
                        // initials and padding of widget_compare) so the dot stays round; a guess if unknown.
                        val size = manager.getAppWidgetOptions(ids.first())
                        val boxW = size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 }?.minus(32) ?: 280
                        val boxH = size.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 }?.minus(100) ?: 60
                        setImageViewBitmap(R.id.widget_chart, compareChart(context, now, last, todayIndex, boxW.coerceAtLeast(120), boxH.coerceAtLeast(30)))
                        val locale = context.resources.configuration.locales[0]
                        DAY_IDS.forEachIndexed { i, id ->
                            setTextViewText(id, DayOfWeek.of(i + 1).getDisplayName(TextStyle.NARROW, locale))
                            setTextColor(id, context.getColor(if (i == todayIndex) R.color.widget_gold else R.color.widget_mist))
                        }
                    }
                    Period.WEEK -> Unit
                }
                setOnClickPendingIntent(R.id.widget_root, open(context, add = false))
                if (period != Period.COMPARE) setOnClickPendingIntent(R.id.widget_add, open(context, add = true))
            }
            manager.updateAppWidget(ids, views)
        }

        private val DAY_IDS = listOf(R.id.widget_d0, R.id.widget_d1, R.id.widget_d2, R.id.widget_d3,
            R.id.widget_d4, R.id.widget_d5, R.id.widget_d6)

        /**
         * The compare graph, stretched to fill its box (so no text in it; the day initials are views under it):
         * last week as a faint dashed line over all 7 days, this week as a glacier line with a soft fill up to a
         * gold dot on today. Each day sits at the centre of its seventh, above its initial.
         */
        private fun compareChart(context: Context, now: List<Long>, last: List<Long>, todayIndex: Int, wDp: Int, hDp: Int): Bitmap {
            val d = context.resources.displayMetrics.density
            val w = wDp * d; val h = hDp * d
            val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val top = 8 * d; val bottom = h - 3 * d
            val max = maxOf(now[todayIndex], last.last(), 60L).toFloat()
            fun x(i: Int) = w * (i + .5f) / 7
            fun y(m: Long) = bottom - (bottom - top) * m / max
            fun line(values: List<Long>, upTo: Int) = Path().apply {
                moveTo(x(0), y(values[0])); for (i in 1..upTo) lineTo(x(i), y(values[i]))
            }
            canvas.drawLine(0f, bottom, w, bottom, Paint().apply { color = 0x268CCBFF; strokeWidth = d })
            canvas.drawPath(line(last, 6), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 2 * d; color = 0x80A9B4CC.toInt()
                strokeJoin = Paint.Join.ROUND; pathEffect = DashPathEffect(floatArrayOf(5 * d, 4 * d), 0f)
            })
            val area = line(now, todayIndex).apply { lineTo(x(todayIndex), bottom); lineTo(x(0), bottom); close() }
            canvas.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, top, 0f, bottom, 0x738CCBFF, 0x008CCBFF, Shader.TileMode.CLAMP)
            })
            canvas.drawPath(line(now, todayIndex), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 3 * d; color = 0xFF8CCBFF.toInt()
                strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
            })
            val dot = Paint(Paint.ANTI_ALIAS_FLAG)
            dot.color = 0x40F4C06A; canvas.drawCircle(x(todayIndex), y(now[todayIndex]), 8 * d, dot)
            dot.color = 0xFFF4C06A.toInt(); canvas.drawCircle(x(todayIndex), y(now[todayIndex]), 4.5f * d, dot)
            return bitmap
        }

        /** "Today", "Tomorrow", or a short day like "Thu 20". */
        private fun dayLabel(context: Context, date: LocalDate, today: LocalDate): String = when (date) {
            today -> context.getString(R.string.today)
            today.plusDays(1) -> context.getString(R.string.tomorrow)
            else -> date.format(DateTimeFormatter.ofPattern("EEE d", context.resources.configuration.locales[0]))
        }

        /** "3h 30m" with the h and m smaller and in mist, like the app's big numbers. */
        private fun hoursText(context: Context, minutes: Long): CharSequence {
            val text = if (minutes == 0L) "0h" else formatDuration(minutes)
            val mist = context.getColor(R.color.widget_mist)
            return SpannableStringBuilder(text).apply {
                text.forEachIndexed { i, c ->
                    if (c.isLetter()) {
                        setSpan(RelativeSizeSpan(.5f), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(ForegroundColorSpan(mist), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
        }

        private fun open(context: Context, add: Boolean): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_ADD_LESSON, add)
            return PendingIntent.getActivity(context, if (add) 1 else 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
