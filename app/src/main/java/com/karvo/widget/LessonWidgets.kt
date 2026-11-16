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
import android.view.View
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

enum class Period { WEEK, MONTH, OVERVIEW }

abstract class LessonWidget(private val period: Period) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { render(context, manager, ids, period) } finally { pending.finish() }
        }
    }

    companion object {
        const val EXTRA_ADD_LESSON = "com.karvo.ADD_LESSON"

        /** Redraws every placed widget, week, month and overview; called whenever the lessons change. */
        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            for ((provider, period) in listOf(WeekWidget::class.java to Period.WEEK, MonthWidget::class.java to Period.MONTH,
                    OverviewWidget::class.java to Period.OVERVIEW)) {
                val ids = manager.getAppWidgetIds(ComponentName(context, provider))
                if (ids.isNotEmpty()) render(context, manager, ids, period)
            }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray, period: Period) {
            val container = (context.applicationContext as KarvoApp).container
            val zone = runCatching { ZoneId.of(container.settingsRepository.timeZoneId.first()) }.getOrDefault(ZoneId.systemDefault())
            val today = LocalDate.now(zone)
            val lessons: List<Lesson> = container.lessonRepository.allLessons()
            val weekLabel = context.getString(R.string.widget_week_short)
            val monthLabel = today.month.getDisplayName(TextStyle.FULL_STANDALONE, context.resources.configuration.locales[0])
            val layout = when (period) {
                Period.WEEK -> R.layout.widget_week
                Period.MONTH -> R.layout.widget_month
                Period.OVERVIEW -> R.layout.widget_overview
            }
            val views = RemoteViews(context.packageName, layout).apply {
                // The overview's week column uses the single widgets' ids; its month column has its own.
                fun fill(labelId: Int, hoursId: Int, subId: Int, label: String, totals: Pair<Long, Int>) {
                    setTextViewText(labelId, label)
                    setTextViewText(hoursId, hoursText(context, totals.first))
                    setTextViewText(subId, context.resources.getQuantityString(R.plurals.lesson_count, totals.second, totals.second))
                }
                if (period != Period.MONTH) fill(R.id.widget_label, R.id.widget_hours, R.id.widget_sub, weekLabel, weekSoFar(lessons, today))
                when (period) {
                    Period.MONTH -> fill(R.id.widget_label, R.id.widget_hours, R.id.widget_sub, monthLabel, monthSoFar(lessons, today))
                    Period.OVERVIEW -> {
                        val season = seasonOf(today, container.settingsRepository.seasonStartMonth.first()).range().start
                        setTextViewText(R.id.widget_month_label, monthLabel)
                        setTextViewText(R.id.widget_month_hours, chartDurationLabel(monthSoFar(lessons, today).first))
                        setTextViewText(R.id.widget_today_hours, chartDurationLabel(totalBetween(lessons, today, today).first))
                        setTextViewText(R.id.widget_season_hours, chartDurationLabel(totalBetween(lessons, season, today).first))
                        val next = container.eventRepository.all().firstOrNull { it.date >= today }
                        setTextViewText(R.id.widget_event_text, next?.text ?: context.getString(R.string.widget_no_events))
                        setTextViewText(R.id.widget_event_date, next?.let { dayLabel(context, it.date, today) } ?: "")
                        setViewVisibility(R.id.widget_event_bar, if (next != null) View.VISIBLE else View.GONE)
                    }
                    Period.WEEK -> Unit
                }
                setOnClickPendingIntent(R.id.widget_root, open(context, add = false))
                setOnClickPendingIntent(R.id.widget_add, open(context, add = true))
            }
            manager.updateAppWidget(ids, views)
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
