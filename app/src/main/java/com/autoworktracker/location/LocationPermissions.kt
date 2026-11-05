package com.autoworktracker.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object LocationPermissions {
    fun hasForeground(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackground(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return hasForeground(context)
        val background = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        )
        return background == PackageManager.PERMISSION_GRANTED
    }

    fun canMonitor(context: Context): Boolean = hasForeground(context) && hasBackground(context)
}
