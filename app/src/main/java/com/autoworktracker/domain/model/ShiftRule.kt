package com.autoworktracker.domain.model

import java.time.LocalTime

data class ShiftRule(
    val id: String,
    val name: String,
    val windowStart: LocalTime,
    val windowEnd: LocalTime,
    val hours: Double,
    val enabled: Boolean = true,
    val alarmBackupEnabled: Boolean = false,
) {
    fun contains(time: LocalTime): Boolean {
        return if (!windowStart.isAfter(windowEnd)) {
            !time.isBefore(windowStart) && !time.isAfter(windowEnd)
        } else {
            !time.isBefore(windowStart) || !time.isAfter(windowEnd)
        }
    }

    /**
     * Точка подстраховки внутри окна. Для окон по умолчанию это 14:15 и 18:15:
     * старт плюс 35 минут, но не позже чем за минуту до конца окна.
     */
    fun checkpoint(): LocalTime {
        val start = windowStart.toMinuteOfDay()
        val endRaw = windowEnd.toMinuteOfDay()
        val end = if (endRaw >= start) endRaw else endRaw + MINUTES_PER_DAY
        val latest = (end - 1).coerceAtLeast(start)
        val target = (start + CHECKPOINT_OFFSET_MINUTES).coerceIn(start, latest)
        return (target % MINUTES_PER_DAY).toLocalTime()
    }

    companion object {
        const val CHECKPOINT_OFFSET_MINUTES = 35
        private const val MINUTES_PER_DAY = 24 * 60

        fun defaults(): List<ShiftRule> = listOf(
            ShiftRule(
                id = "short",
                name = "Короткая смена",
                windowStart = LocalTime.of(13, 40),
                windowEnd = LocalTime.of(15, 30),
                hours = 7.5,
            ),
            ShiftRule(
                id = "long",
                name = "Длинная смена",
                windowStart = LocalTime.of(17, 40),
                windowEnd = LocalTime.of(19, 30),
                hours = 11.0,
            ),
        )
    }
}

fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

fun Int.toLocalTime(): LocalTime {
    val minute = Math.floorMod(this, 24 * 60)
    return LocalTime.of(minute / 60, minute % 60)
}
