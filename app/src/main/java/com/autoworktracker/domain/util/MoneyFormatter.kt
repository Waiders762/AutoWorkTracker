package com.autoworktracker.domain.util

import com.autoworktracker.domain.model.AppLanguage
import java.math.BigDecimal
import java.math.RoundingMode

object MoneyFormatter {
    fun format(value: Double, currency: String): String {
        val symbol = currency.ifBlank { "Kč" }
        val rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
        val negative = rounded.signum() < 0
        val absolute = rounded.abs().toPlainString()
        val parts = absolute.split('.')
        val grouped = parts[0].reversed().chunked(3).joinToString("\u00A0").reversed()
        val body = if (parts.getOrNull(1) == "00") grouped else "$grouped,${parts[1]}"
        return (if (negative) "−" else "") + "$body $symbol"
    }

    fun formatKc(value: Double): String = format(value, "Kč")
}

object DistanceFormatter {
    fun formatMeters(meters: Float, language: AppLanguage = AppLanguage.RU): String {
        val meter = when (language) {
            AppLanguage.RU, AppLanguage.UK -> "м"
            AppLanguage.CS, AppLanguage.EN -> "m"
        }
        val kilometer = when (language) {
            AppLanguage.RU, AppLanguage.UK -> "км"
            AppLanguage.CS, AppLanguage.EN -> "km"
        }
        if (meters < 1000f) return "${meters.toInt()} $meter"
        val kilometers = meters / 1000f
        val tenths = (kilometers * 10f).toInt() / 10f
        val text = if (language == AppLanguage.EN) {
            tenths.toString()
        } else {
            tenths.toString().replace('.', ',')
        }
        return "$text $kilometer"
    }
}
