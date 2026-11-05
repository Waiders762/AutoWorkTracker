package com.autoworktracker.domain.usecase

import com.autoworktracker.domain.model.ShiftRule
import java.time.LocalTime
import kotlin.math.abs

sealed interface ExitDecision {
    data class Apply(val rule: ShiftRule) : ExitDecision
    data class KeepExisting(val rule: ShiftRule) : ExitDecision
    data object OutsideWindow : ExitDecision
}

class EvaluateExitUseCase {
    fun decide(
        exitTime: LocalTime,
        rules: List<ShiftRule>,
        existingHours: Double?,
        overwriteEnabled: Boolean,
    ): ExitDecision {
        val matches = rules.filter { rule ->
            rule.enabled && rule.hours > 0.0 && rule.contains(exitTime)
        }
        if (matches.isEmpty()) return ExitDecision.OutsideWindow
        val best = matches.maxWith(compareBy<ShiftRule> { it.hours }.thenBy { it.windowStart })
        if (existingHours == null) return ExitDecision.Apply(best)
        val delta = best.hours - existingHours
        return when {
            delta > HOURS_EPSILON && overwriteEnabled -> ExitDecision.Apply(best)
            abs(delta) <= HOURS_EPSILON -> ExitDecision.Apply(best)
            else -> ExitDecision.KeepExisting(best)
        }
    }

    private companion object {
        const val HOURS_EPSILON = 0.001
    }
}
