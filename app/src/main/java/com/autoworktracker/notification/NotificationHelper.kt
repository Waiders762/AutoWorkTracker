package com.autoworktracker.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.autoworktracker.MainActivity
import com.autoworktracker.R
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.util.RussianDateFormatter

class NotificationHelper(context: Context) {
    private val appContext = context.applicationContext

    fun notifyShiftRecorded(shift: Shift, language: AppLanguage) {
        if (!canNotify()) return
        val text = AppText.of(language)
        val line = RussianDateFormatter.recordLine(shift.date, shift.hours, language)
        val clock = listOfNotNull(
            shift.entryTime?.let { text.entryAt(RussianDateFormatter.formatTime(it)) },
            shift.exitTime?.let { text.exitAt(RussianDateFormatter.formatTime(it)) },
        ).joinToString(" · ")
        val body = buildString {
            append(line)
            append(" · ")
            append(shift.source.label(language))
            if (clock.isNotBlank()) {
                append('\n')
                append(clock)
            }
        }
        val notification = baseBuilder(CHANNEL_SHIFTS)
            .setContentTitle(text.shiftRecordedTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(appContext).notify(SHIFT_NOTIFICATION_ID, notification)
    }

    fun notifyPresence(title: String, text: String) {
        if (!canNotify()) return
        val notification = baseBuilder(CHANNEL_PRESENCE)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
        NotificationManagerCompat.from(appContext).notify(PRESENCE_NOTIFICATION_ID, notification)
    }

    private fun baseBuilder(channelId: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_SHIFTS = "shifts"
        const val CHANNEL_PRESENCE = "presence"
        private const val SHIFT_NOTIFICATION_ID = 42
        private const val PRESENCE_NOTIFICATION_ID = 43

        fun createChannels(context: Context, language: AppLanguage) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val text = AppText.of(language)
            val manager = context.getSystemService(NotificationManager::class.java)
            val shifts = NotificationChannel(
                CHANNEL_SHIFTS,
                text.channelShifts,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = text.channelShiftsDescription
            }
            val presence = NotificationChannel(
                CHANNEL_PRESENCE,
                text.channelPresence,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = text.channelPresenceDescription
                setShowBadge(false)
            }
            manager.createNotificationChannel(shifts)
            manager.createNotificationChannel(presence)
        }
    }
}
