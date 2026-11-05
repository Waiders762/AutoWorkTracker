package com.autoworktracker.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoworktracker.AutoWorkTrackerApp
import com.autoworktracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = (application as AutoWorkTrackerApp).container.settingsRepository

    val themeMode: StateFlow<ThemeMode> = settings.observe()
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.DARK)

    val dynamicColor: StateFlow<Boolean> = settings.observe()
        .map { it.dynamicColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
