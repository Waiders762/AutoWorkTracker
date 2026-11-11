package com.autoworktracker.data.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.autoworktracker.data.local.ShiftDao
import com.autoworktracker.data.local.toDomain
import com.autoworktracker.data.preferences.SettingsDataStore
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.model.toMinuteOfDay
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.logging.AppLogger
import com.autoworktracker.domain.model.BackupLocations
import com.autoworktracker.domain.model.RestoredSettings
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.util.WorkReportFormatter
import com.autoworktracker.domain.util.WorkReportParser
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

data class ParsedBackup(
    val shifts: List<Shift>,
    val settings: RestoredSettings?,
)

class BackupManager(
    context: Context,
    private val shiftDao: ShiftDao,
    private val settingsDataStore: SettingsDataStore,
) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    suspend fun mirror() {
        mutex.withLock {
            val settings = settingsDataStore.current()
            val shifts = shiftDao.getAll().map { it.toDomain() }
            if (shifts.isEmpty() && !settings.initialized && readBestBackupLocked() != null) {
                Log.i(TAG, "Skip empty mirror so an existing Documents backup stays intact")
                return@withLock
            }
            writeSnapshot(settings, shifts)
        }
    }

    fun readBestBackup(): ByteArray? = readBestBackupLocked()

    fun parse(bytes: ByteArray, language: AppLanguage = AppLanguage.RU): ParsedBackup {
        val labels = AppText.of(language)
        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF").trim()
        if (text.startsWith("{")) {
            val payload = try {
                json.decodeFromString(BackupPayload.serializer(), text)
            } catch (error: Exception) {
                throw IllegalArgumentException(labels.jsonCorrupt(), error)
            }
            val shifts = payload.shifts.mapNotNull { it.toDomain() }
            return ParsedBackup(shifts = shifts, settings = payload.toRestoredSettings())
        }
        val shifts = WorkReportParser.parse(text)
        if (shifts.isEmpty()) {
            throw IllegalArgumentException(labels.noReportLines())
        }
        return ParsedBackup(shifts = shifts, settings = null)
    }

    private suspend fun writeSnapshot(settings: AppSettings, shifts: List<Shift>) {
        val now = LocalDateTime.now()
        val report = WorkReportFormatter.fullReport(shifts, now, settings.language)
        val payload = BackupPayload(
            version = 1,
            exportedAtEpochMillis = System.currentTimeMillis(),
            workplaceName = settings.workplace?.name,
            latitude = settings.workplace?.latitude,
            longitude = settings.workplace?.longitude,
            radiusMeters = settings.workplace?.radiusMeters,
            rules = settings.rules.map { it.toStored() },
            overwriteShortWithLong = settings.overwriteShortWithLong,
            hourlyRate = settings.hourlyRate,
            currency = settings.currency,
            language = settings.language.name,
            themeMode = settings.themeMode.name,
            dynamicColor = settings.dynamicColor,
            onSite = settings.presence.onSite,
            firstEntryEpochMillis = settings.presence.firstEntryEpochMillis,
            lastEntryEpochMillis = settings.presence.lastEntryEpochMillis,
            dwellConfirmed = settings.presence.dwellConfirmed,
            sessionConfirmed = settings.presence.sessionConfirmed,
            noAfternoonBreakToday = settings.presence.noAfternoonBreakToday,
            breakAutoDetected = settings.presence.breakAutoDetected,
            autoDetectLunchBreak = settings.autoDetectLunchBreak,
            lunchWindowStartMinutes = settings.lunchWindowStart.toMinuteOfDay(),
            lunchWindowEndMinutes = settings.lunchWindowEnd.toMinuteOfDay(),
            noBreakHours = settings.noBreakHours,
            lunchMaxUnlockedMinutes = settings.lunchMaxUnlockedMinutes,
            lunchMinActiveMinutes = settings.lunchMinActiveMinutes,
            shifts = shifts.map { it.toStored() },
        )
        val jsonText = json.encodeToString(BackupPayload.serializer(), payload)
        val textBytes = report.toByteArray(Charsets.UTF_8)
        val jsonBytes = jsonText.toByteArray(Charsets.UTF_8)
        val textOk = writeMediaStore(BackupLocations.TEXT_FILE, "text/plain", textBytes) ||
            writeDirect(BackupLocations.TEXT_FILE, textBytes)
        val jsonOk = writeMediaStore(BackupLocations.JSON_FILE, "application/json", jsonBytes) ||
            writeDirect(BackupLocations.JSON_FILE, jsonBytes)
        if (textOk && jsonOk) {
            settingsDataStore.setBackupStatus(
                healthy = true,
                epochMillis = System.currentTimeMillis(),
                error = null,
            )
            if (!settings.initialized) settingsDataStore.setInitialized(true)
        } else {
            val missing = buildList {
                if (!textOk) add(BackupLocations.TEXT_FILE)
                if (!jsonOk) add(BackupLocations.JSON_FILE)
            }.joinToString(", ")
            settingsDataStore.setBackupStatus(
                healthy = false,
                epochMillis = settings.lastBackupEpochMillis,
                error = AppText.of(settings.language).writeFailed(missing),
            )
        }
        AppLogger.backup("Mirror textOk=$textOk jsonOk=$jsonOk shifts=${shifts.size}")
    }

    private fun readBestBackupLocked(): ByteArray? {
        return readDirect(BackupLocations.JSON_FILE)
            ?: readMediaStore(BackupLocations.JSON_FILE)
            ?: readDirect(BackupLocations.TEXT_FILE)
            ?: readMediaStore(BackupLocations.TEXT_FILE)
    }

    private fun documentsDir(): File {
        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(documents, BackupLocations.DIRECTORY)
    }

    private fun writeDirect(displayName: String, bytes: ByteArray): Boolean {
        return try {
            val dir = documentsDir()
            if (!dir.exists() && !dir.mkdirs()) return false
            File(dir, displayName).writeBytes(bytes)
            true
        } catch (error: Exception) {
            Log.w(TAG, "Direct write $displayName failed: ${error.message}")
            false
        }
    }

    private fun readDirect(displayName: String): ByteArray? {
        return try {
            val file = File(documentsDir(), displayName)
            if (file.isFile && file.canRead() && file.length() > 0L) file.readBytes() else null
        } catch (error: Exception) {
            Log.w(TAG, "Direct read $displayName failed: ${error.message}")
            null
        }
    }

    private fun filesCollection() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    private fun writeMediaStore(displayName: String, mime: String, bytes: ByteArray): Boolean {
        return try {
            val resolver = appContext.contentResolver
            val collection = filesCollection()
            val existing = findExisting(displayName)
            val uri = existing ?: resolver.insert(collection, insertValues(displayName, mime)) ?: return false
            resolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                }
                resolver.update(uri, done, null, null)
            }
            true
        } catch (error: Exception) {
            Log.w(TAG, "MediaStore write $displayName failed: ${error.message}")
            false
        }
    }

    private fun readMediaStore(displayName: String): ByteArray? {
        return try {
            val uri = findExisting(displayName) ?: return null
            appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (error: Exception) {
            Log.w(TAG, "MediaStore read $displayName failed: ${error.message}")
            null
        }
    }

    private fun insertValues(displayName: String, mime: String): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Files.FileColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOCUMENTS}/${BackupLocations.DIRECTORY}/",
                )
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            } else {
                documentsDir().mkdirs()
                put(MediaStore.MediaColumns.DATA, File(documentsDir(), displayName).absolutePath)
            }
        }
    }

    private fun findExisting(displayName: String): android.net.Uri? {
        val resolver = appContext.contentResolver
        val collection = filesCollection()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )
        var bestId: Long? = null
        var bestModified = Long.MIN_VALUE
        resolver.query(
            collection,
            projection,
            "${MediaStore.Files.FileColumns.DISPLAY_NAME}=?",
            arrayOf(displayName),
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val pathIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
            val modifiedIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val path = if (pathIndex >= 0 && !cursor.isNull(pathIndex)) cursor.getString(pathIndex) else ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !path.contains(BackupLocations.DIRECTORY)
                ) {
                    continue
                }
                val modified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                    cursor.getLong(modifiedIndex)
                } else {
                    0L
                }
                if (modified >= bestModified) {
                    bestModified = modified
                    bestId = cursor.getLong(idIndex)
                }
            }
        }
        return bestId?.let { ContentUris.withAppendedId(collection, it) }
    }

    private companion object {
        const val TAG = "AutoWork"
    }
}
