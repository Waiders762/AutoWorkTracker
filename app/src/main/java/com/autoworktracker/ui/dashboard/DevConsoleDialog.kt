package com.autoworktracker.ui.dashboard

import android.app.KeyguardManager
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoworktracker.AutoWorkTrackerApp
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.logging.AppLogEntry
import com.autoworktracker.logging.AppLogger
import com.autoworktracker.logging.DevLogWriter
import com.autoworktracker.ui.settings.MotionProbe
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevConsoleDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoWorkTrackerApp
    val scope = rememberCoroutineScope()
    val settings by app.container.settingsRepository.observe().collectAsStateWithLifecycle(AppSettings())
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LogFilter.ALL) }
    var tab by remember { mutableIntStateOf(0) }
    var follow by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val allLogs by AppLogger.entriesState.collectAsStateWithLifecycle()
    val logs = remember(allLogs, query, filter) {
        allLogs.filter { entry ->
            filter.matches(entry) && (query.isBlank() || entry.line().contains(query, ignoreCase = true))
        }
    }
    LaunchedEffect(logs.size, follow) {
        if (follow && logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(AppLogger.exportText().toByteArray())
        }
        AppLogger.ui("Dev logs saved through the system picker")
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Консоль", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onDismiss) { Text("Закрыть") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Логи") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Живая телеметрия") })
                }
                if (tab == 0) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Поиск") },
                        singleLine = true,
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LogFilter.entries.forEach { item ->
                            FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item.label) })
                        }
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Автопрокрутка", modifier = Modifier.weight(1f))
                        Switch(checked = follow, onCheckedChange = { follow = it })
                    }
                    LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                        items(logs, key = { "${it.timestamp}-${it.category}-${it.message}" }) { entry ->
                            Text(entry.line(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = {
                        val path = DevLogWriter.writePublic(context)
                        AppLogger.ui("Dev logs public save path=${path ?: "failed"}")
                    }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить логи в файл (.txt)") }
                    OutlinedButton(onClick = { export.launch("autowork_dev_logs.txt") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Сохранить через систему")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("logs", AppLogger.exportText()))
                            AppLogger.ui("Dev logs copied")
                        }, modifier = Modifier.weight(1f)) { Text("Скопировать всё") }
                        OutlinedButton(onClick = {
                            AppLogger.clear()
                        }, modifier = Modifier.weight(1f)) { Text("Очистить логи") }
                    }
                } else {
                    TelemetryTab(settings = settings, onStartLunch = {
                        scope.launch { app.container.lunchDetector.startWindow() }
                    }, onEnter = {
                        scope.launch { app.container.shiftCoordinator.onEnter() }
                    }, onExit = {
                        scope.launch { app.container.shiftCoordinator.onExit() }
                    })
                }
            }
        }
    }
}

@Composable
private fun TelemetryTab(
    settings: AppSettings,
    onStartLunch: () -> Unit,
    onEnter: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val probe = remember { MotionProbe(context) }
    val x by probe.x.collectAsStateWithLifecycle()
    val y by probe.y.collectAsStateWithLifecycle()
    val z by probe.z.collectAsStateWithLifecycle()
    val delta by probe.delta.collectAsStateWithLifecycle()
    DisposableEffect(probe) {
        probe.start()
        onDispose { probe.stop() }
    }
    val power = context.getSystemService(PowerManager::class.java)
    val keyguard = context.getSystemService(KeyguardManager::class.java)
    val presence = settings.presence
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("X ${format(x)}   Y ${format(y)}   Z ${format(z)}")
        Text("Δg ${format(delta)}")
        Text("Экран: ${if (power.isInteractive && !keyguard.isDeviceLocked) "разблокирован" else "заблокирован"}")
        Text("На территории: ${presence.onSite}")
        Text("Без обеда: ${presence.noAfternoonBreakToday}  авто: ${presence.breakAutoDetected}")
        Button(onClick = onStartLunch, modifier = Modifier.fillMaxWidth()) { Text("Запустить тест обеда") }
        Button(onClick = onEnter, modifier = Modifier.fillMaxWidth()) { Text("Симулировать вход") }
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Симулировать выход") }
    }
}

private fun format(value: Float): String = String.format(Locale.US, "%.2f", value)

private enum class LogFilter(val label: String) {
    ALL("ВСЕ"),
    UI("UI"),
    GPS("GPS/GEO"),
    SENSORS("SENSORS"),
    ALARM("ALARM"),
    BACKUP("DB/BACKUP"),
    ;

    fun matches(entry: AppLogEntry): Boolean = when (this) {
        ALL -> true
        UI -> entry.category == AppLogger.NAV || entry.category == AppLogger.UI || entry.category == AppLogger.SETTINGS
        GPS -> entry.category == AppLogger.GPS || entry.category == AppLogger.GEOFENCE
        SENSORS -> entry.category == AppLogger.SENSORS
        ALARM -> entry.category == AppLogger.ALARM
        BACKUP -> entry.category == AppLogger.BACKUP
    }
}
