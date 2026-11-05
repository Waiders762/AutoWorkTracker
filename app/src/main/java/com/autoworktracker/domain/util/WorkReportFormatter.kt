package com.autoworktracker.domain.util

import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.Shift
import java.time.LocalDateTime
import java.time.YearMonth

object WorkReportFormatter {
    fun fullReport(
        shifts: List<Shift>,
        updatedAt: LocalDateTime,
        language: AppLanguage = AppLanguage.RU,
    ): String {
        val builder = StringBuilder()
        builder.appendLine("AutoWorkTracker")
        builder.appendLine(workedHoursTitle(language))
        builder.appendLine("${updatedLabel(language)}: ${RussianDateFormatter.formatUpdated(updatedAt, language)}")
        builder.appendLine()
        if (shifts.isEmpty()) {
            builder.appendLine(emptyYet(language))
            return builder.toString().trimEnd() + "\n"
        }
        val grouped = shifts.groupBy { YearMonth.from(it.date) }
        val months = grouped.keys.sortedDescending()
        var total = 0.0
        for (month in months) {
            val items = grouped.getValue(month).sortedBy { it.date }
            builder.appendLine(RussianDateFormatter.monthTitle(month, language))
            for (shift in items) {
                builder.appendLine(RussianDateFormatter.recordLine(shift.date, shift.hours, language))
            }
            val monthHours = items.sumOf { it.hours }
            total += monthHours
            builder.appendLine(totalLine(monthHours, items.size, language))
            builder.appendLine()
        }
        builder.appendLine("${grandTotalLabel(language)}: ${HoursFormatter.formatHours(total, language)}")
        return builder.toString().trimEnd() + "\n"
    }

    fun monthReport(
        shifts: List<Shift>,
        yearMonth: YearMonth,
        language: AppLanguage = AppLanguage.RU,
    ): String {
        val items = shifts
            .filter { YearMonth.from(it.date) == yearMonth }
            .sortedBy { it.date }
        val builder = StringBuilder()
        builder.appendLine(RussianDateFormatter.monthTitle(yearMonth, language))
        if (items.isEmpty()) {
            builder.appendLine(emptyMonth(language))
        } else {
            for (shift in items) {
                builder.appendLine(RussianDateFormatter.recordLine(shift.date, shift.hours, language))
            }
            builder.appendLine(totalLine(items.sumOf { it.hours }, items.size, language))
        }
        return builder.toString().trimEnd() + "\n"
    }

    private fun totalLine(hours: Double, count: Int, language: AppLanguage): String {
        val label = when (language) {
            AppLanguage.RU -> "Итого"
            AppLanguage.UK -> "Разом"
            AppLanguage.CS -> "Součet"
            AppLanguage.EN -> "Total"
        }
        return "$label: ${HoursFormatter.formatHours(hours, language)} ($count ${HoursFormatter.shiftWord(count, language)})"
    }

    private fun workedHoursTitle(language: AppLanguage): String = when (language) {
        AppLanguage.RU -> "Отработанные часы"
        AppLanguage.UK -> "Відпрацьовані години"
        AppLanguage.CS -> "Odpracované hodiny"
        AppLanguage.EN -> "Worked hours"
    }

    private fun updatedLabel(language: AppLanguage): String = when (language) {
        AppLanguage.RU -> "Обновлено"
        AppLanguage.UK -> "Оновлено"
        AppLanguage.CS -> "Aktualizováno"
        AppLanguage.EN -> "Updated"
    }

    private fun emptyYet(language: AppLanguage): String = when (language) {
        AppLanguage.RU -> "Записей пока нет."
        AppLanguage.UK -> "Записів поки немає."
        AppLanguage.CS -> "Zatím žádné záznamy."
        AppLanguage.EN -> "No records yet."
    }

    private fun emptyMonth(language: AppLanguage): String = when (language) {
        AppLanguage.RU -> "Записей нет."
        AppLanguage.UK -> "Записів немає."
        AppLanguage.CS -> "Žádné záznamy."
        AppLanguage.EN -> "No records."
    }

    private fun grandTotalLabel(language: AppLanguage): String = when (language) {
        AppLanguage.RU -> "Всего"
        AppLanguage.UK -> "Усього"
        AppLanguage.CS -> "Celkem"
        AppLanguage.EN -> "Overall"
    }
}
