package com.autoworktracker.logging

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppLogEntry(
    val timestamp: String,
    val category: String,
    val message: String,
) {
    fun line(): String = "$timestamp [$category] $message"
}

object AppLogger {
    const val NAV = "NAV"
    const val UI = "UI_ACTION"
    const val SETTINGS = "SETTINGS"
    const val GPS = "GPS"
    const val GEOFENCE = "GEOFENCE"
    const val ALARM = "ALARM"
    const val SENSORS = "SENSORS"
    const val BACKUP = "DB_BACKUP"
    private const val MAX_ENTRIES = 2000

    private val lock = Any()
    private val entries = ArrayDeque<AppLogEntry>()
    private val entriesFlow = MutableStateFlow<List<AppLogEntry>>(emptyList())
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private var file: File? = null

    val entriesState: StateFlow<List<AppLogEntry>> = entriesFlow.asStateFlow()

    fun init(context: Context) {
        synchronized(lock) {
            if (file != null) return
            val target = File(context.applicationContext.filesDir, "app_internal.log")
            file = target
            if (target.exists()) {
                target.readLines().takeLast(MAX_ENTRIES).forEach { line ->
                    parse(line)?.let(entries::addLast)
                }
            }
            entriesFlow.value = entries.toList()
        }
    }

    fun log(category: String, message: String) {
        val entry = AppLogEntry(
            timestamp = LocalDateTime.now().format(stamp),
            category = category,
            message = message,
        )
        synchronized(lock) {
            entries.addLast(entry)
            while (entries.size > MAX_ENTRIES) entries.removeFirst()
            file?.let { target ->
                runCatching {
                    target.appendText(entry.line() + "\n")
                    if (target.length() > 1_500_000) {
                        val kept = entries.joinToString("\n") { it.line() } + "\n"
                        target.writeText(kept)
                    }
                }
            }
            entriesFlow.value = entries.toList()
        }
    }

    fun nav(screen: String) = log(NAV, "Open $screen")

    fun ui(message: String) = log(UI, message)

    fun settings(message: String) = log(SETTINGS, message)

    fun gps(message: String) = log(GPS, message)

    fun geofence(message: String) = log(GEOFENCE, message)

    fun alarm(message: String) = log(ALARM, message)

    fun sensors(message: String) = log(SENSORS, message)

    fun backup(message: String) = log(BACKUP, message)

    fun snapshot(): List<AppLogEntry> = synchronized(lock) { entries.toList() }

    fun exportText(): String = snapshot().joinToString("\n") { it.line() }

    fun clear() {
        synchronized(lock) {
            entries.clear()
            runCatching { file?.writeText("") }
            entriesFlow.value = emptyList()
        }
        log(UI, "Log buffer cleared")
    }

    private fun parse(line: String): AppLogEntry? {
        val timeEnd = line.indexOf(" [")
        val catEnd = line.indexOf("] ")
        if (timeEnd <= 0 || catEnd <= timeEnd) return null
        return AppLogEntry(
            timestamp = line.substring(0, timeEnd),
            category = line.substring(timeEnd + 2, catEnd),
            message = line.substring(catEnd + 2),
        )
    }
}
