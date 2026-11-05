package com.autoworktracker.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class ShiftSource {
    AUTO_GEOFENCE,
    AUTO_CHECK,
    MANUAL,
    RESTORED,
    ;

    fun label(language: AppLanguage = AppLanguage.RU): String = when (language) {
        AppLanguage.RU -> when (this) {
            AUTO_GEOFENCE -> "Авто (Геозона)"
            AUTO_CHECK -> "Авто (проверка смены)"
            MANUAL -> "Вручную"
            RESTORED -> "Восстановлено"
        }
        AppLanguage.UK -> when (this) {
            AUTO_GEOFENCE -> "Авто (Геозона)"
            AUTO_CHECK -> "Авто (перевірка зміни)"
            MANUAL -> "Вручну"
            RESTORED -> "Відновлено"
        }
        AppLanguage.CS -> when (this) {
            AUTO_GEOFENCE -> "Auto (geozóna)"
            AUTO_CHECK -> "Auto (kontrola směny)"
            MANUAL -> "Ručně"
            RESTORED -> "Obnoveno"
        }
        AppLanguage.EN -> when (this) {
            AUTO_GEOFENCE -> "Auto (geofence)"
            AUTO_CHECK -> "Auto (shift check)"
            MANUAL -> "Manual"
            RESTORED -> "Restored"
        }
    }
}

data class Shift(
    val id: Long = 0,
    val date: LocalDate,
    val hours: Double,
    val entryTime: LocalTime?,
    val exitTime: LocalTime?,
    val source: ShiftSource,
    val note: String = "",
    val updatedAtEpochMillis: Long,
)
