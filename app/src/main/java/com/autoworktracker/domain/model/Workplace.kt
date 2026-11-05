package com.autoworktracker.domain.model

data class Workplace(
    val name: String = "Работа",
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = DEFAULT_RADIUS_METERS,
) {
    fun sanitized(): Workplace {
        val safeName = name.trim().ifBlank { "Работа" }.take(40)
        val safeRadius = radiusMeters.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
        return copy(name = safeName, radiusMeters = safeRadius)
    }

    companion object {
        const val DEFAULT_RADIUS_METERS = 200f
        const val MIN_RADIUS_METERS = 50f
        const val MAX_RADIUS_METERS = 1000f
    }
}
