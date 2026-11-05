package com.autoworktracker.domain.model

data class RuleCount(
    val rule: ShiftRule,
    val count: Int,
)

data class MonthSummary(
    val totalHours: Double = 0.0,
    val shiftCount: Int = 0,
    val byRule: List<RuleCount> = emptyList(),
    val otherCount: Int = 0,
    val salary: Double? = null,
)
