package com.autoworktracker.logging

import android.content.Context
import android.os.Environment
import com.autoworktracker.domain.model.BackupLocations
import java.io.File

object DevLogWriter {
    fun writePublic(context: Context): String? {
        return try {
            val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(documents, BackupLocations.DIRECTORY)
            if (!dir.exists() && !dir.mkdirs()) return null
            val file = File(dir, "autowork_dev_logs.txt")
            file.writeText(AppLogger.exportText())
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
