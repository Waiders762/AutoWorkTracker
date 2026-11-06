package com.autoworktracker.domain.model

sealed interface GeofenceStatus {
    data object NotRegistered : GeofenceStatus
    data object WorkplaceNotSet : GeofenceStatus
    data object NeedBackgroundPermission : GeofenceStatus
    data object PlayServicesUnavailable : GeofenceStatus
    data class Active(val radiusMeters: Int) : GeofenceStatus
    data object Timeout : GeofenceStatus
    data class Error(val code: Int?) : GeofenceStatus
}
