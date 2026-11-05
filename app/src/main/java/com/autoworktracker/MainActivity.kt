package com.autoworktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoworktracker.ui.navigation.AutoWorkNav
import com.autoworktracker.ui.theme.AutoWorkTheme
import com.autoworktracker.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by themeViewModel.dynamicColor.collectAsStateWithLifecycle()
            AutoWorkTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                AutoWorkNav()
            }
        }
    }
}
