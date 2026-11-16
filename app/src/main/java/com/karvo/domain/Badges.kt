package com.karvo.domain

/** Season badges: the hours taught in one season unlock them in order. */
enum class Badge(val hours: Int) {
    FIRST_TRACKS(10), SNOWPLOUGH(50), PARALLEL(100), CARVER(200), MOGULS(300), POWDER(450), LEGEND(600);

    val minutes: Long get() = hours * 60L
}

/** [earned] is the best badge reached (null: none yet); [next] the one after it (null: all earned). */
data class BadgeProgress(val earned: Badge?, val next: Badge?, val minutes: Long) {
    /** 0..1 from [earned] to [next]; 1 once every badge is earned. */
    val fraction: Float get() {
        val to = next?.minutes ?: return 1f
        val from = earned?.minutes ?: 0L
        return ((minutes - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    }
    val minutesToNext: Long get() = next?.let { it.minutes - minutes } ?: 0L
}

fun badgeProgress(minutes: Long): BadgeProgress = BadgeProgress(
    earned = Badge.entries.lastOrNull { minutes >= it.minutes },
    next = Badge.entries.firstOrNull { minutes < it.minutes },
    minutes = minutes,
)
