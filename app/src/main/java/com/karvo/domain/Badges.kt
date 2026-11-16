package com.karvo.domain

/** Season badges: the hours taught in one season unlock them in order. */
enum class Badge(val hours: Int) {
    KARVER(1), FIRST_TRACKS(5), BUNNY_HILL(10),
    SNOWPLOUGH(25), PARALLEL(50), RED_RUN(75),
    EDGE_MASTER(100), BLACK_RUN(150), MOGULS(200),
    OFF_PISTE(300), POWDER(400), AVALANCHE(500),
    SUMMIT(600), LEGEND(750);

    val minutes: Long get() = hours * 60L
    /** Bronze, silver, gold, amethyst: three badges each; the last two are the ice legends (4). */
    val tier: Int get() = minOf(ordinal / 3, 4)
    /** Where it sits in its tier: 0, 1 or 2. */
    val step: Int get() = ordinal - tier * 3
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
