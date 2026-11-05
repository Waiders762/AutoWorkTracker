package com.autoworktracker.domain.usecase

import com.autoworktracker.domain.model.MonthSummary
import com.autoworktracker.domain.model.RuleCount
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.model.ShiftRule
import kotlin.math.abs

class BuildMonthSummaryUseCase {
    fun build(
        shifts: List<Shift>,
        rules: List<ShiftRule>,
        hourlyRate: Double?,
    ): MonthSummary {
        val counts = rules.map { rule ->
            RuleCount(
                rule = rule,
                count = shifts.count { shift -> matchesRule(shift.hours, rule, rules) },
            )
        }
        val matched = shifts.count { shift ->
            rules.any { rule -> matchesRule(shift.hours, rule, rules) }
        }
        val total = shifts.sumOf { it.hours }
        val salary = hourlyRate?.takeIf { it > 0.0 }?.times(total)
        return MonthSummary(
            totalHours = total,
            shiftCount = shifts.size,
            byRule = counts,
            otherCount = (shifts.size - matched).coerceAtLeast(0),
            salary = salary,
        )
    }

    /**
     * Точное совпадение часов остаётся главным.
     * Смена 11, 11,5 или 12 часов, которая не попала точно в другое правило,
     * относится к одному правилу длинной смены: к правилу с id "long",
     * а если такого id нет — к правилу с наибольшим числом часов не меньше 10.
     * Так 11 и 11,5 не уходят в «Другие», даже когда в правиле записано другое из этих двух значений.
     */
    private fun matchesRule(shiftHours: Double, rule: ShiftRule, rules: List<ShiftRule>): Boolean {
        if (abs(shiftHours - rule.hours) < MATCH_EPSILON) return true
        val longRule = longShiftRule(rules) ?: return false
        if (rule.id != longRule.id) return false
        if (shiftHours < LONG_SHIFT_FLOOR) return false
        val exactOther = rules.any { other ->
            other.id != rule.id && abs(shiftHours - other.hours) < MATCH_EPSILON
        }
        return !exactOther
    }

    private fun longShiftRule(rules: List<ShiftRule>): ShiftRule? {
        rules.firstOrNull { it.id == "long" }?.let { return it }
        val maxHours = rules.maxOfOrNull { it.hours } ?: return null
        if (maxHours < LONG_RULE_MIN_HOURS) return null
        return rules.firstOrNull { abs(it.hours - maxHours) < MATCH_EPSILON }
    }

    private companion object {
        const val MATCH_EPSILON = 0.051
        const val LONG_SHIFT_FLOOR = 11.0
        const val LONG_RULE_MIN_HOURS = 10.0
    }
}
