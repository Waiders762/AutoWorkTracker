package com.autoworktracker.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.KeyguardManager
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.domain.model.ThemeMode
import com.autoworktracker.domain.model.Workplace
import com.autoworktracker.domain.model.availableCurrencies
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.util.HoursFormatter
import com.autoworktracker.domain.util.RussianDateFormatter
import com.autoworktracker.ui.common.CollectUserEvents
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onOpenMap: (latitude: Double?, longitude: Double?, radiusMeters: Float) -> Unit = { _, _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val text = AppText.of(state.settings.language)
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    CollectUserEvents(viewModel.events, snackbar, text = text)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.syncMonitoring()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restoreFromUri(uri)
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.syncMonitoring()
    }
    var showBackgroundHint by rememberSaveable { mutableStateOf(false) }
    var openMapAfterLocationPermission by rememberSaveable { mutableStateOf(false) }
    var centerMapAfterGrant by remember { mutableStateOf(false) }
    val backgroundPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.syncMonitoring()
    }
    val foregroundPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && openMapAfterLocationPermission) {
            openMapAfterLocationPermission = false
            centerMapAfterGrant = true
            viewModel.syncMonitoring()
        } else if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showBackgroundHint = true
        } else {
            viewModel.syncMonitoring()
        }
    }

    val workplace = state.settings.workplace
    val radius = workplace?.radiusMeters ?: Workplace.DEFAULT_RADIUS_METERS
    val workplaceName = text.workplaceFieldName(workplace?.name)
    var name by rememberSaveable(workplace?.name, text.defaultWorkplaceName) { mutableStateOf(workplaceName) }
    var latitude by rememberSaveable { mutableStateOf(workplace?.let { formatCoord(it.latitude) }.orEmpty()) }
    var longitude by rememberSaveable { mutableStateOf(workplace?.let { formatCoord(it.longitude) }.orEmpty()) }
    var rate by rememberSaveable {
        mutableStateOf(state.settings.hourlyRate?.let { HoursFormatter.formatNumber(it, state.settings.language) }.orEmpty())
    }
    androidx.compose.runtime.LaunchedEffect(workplace?.latitude, workplace?.longitude, workplace?.name, text.defaultWorkplaceName) {
        name = text.workplaceFieldName(workplace?.name)
        latitude = workplace?.let { formatCoord(it.latitude) }.orEmpty()
        longitude = workplace?.let { formatCoord(it.longitude) }.orEmpty()
    }
    androidx.compose.runtime.LaunchedEffect(state.settings.hourlyRate, state.settings.language) {
        rate = state.settings.hourlyRate?.let { HoursFormatter.formatNumber(it, state.settings.language) }.orEmpty()
    }
    androidx.compose.runtime.LaunchedEffect(centerMapAfterGrant) {
        if (!centerMapAfterGrant) return@LaunchedEffect
        centerMapAfterGrant = false
        viewModel.previewCurrentLocation { lat, lng -> onOpenMap(lat, lng, radius) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text.settingsTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LanguageMenu(
                    selected = state.settings.language,
                    onSelect = viewModel::setLanguage,
                )
            }
            Text(
                text.settingsSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SectionCard(title = text.workplaceSection) {
                if (state.locating) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Button(
                    onClick = {
                        onOpenMap(
                            workplace?.latitude,
                            workplace?.longitude,
                            radius,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text.openMap)
                }
                Text(
                    text.workplaceHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        if (state.permissions.foregroundLocation) {
                            viewModel.previewCurrentLocation { lat, lng -> onOpenMap(lat, lng, radius) }
                        } else {
                            openMapAfterLocationPermission = true
                            foregroundPermission.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.locating,
                ) {
                    Text(if (state.locating) text.locatingGps else text.suggestMyPlace)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.nameLabel) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.latitude) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.longitude) },
                    singleLine = true,
                )
                Button(
                    onClick = { viewModel.saveManualWorkplace(name, latitude, longitude, radius) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text.savePoint)
                }
            }
            SectionCard(title = text.rulesSection) {
                SwitchRow(
                    title = text.overwriteTitle,
                    checked = state.settings.overwriteShortWithLong,
                    onChecked = viewModel::setOverwrite,
                )
                Text(
                    text.overwriteHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.settings.rules.forEach { rule ->
                    RuleCard(
                        rule = rule,
                        language = state.settings.language,
                        text = text,
                        onEnabled = { viewModel.setRuleEnabled(rule.id, it) },
                        onAlarmBackup = { viewModel.setAlarmBackup(rule.id, it) },
                        onRename = { viewModel.setRuleName(rule.id, it) },
                        onHours = { viewModel.setRuleHours(rule.id, it) },
                        onStart = { viewModel.setRuleWindow(rule.id, start = it, end = null) },
                        onEnd = { viewModel.setRuleWindow(rule.id, start = null, end = it) },
                        onDelete = { viewModel.deleteRule(rule.id) },
                    )
                }
                OutlinedButton(onClick = viewModel::addRule, modifier = Modifier.fillMaxWidth()) {
                    Text(text.addRule)
                }
            }
            LunchControlCard(state = state, text = text, viewModel = viewModel)
            SectionCard(title = text.paySection) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableCurrencies.forEach { currency ->
                        FilterChip(
                            selected = state.settings.currency == currency,
                            onClick = { viewModel.setCurrency(currency) },
                            label = { Text(currency) },
                        )
                    }
                }
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.rateLabel(state.settings.currency)) },
                    singleLine = true,
                )
                Button(onClick = { viewModel.setHourlyRate(rate) }, modifier = Modifier.fillMaxWidth()) {
                    Text(text.saveRate)
                }
            }
            SectionCard(title = text.backupSection) {
                val backupStatus = when {
                    state.settings.backupHealthy -> text.backupOk
                    state.settings.lastBackupError != null -> state.settings.lastBackupError
                    else -> text.backupWaitingFile
                }
                Text(
                    backupStatus.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { openBackup.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text.restoreBackup)
                }
            }
            SectionCard(title = text.permissionsSection) {
                PermissionButton(
                    title = text.permissionBackground,
                    granted = state.permissions.backgroundLocation,
                    grantedLabel = text.granted,
                    neededLabel = text.needed,
                    action = text.allowAlways,
                    onClick = {
                        if (!state.permissions.foregroundLocation) {
                            foregroundPermission.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !state.permissions.backgroundLocation) {
                            showBackgroundHint = true
                        }
                    },
                )
                PermissionButton(
                    title = text.permissionNotifications,
                    granted = state.permissions.notifications,
                    grantedLabel = text.granted,
                    neededLabel = text.needed,
                    action = text.allowNotifications,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                PermissionButton(
                    title = text.permissionBattery,
                    granted = state.permissions.batteryUnrestricted,
                    grantedLabel = text.granted,
                    neededLabel = text.needed,
                    action = text.disableBattery,
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                )
                PermissionButton(
                    title = text.permissionAlarms,
                    granted = state.permissions.exactAlarm,
                    grantedLabel = text.granted,
                    neededLabel = text.needed,
                    action = text.allowAlarms,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                },
                            )
                        }
                    },
                )
                PermissionButton(
                    title = text.permissionDocuments,
                    granted = state.permissions.allFiles,
                    grantedLabel = text.granted,
                    neededLabel = text.needed,
                    action = text.allowAllFiles,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                                .onFailure {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                                }
                        }
                    },
                )
            }
            SectionCard(title = text.appearanceSection) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.DARK -> text.themeDark
                                        ThemeMode.LIGHT -> text.themeLight
                                        ThemeMode.SYSTEM -> text.themeSystem
                                    },
                                )
                            },
                        )
                    }
                }
                SwitchRow(
                    title = text.dynamicColor,
                    checked = state.settings.dynamicColor,
                    onChecked = viewModel::setDynamicColor,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showBackgroundHint) {
        AlertDialog(
            onDismissRequest = { showBackgroundHint = false },
            title = { Text(text.backgroundTitle) },
            text = { Text(text.backgroundBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundHint = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundPermission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    },
                ) { Text(text.continueAction) }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundHint = false }) { Text(text.notNow) }
            },
        )
    }
}

