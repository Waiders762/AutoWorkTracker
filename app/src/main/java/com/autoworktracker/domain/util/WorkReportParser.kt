package com.autoworktracker.domain.util

import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.model.ShiftSource
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Year

object WorkReportParser {
    private val header = Regex("""^(\p{L}+)\s+(\d{4})$""")
    private val dayDotRecord = Regex("""^(\d{1,2})\.\s*(\p{L}+):\s+(\d+(?:[.,]\d+)?)""")
    private val dayFirstRecord = Regex("""^(\d{1,2})\s+(\p{L}+):\s+(\d+(?:[.,]\d+)?)""")
    private val monthFirstRecord = Regex("""^(\p{L}+)\s+(\d{1,2}):\s+(\d+(?:[.,]\d+)?)""")

    fun parse(text: String, restoredAtEpochMillis: Long = System.currentTimeMillis()): List<Shift> {
        var year = Year.now().value
        val byDate = linkedMapOf<LocalDate, Shift>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val headerMatch = header.matchEntire(line)
            if (headerMatch != null && RussianDateFormatter.monthIndexFromNominative(headerMatch.groupValues[1]) != null) {
                year = headerMatch.groupValues[2].toInt()
                return@forEach
            }
            val parsed = parseRecord(line) ?: return@forEach
            if (parsed.hours <= 0.0 || parsed.hours > 24.0) return@forEach
            val date = try {
                LocalDate.of(year, parsed.month, parsed.day)
            } catch (_: DateTimeException) {
                return@forEach
            }
            byDate[date] = Shift(
                date = date,
                hours = parsed.hours,
                entryTime = null,
                exitTime = null,
                source = ShiftSource.RESTORED,
                updatedAtEpochMillis = restoredAtEpochMillis,
            )
        }
        return byDate.values.sortedBy { it.date }
    }

    private fun parseRecord(line: String): ParsedRecord? {
        dayDotRecord.find(line)?.let { match ->
            val month = RussianDateFormatter.monthIndexFromGenitive(match.groupValues[2]) ?: return@let
            val hours = match.groupValues[3].toHours() ?: return@let
            return ParsedRecord(day = match.groupValues[1].toInt(), month = month, hours = hours)
        }
        dayFirstRecord.find(line)?.let { match ->
            val month = RussianDateFormatter.monthIndexFromGenitive(match.groupValues[2]) ?: return@let
            val hours = match.groupValues[3].toHours() ?: return@let
            return ParsedRecord(day = match.groupValues[1].toInt(), month = month, hours = hours)
        }
        monthFirstRecord.find(line)?.let { match ->
            val month = RussianDateFormatter.monthIndexFromNominative(match.groupValues[1]) ?: return@let
            val hours = match.groupValues[3].toHours() ?: return@let
            return ParsedRecord(day = match.groupValues[2].toInt(), month = month, hours = hours)
        }
        return null
    }

    private fun String.toHours(): Double? = replace(',', '.').toDoubleOrNull()

    private data class ParsedRecord(val day: Int, val month: Int, val hours: Double)
}
