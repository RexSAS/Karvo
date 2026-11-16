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
import com.karvo.domain.formatDuration
import com.karvo.domain.weekSoFar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** 2×1 home-screen widget: this week's hours and lessons (Monday to today), and a + that opens a new lesson. */
class WeekWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { render(context, manager, ids) } finally { pending.finish() }
        }
    }

    companion object {
        const val EXTRA_ADD_LESSON = "com.karvo.ADD_LESSON"

        /** Redraws every placed widget; called whenever the lessons change. */
        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeekWidget::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val container = (context.applicationContext as KarvoApp).container
            val zone = runCatching { ZoneId.of(container.settingsRepository.timeZoneId.first()) }.getOrDefault(ZoneId.systemDefault())
            val (minutes, count) = weekSoFar(container.lessonRepository.allLessons(), LocalDate.now(zone))
            val views = RemoteViews(context.packageName, R.layout.widget_week).apply {
                setTextViewText(R.id.widget_hours, hoursText(context, minutes))
                setTextViewText(R.id.widget_sub, context.resources.getQuantityString(R.plurals.lesson_count, count, count))
                setOnClickPendingIntent(R.id.widget_root, open(context, add = false))
                setOnClickPendingIntent(R.id.widget_add, open(context, add = true))
            }
            manager.updateAppWidget(ids, views)
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
