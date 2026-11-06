package com.autoworktracker.ui.common

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.autoworktracker.domain.model.BackupLocations
import java.io.File

object ShareCenter {
    fun share(
        context: Context,
        message: String,
        attachment: String,
        chooserTitle: String,
        subject: String,
    ) {
        val directory = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(directory, BackupLocations.TEXT_FILE)
        file.writeText(attachment)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message.trim())
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(BackupLocations.TEXT_FILE, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
