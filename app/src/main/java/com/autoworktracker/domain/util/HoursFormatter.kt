package com.autoworktracker.domain.util

import com.autoworktracker.domain.model.AppLanguage
import java.math.BigDecimal
import java.math.RoundingMode

object HoursFormatter {
    fun formatNumber(hours: Double, language: AppLanguage = AppLanguage.RU): String {
        val rounded = BigDecimal.valueOf(hours)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        val plain = rounded.toPlainString()
        return if (language == AppLanguage.EN) plain else plain.replace('.', ',')
    }

    fun hourWord(hours: Double, language: AppLanguage = AppLanguage.RU): String {
        val rounded = BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP)
        val fractional = rounded.remainder(BigDecimal.ONE).abs().compareTo(BigDecimal.ZERO) != 0
        val value = rounded.abs().toLong()
        return when (language) {
            AppLanguage.RU -> if (fractional) "часов" else slavic(value, "час", "часа", "часов")
            AppLanguage.UK -> if (fractional) "годин" else slavic(value, "година", "години", "годин")
            AppLanguage.CS -> if (fractional) "hodin" else czech(value, "hodina", "hodiny", "hodin")
            AppLanguage.EN -> if (fractional || value != 1L) "hours" else "hour"
        }
    }

    fun formatHours(hours: Double, language: AppLanguage = AppLanguage.RU): String {
        return "${formatNumber(hours, language)} ${hourWord(hours, language)}"
    }

    fun formatCompact(hours: Double, language: AppLanguage = AppLanguage.RU): String {
        val suffix = when (language) {
            AppLanguage.RU -> "ч"
            AppLanguage.UK -> "год"
            AppLanguage.CS, AppLanguage.EN -> "h"
        }
        return "${formatNumber(hours, language)} $suffix"
    }

    fun shiftWord(count: Int, language: AppLanguage = AppLanguage.RU): String {
        val value = count.toLong()
        return when (language) {
            AppLanguage.RU -> slavic(value, "смена", "смены", "смен")
            AppLanguage.UK -> slavic(value, "зміна", "зміни", "змін")
            AppLanguage.CS -> czech(value, "směna", "směny", "směn")
            AppLanguage.EN -> if (count == 1) "shift" else "shifts"
        }
    }

    private fun slavic(value: Long, one: String, few: String, many: String): String {
        val n100 = (value % 100).toInt()
        val n10 = (value % 10).toInt()
        return when {
            n100 in 11..14 -> many
            n10 == 1 -> one
            n10 in 2..4 -> few
            else -> many
        }
    }

    private fun czech(value: Long, one: String, few: String, many: String): String {
        return when (value) {
            1L -> one
            2L, 3L, 4L -> few
            else -> many
        }
    }
}
