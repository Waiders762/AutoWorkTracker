package com.autoworktracker.domain.usecase

import com.autoworktracker.domain.model.ShiftRule

object LunchVerdict {
    const val MOVEMENT_DELTA_G = 0.65f

    fun minuteWasActive(averageDelta: Float, peakDelta: Float): Boolean {
        return averageDelta > MOVEMENT_DELTA_G || peakDelta > MOVEMENT_DELTA_G
    }

    fun workedWithoutBreak(
        unlockedSeconds: Int,
        activeMinutes: Int,
        maxUnlockedMinutes: Int,
        minActiveMinutes: Int,
    ): Boolean {
        return unlockedSeconds < maxUnlockedMinutes * 60 && activeMinutes >= minActiveMinutes
    }
}

object EveningShiftHours {
    fun resolve(rule: ShiftRule, noAfternoonBreak: Boolean, noBreakHours: Double): Double {
        val longShift = rule.id == "long" || rule.hours >= 10.0
        return if (longShift && noAfternoonBreak) noBreakHours else rule.hours
    }
}