@Composable
private fun LanguageMenu(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        AppLanguage.RU to "Русский (RU)",
        AppLanguage.UK to "Українська (UA)",
        AppLanguage.CS to "Čeština (CZ)",
        AppLanguage.EN to "English (EN)",
    )
    Box {
        FilledTonalButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Translate, contentDescription = null)
            Text(selected.chip)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (language, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(language)
                        expanded = false
                    },
                    trailingIcon = if (language == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun PermissionButton(
    title: String,
    granted: Boolean,
    grantedLabel: String,
    neededLabel: String,
    action: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (granted) grantedLabel else neededLabel,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (!granted) {
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(action) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleCard(
    rule: ShiftRule,
    language: AppLanguage,
    text: AppText,
    onEnabled: (Boolean) -> Unit,
    onAlarmBackup: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onHours: (String) -> Unit,
    onStart: (LocalTime) -> Unit,
    onEnd: (LocalTime) -> Unit,
    onDelete: () -> Unit,
) {
    val localizedName = text.ruleBadge(rule)
    var name by rememberSaveable(rule.id, localizedName) { mutableStateOf(localizedName) }
    var hours by rememberSaveable(rule.id, rule.hours, language) {
        mutableStateOf(HoursFormatter.formatNumber(rule.hours, language))
    }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    val hourPresets = listOf(7.5, 11.0, 11.5)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SwitchRow(title = text.ruleEnabled, checked = rule.enabled, onChecked = onEnabled)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && name.isNotBlank()) onRename(name)
                    },
                label = { Text(text.nameLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onRename(name) }),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picking = "start" }, modifier = Modifier.weight(1f)) {
                    Text(text.fromTime(RussianDateFormatter.formatTime(rule.windowStart)))
                }
                OutlinedButton(onClick = { picking = "end" }, modifier = Modifier.weight(1f)) {
                    Text(text.untilTime(RussianDateFormatter.formatTime(rule.windowEnd)))
                }
            }
            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && hours.isNotBlank()) onHours(hours)
                    },
                label = { Text(text.hoursLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (hours.isNotBlank()) onHours(hours) }),
                supportingText = { Text(text.hoursFractionHint) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                hourPresets.forEach { preset ->
                    val label = HoursFormatter.formatCompact(preset, language)
                    val selected = hours.replace(',', '.').toDoubleOrNull()?.let { kotlin.math.abs(it - preset) < 0.001 } == true
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val formatted = HoursFormatter.formatNumber(preset, language)
                            hours = formatted
                            onHours(formatted)
                        },
                        label = { Text(label) },
                    )
                }
            }
            SwitchRow(
                title = text.alarmTitle(RussianDateFormatter.formatTime(rule.checkpoint())),
                checked = rule.alarmBackupEnabled,
                onChecked = onAlarmBackup,
            )
            Text(
                if (rule.alarmBackupEnabled) text.alarmOnHint else text.alarmOffHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onDelete) {
                Text(text.deleteRule, color = MaterialTheme.colorScheme.error)
            }
        }
    }
    val target = picking
    if (target != null) {
        val initial = if (target == "start") rule.windowStart else rule.windowEnd
        TimeDialog(
            text = text,
            initial = initial,
            onDismiss = { picking = null },
            onConfirm = { time ->
                picking = null
                if (target == "start") onStart(time) else onEnd(time)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    text: AppText,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val picker = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(picker.hour, picker.minute)) }) { Text(text.done) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text.cancel) } },
        title = { Text(text.timeWindow) },
        text = { TimePicker(state = picker) },
    )
}

private fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LunchControlCard(
    state: SettingsUiState,
    text: AppText,
    viewModel: SettingsViewModel,
) {
    val settings = state.settings
    var hours by rememberSaveable(settings.noBreakHours, settings.language) {
        mutableStateOf(HoursFormatter.formatNumber(settings.noBreakHours, settings.language))
    }
    var unlocked by remember(settings.lunchMaxUnlockedMinutes) {
        mutableFloatStateOf(settings.lunchMaxUnlockedMinutes.toFloat())
    }
    var motion by remember(settings.lunchMinActiveMinutes) {
        mutableFloatStateOf(settings.lunchMinActiveMinutes.toFloat())
    }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    SectionCard(title = text.lunchCardTitle()) {
        SwitchRow(
            title = text.lunchAutoSwitch(),
            checked = settings.autoDetectLunchBreak,
            onChecked = viewModel::setAutoDetectLunchBreak,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picking = "start" }, modifier = Modifier.weight(1f)) {
                Text(text.fromTime(RussianDateFormatter.formatTime(settings.lunchWindowStart)))
            }
            OutlinedButton(onClick = { picking = "end" }, modifier = Modifier.weight(1f)) {
                Text(text.untilTime(RussianDateFormatter.formatTime(settings.lunchWindowEnd)))
            }
        }
        Text(text.lunchHoursLabel(), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(11.5, 12.0).forEach { preset ->
                val label = HoursFormatter.formatCompact(preset, settings.language)
                val selected = kotlin.math.abs((hours.replace(',', '.').toDoubleOrNull() ?: 0.0) - preset) < 0.001
                FilterChip(
                    selected = selected,
                    onClick = {
                        hours = HoursFormatter.formatNumber(preset, settings.language)
                        viewModel.setNoBreakHours(preset)
                    },
                    label = { Text(label) },
                )
            }
        }
        OutlinedTextField(
            value = hours,
            onValueChange = { hours = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text.hoursLabel) },
            singleLine = true,
        )
        Button(
            onClick = {
                val value = hours.replace(',', '.').toDoubleOrNull()
                if (value != null) viewModel.setNoBreakHours(value)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(text.save) }
        Text(text.lunchUnlockedLabel(unlocked.toInt()), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = unlocked,
            onValueChange = { unlocked = it.toInt().coerceIn(1, 15).toFloat() },
            onValueChangeFinished = { viewModel.setLunchMaxUnlockedMinutes(unlocked.toInt()) },
            valueRange = 1f..15f,
            steps = 13,
        )
        Text(text.lunchUnlockedHint(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text.lunchMotionLabel(motion.toInt()), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = motion,
            onValueChange = { motion = it.toInt().coerceIn(5, 30).toFloat() },
            onValueChangeFinished = { viewModel.setLunchMinActiveMinutes(motion.toInt()) },
            valueRange = 5f..30f,
            steps = 24,
        )
        Text(text.lunchMotionHint(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        SensorCheck(text)
    }
    val target = picking
    if (target != null) {
        TimeDialog(
            text = text,
            initial = if (target == "start") settings.lunchWindowStart else settings.lunchWindowEnd,
            onDismiss = { picking = null },
            onConfirm = { time ->
                picking = null
                if (target == "start") viewModel.setLunchWindow(time, null) else viewModel.setLunchWindow(null, time)
            },
        )
    }
}

@Composable
private fun SensorCheck(text: AppText) {
    val context = LocalContext.current
    val probe = remember { MotionProbe(context) }
    var testing by remember { mutableStateOf(false) }
    val delta by probe.delta.collectAsStateWithLifecycle()
    val moving by probe.moving.collectAsStateWithLifecycle()
    DisposableEffect(testing) {
        if (testing) probe.start()
        onDispose { if (testing) probe.stop() }
    }
    Text(text.sensorCheckTitle(), style = MaterialTheme.typography.titleMedium)
    Text(text.sensorAccelerometer(probe.available))
    Text(text.sensorScreen(screenUnlocked(context)))
    Text(text.sensorAlarm(exactAlarmsAllowed(context)))
    Text(text.sensorGps(gpsReady(context)))
    OutlinedButton(onClick = { testing = !testing }, modifier = Modifier.fillMaxWidth()) {
        Text(text.sensorTestButton())
    }
    if (testing) {
        Text(text.sensorDelta(String.format(Locale.US, "%.2f", delta)))
        Text(
            if (moving) text.sensorMoving() else text.sensorStill(),
            color = if (moving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun screenUnlocked(context: android.content.Context): Boolean {
    val power = context.getSystemService(PowerManager::class.java)
    val keyguard = context.getSystemService(KeyguardManager::class.java)
    return power.isInteractive && !keyguard.isDeviceLocked
}

private fun exactAlarmsAllowed(context: android.content.Context): Boolean {
    val alarms = context.getSystemService(AlarmManager::class.java)
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
}

private fun gpsReady(context: android.content.Context): Boolean {
    val manager = context.getSystemService(LocationManager::class.java)
    val enabled = runCatching { manager.isLocationEnabled }.getOrDefault(false)
    val permission = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    return enabled && permission
}
