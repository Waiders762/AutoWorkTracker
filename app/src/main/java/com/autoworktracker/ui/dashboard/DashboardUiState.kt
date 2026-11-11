package com.autoworktracker.ui.dashboard

import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.model.MonthSummary
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.location.GeofenceRegistration
import java.time.LocalDate
import java.time.YearMonth

data class EditorState(
    val originalDate: LocalDate?,
    val date: LocalDate,
    val hoursText: String,
)

sealed interface DistanceReading {
    data object Idle : DistanceReading
    data object Loading : DistanceReading
    data class Ready(val meters: Float, val inside: Boolean) : DistanceReading
    data class Approximate(val radiusMeters: Int) : DistanceReading
    data object Unavailable : DistanceReading
    data object NoPermission : DistanceReading
    data object NoWorkplace : DistanceReading
}

data class DashboardCore(
    val yearMonth: YearMonth,
    val shifts: List<Shift>,
    val settings: AppSettings,
    val geofence: GeofenceRegistration,
)

data class DashboardUiState(
    val loading: Boolean = true,
    val yearMonth: YearMonth = YearMonth.now(),
    val monthTitle: String = "",
    val shifts: List<Shift> = emptyList(),
    val summary: MonthSummary = MonthSummary(),
    val rules: List<ShiftRule> = ShiftRule.defaults(),
    val currency: String = "Kč",
    val language: AppLanguage = AppLanguage.RU,
    val statusTitle: String = "Вне работы — спящий режим геозоны",
    val statusDetail: String = "",
    val entryLabel: String? = null,
    val onSite: Boolean = false,
    val confirmed: Boolean = false,
    val distanceLabel: String = "Расстояние появится после разовой проверки",
    val workplaceConfigured: Boolean = false,
    val geofenceActive: Boolean = false,
    val geofenceMessage: String = "",
    val backupHealthy: Boolean = false,
    val backupError: String? = null,
    val noAfternoonBreak: Boolean = false,
    val lunchStart: String = "14:00",
    val lunchEnd: String = "14:30",
    val noBreakHours: Double = 11.5,
    val longShiftHours: Double = 11.0,
    val editor: EditorState? = null,
    val offerRestore: Boolean = false,
)
