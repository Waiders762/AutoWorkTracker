package com.autoworktracker.domain

import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.domain.model.ShiftSource
import com.autoworktracker.domain.usecase.BuildMonthSummaryUseCase
import com.autoworktracker.domain.usecase.EvaluateExitUseCase
import com.autoworktracker.domain.usecase.ExitDecision
import com.autoworktracker.domain.util.HoursFormatter
import com.autoworktracker.domain.util.RussianDateFormatter
import com.autoworktracker.domain.util.WorkReportFormatter
import com.autoworktracker.domain.util.WorkReportParser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoursAndRulesTest {
    private val evaluate = EvaluateExitUseCase()
    private val rules = ShiftRule.defaults()

    @Test
    fun formatsWholeAndFractionalHours() {
        assertEquals("11", HoursFormatter.formatNumber(11.0))
        assertEquals("7,5", HoursFormatter.formatNumber(7.5))
        assertEquals("11 часов", HoursFormatter.formatHours(11.0))
        assertEquals("7,5 часов", HoursFormatter.formatHours(7.5))
        assertEquals("1 час", HoursFormatter.formatHours(1.0))
        assertEquals("2 часа", HoursFormatter.formatHours(2.0))
        assertEquals("21 час", HoursFormatter.formatHours(21.0))
        assertEquals("22 часа", HoursFormatter.formatHours(22.0))
        assertEquals("1,5 часов", HoursFormatter.formatHours(1.5))
        assertEquals("5 часов", HoursFormatter.formatHours(5.0))
    }

    @Test
    fun formatsRussianDateLine() {
        val date = LocalDate.of(2026, 11, 2)
        assertEquals("2 ноября", RussianDateFormatter.dayMonth(date))
        assertEquals("2 ноября: 11 часов", RussianDateFormatter.recordLine(date, 11.0))
        assertEquals("3 ноября: 7,5 часов", RussianDateFormatter.recordLine(date.plusDays(1), 7.5))
        assertEquals("Ноябрь 2026", RussianDateFormatter.monthTitle(java.time.YearMonth.of(2026, 11)))
        assertEquals("ноябрь", RussianDateFormatter.monthAccusative(11))
        assertEquals("январь", RussianDateFormatter.monthAccusative(1))
        assertEquals("11,5", HoursFormatter.formatNumber(11.5))
        assertEquals("11,5 часов", HoursFormatter.formatHours(11.5))
    }

    @Test
    fun defaultCheckpointsSitInsideShiftWindows() {
        val short = rules.first { it.id == "short" }
        val long = rules.first { it.id == "long" }
        assertEquals(LocalTime.of(14, 15), short.checkpoint())
        assertEquals(LocalTime.of(18, 15), long.checkpoint())
        assertTrue(short.contains(LocalTime.of(13, 40)))
        assertTrue(short.contains(LocalTime.of(15, 30)))
        assertFalse(short.contains(LocalTime.of(15, 31)))
        assertTrue(long.contains(LocalTime.of(17, 40)))
        assertTrue(long.contains(LocalTime.of(19, 30)))
    }

    @Test
    fun wrappedWindowIncludesNightAndExcludesNoon() {
        val night = ShiftRule(
            id = "night",
            name = "Ночь",
            windowStart = LocalTime.of(22, 0),
            windowEnd = LocalTime.of(2, 0),
            hours = 8.0,
        )
        assertTrue(night.contains(LocalTime.of(23, 0)))
        assertTrue(night.contains(LocalTime.of(1, 0)))
        assertFalse(night.contains(LocalTime.of(12, 0)))
    }

    @Test
    fun eveningExitUpgradesShortShiftOnlyWhenAllowed() {
        val fourteen = evaluate.decide(LocalTime.of(14, 5), rules, null, true)
        assertTrue(fourteen is ExitDecision.Apply)
        assertEquals(7.5, (fourteen as ExitDecision.Apply).rule.hours, 0.001)

        val upgrade = evaluate.decide(LocalTime.of(18, 2), rules, 7.5, true)
        assertTrue(upgrade is ExitDecision.Apply)
        assertEquals(11.0, (upgrade as ExitDecision.Apply).rule.hours, 0.001)

        val blocked = evaluate.decide(LocalTime.of(18, 2), rules, 7.5, false)
        assertTrue(blocked is ExitDecision.KeepExisting)

        val noDowngrade = evaluate.decide(LocalTime.of(14, 10), rules, 11.0, true)
        assertTrue(noDowngrade is ExitDecision.KeepExisting)

        val outside = evaluate.decide(LocalTime.of(12, 0), rules, null, true)
        assertTrue(outside is ExitDecision.OutsideWindow)

        val half = rules.map { rule -> if (rule.id == "long") rule.copy(hours = 11.5) else rule }
        val evening = evaluate.decide(LocalTime.of(18, 2), half, 7.5, true)
        assertTrue(evening is ExitDecision.Apply)
        assertEquals(11.5, (evening as ExitDecision.Apply).rule.hours, 0.001)
    }

    @Test
    fun elevenPointFiveCountsAsLongShiftWithNoOthers() {
        val useCase = BuildMonthSummaryUseCase()
        val summary = useCase.build(
            shifts = listOf(
                sample(LocalDate.of(2026, 11, 2), 11.0),
                sample(LocalDate.of(2026, 11, 3), 11.5),
                sample(LocalDate.of(2026, 11, 4), 7.5),
            ),
            rules = rules,
            hourlyRate = null,
        )
        assertEquals(2, summary.byRule.first { it.rule.id == "long" }.count)
        assertEquals(1, summary.byRule.first { it.rule.id == "short" }.count)
        assertEquals(0, summary.otherCount)

        val longHalf = rules.map { rule -> if (rule.id == "long") rule.copy(hours = 11.5) else rule }
        val swapped = useCase.build(
            shifts = listOf(
                sample(LocalDate.of(2026, 11, 2), 11.0),
                sample(LocalDate.of(2026, 11, 3), 11.5),
            ),
            rules = longHalf,
            hourlyRate = null,
        )
        assertEquals(2, swapped.byRule.first { it.rule.id == "long" }.count)
        assertEquals(0, swapped.otherCount)
    }

    @Test
    fun reportRoundTripKeepsDaysAndHours() {
        val shifts = listOf(
            sample(LocalDate.of(2026, 11, 2), 11.0),
            sample(LocalDate.of(2026, 11, 3), 7.5),
            sample(LocalDate.of(2026, 10, 30), 11.0),
        )
        val report = WorkReportFormatter.fullReport(shifts, LocalDateTime.of(2026, 11, 3, 18, 4))
        assertTrue(report.contains("2 ноября: 11 часов"))
        assertTrue(report.contains("3 ноября: 7,5 часов"))
        assertTrue(report.contains("Итого: 18,5 часов"))
        val parsed = WorkReportParser.parse(report, restoredAtEpochMillis = 10L)
        assertEquals(3, parsed.size)
        assertEquals(11.0, parsed.first { it.date.dayOfMonth == 2 && it.date.monthValue == 11 }.hours, 0.001)
        assertEquals(7.5, parsed.first { it.date.dayOfMonth == 3 }.hours, 0.001)
        assertEquals(ShiftSource.RESTORED, parsed.first().source)
    }

    private fun sample(date: LocalDate, hours: Double) = Shift(
        date = date,
        hours = hours,
        entryTime = LocalTime.of(8, 12),
        exitTime = LocalTime.of(18, 4),
        source = ShiftSource.AUTO_GEOFENCE,
        updatedAtEpochMillis = 1L,
    )
}
