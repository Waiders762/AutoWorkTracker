package com.autoworktracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoworktracker.AutoWorkTrackerApp
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.util.AppText
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autoworktracker.domain.model.Workplace
import com.autoworktracker.ui.dashboard.DashboardScreen
import com.autoworktracker.ui.settings.SettingsScreen
import com.autoworktracker.ui.settings.WorkplaceMapScreen

private data class Destination(
    val route: String,
    val label: (AppText) -> String,
    val icon: ImageVector,
)

private fun workplaceMapRoute(latitude: Double?, longitude: Double?, radius: Float): String {
    val lat = latitude?.toString().orEmpty()
    val lng = longitude?.toString().orEmpty()
    return "workplace-map?lat=$lat&lng=$lng&radius=$radius"
}

private val destinations = listOf(
    Destination("dashboard", { it.navShifts }, Icons.Filled.Home),
    Destination("settings", { it.navSettings }, Icons.Filled.Settings),
)

@Composable
fun AutoWorkNav() {
    val context = LocalContext.current
    val application = context.applicationContext as AutoWorkTrackerApp
    val settings by application.container.settingsRepository.observe()
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val text = AppText.of(settings.language)
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val onMap = current?.route?.startsWith("workplace-map") == true
    Scaffold(
        bottomBar = {
            if (!onMap) NavigationBar {
                destinations.forEach { destination ->
                    val selected = current?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label(text)) },
                        label = { Text(destination.label(text)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
        ) {
            composable("dashboard") { DashboardScreen() }
            composable("settings") {
                SettingsScreen(
                    onOpenMap = { latitude, longitude, radius ->
                        navController.navigate(workplaceMapRoute(latitude, longitude, radius))
                    },
                )
            }
            composable(
                route = "workplace-map?lat={lat}&lng={lng}&radius={radius}",
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType; defaultValue = "" },
                    navArgument("lng") { type = NavType.StringType; defaultValue = "" },
                    navArgument("radius") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val latitude = entry.arguments?.getString("lat")?.toDoubleOrNull()
                val longitude = entry.arguments?.getString("lng")?.toDoubleOrNull()
                val radius = entry.arguments?.getString("radius")?.toFloatOrNull()
                    ?: Workplace.DEFAULT_RADIUS_METERS
                WorkplaceMapScreen(
                    initialLatitude = latitude,
                    initialLongitude = longitude,
                    initialRadius = radius,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
