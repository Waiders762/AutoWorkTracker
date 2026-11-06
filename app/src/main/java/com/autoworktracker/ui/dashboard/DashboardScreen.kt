package com.autoworktracker.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.MonthSummary
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.util.HoursFormatter
import com.autoworktracker.domain.util.MoneyFormatter
import com.autoworktracker.domain.util.RussianDateFormatter
import com.autoworktracker.ui.common.CollectUserEvents
import com.autoworktracker.ui.components.GlowBackdrop
import com.autoworktracker.ui.components.PulsingDot
import com.autoworktracker.ui.theme.Blue
import com.autoworktracker.ui.theme.Danger
import com.autoworktracker.ui.theme.Emerald
import com.autoworktracker.ui.theme.Gold
import com.autoworktracker.ui.theme.GoldInk
import com.autoworktracker.ui.theme.Ink
import com.autoworktracker.ui.theme.Violet
import com.autoworktracker.ui.theme.VioletInk
import java.time.YearMonth
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val text = AppText.of(state.language)
    val snackbarHostState = remember { SnackbarHostState() }
    CollectUserEvents(viewModel.events, snackbarHostState, viewModel::undoDelete, text)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDistance()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restoreFromUri(uri)
    }
    Box(Modifier.fillMaxSize()) {
        GlowBackdrop(Modifier.fillMaxSize())
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = viewModel::openCreate,
                    containerColor = Emerald,
                    contentColor = Ink,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = text.addShift)
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Header(
                        text = text,
                        onRefresh = viewModel::refreshDistance,
                        onCopy = viewModel::copyMonth,
                        onShare = viewModel::shareReport,
                    )
                }
                item { StatusCard(state, text) }
                item {
                    MonthSwitcher(
                        title = state.monthTitle,
                        yearMonth = state.yearMonth,
                        text = text,
                        onPrevious = { viewModel.shiftMonth(-1) },
                        onNext = { viewModel.shiftMonth(1) },
                    )
                }
                item { SummaryCard(state.yearMonth, state.summary, state.currency, state.language) }
                item {
                    Text(
                        text = text.shiftHistory,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (!state.loading && state.shifts.isEmpty()) {
                    item { EmptyHistory(text) }
                }
                items(state.shifts, key = { it.date.toEpochDay() }) { shift ->
                    SwipeableShift(
                        shift = shift,
                        language = state.language,
                        text = text,
                        onOpen = { viewModel.openEdit(shift) },
                        onDelete = { viewModel.deleteShift(shift) },
                    )
                }
            }
        }
    }
    val editor = state.editor
    if (editor != null) {
        val editing = state.shifts.firstOrNull { it.date == editor.originalDate || it.date == editor.date }
        ShiftEditorDialog(
            state = editor,
            rules = state.rules,
            language = state.language,
            onDateChange = viewModel::updateEditorDate,
            onHoursChange = viewModel::updateEditorHours,
            onSave = viewModel::saveEditor,
            onDelete = editing?.let { shift -> { viewModel.deleteShift(shift) } },
            onDismiss = viewModel::dismissEditor,
        )
    }
    if (state.offerRestore) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreOffer,
            title = { Text(text.restoreTitle) },
            text = { Text(text.restoreBody) },
            confirmButton = {
                Button(onClick = { openBackup.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                    Text(text.chooseFile)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRestoreOffer) { Text(text.later) }
            },
        )
    }
}

@Composable
private fun Header(
    text: AppText,
    onRefresh: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AutoWorkTracker",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            ),
            maxLines = 1,
            softWrap = false,
        )
        IconButton(onClick = onCopy) {
            Icon(Icons.Filled.ContentCopy, contentDescription = text.copyReport)
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = text.shareFile)
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = text.refreshDistance)
        }
    }
}

@Composable
private fun StatusCard(state: DashboardUiState, text: AppText) {
    val accent = if (state.onSite) Emerald else Blue
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PulsingDot(color = accent, pulsing = state.onSite)
                Text(state.statusTitle, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                state.statusDetail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.entryLabel?.let { label ->
                Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaPill(
                    text = if (state.backupHealthy) text.backupActive else text.backupWaiting,
                    container = if (state.backupHealthy) Emerald.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                    content = if (state.backupHealthy) Emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MetaPill(
                    text = if (state.geofenceActive) text.ecoMode else text.geofenceOff,
                    container = if (state.geofenceActive) Blue.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                    content = if (state.geofenceActive) Blue else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.backupError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.geofenceMessage.isNotBlank() && state.geofenceMessage != state.statusDetail) {
                Text(
                    state.geofenceMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetaPill(text: String, container: Color, content: Color) {
    Text(
        text = text,
        color = content,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun MonthSwitcher(
    title: String,
    yearMonth: YearMonth,
    text: AppText,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = text.previousMonth)
        }
        AnimatedContent(
            targetState = yearMonth to title,
            transitionSpec = {
                val forward = targetState.first > initialState.first
                val direction = if (forward) 1 else -1
                (slideInHorizontally { it / 3 * direction } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 * direction } + fadeOut())
            },
            label = "month-title",
        ) { value ->
            Text(value.second, style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = text.nextMonth)
        }
    }
}

@Composable
private fun SummaryCard(
    yearMonth: YearMonth,
    summary: MonthSummary,
    currency: String,
    language: AppLanguage,
) {
    val text = AppText.of(language)
    val animated by animateFloatAsState(
        targetValue = summary.totalHours.toFloat(),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "total-hours",
    )
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0E8F68), Color(0xFF2457D6))))
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    RussianDateFormatter.monthTitle(yearMonth, language).uppercase(),
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    HoursFormatter.formatNumber(animated.toDouble(), language),
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    RussianDateFormatter.monthHoursLine(summary.totalHours, yearMonth.monthValue, language),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.byRule.filter { it.count > 0 }.forEach { item ->
                        MetaPill(
                            text = "${text.ruleBadge(item.rule)}: ${item.count}",
                            container = Color.White.copy(alpha = 0.16f),
                            content = Color.White,
                        )
                    }
                    if (summary.otherCount > 0) {
                        MetaPill(
                            text = text.others(summary.otherCount),
                            container = Color.White.copy(alpha = 0.16f),
                            content = Color.White,
                        )
                    }
                    if (summary.shiftCount == 0) {
                        MetaPill(
                            text = text.noShiftsYet,
                            container = Color.White.copy(alpha = 0.16f),
                            content = Color.White,
                        )
                    }
                }
                summary.salary?.let { salary ->
                    Text(
                        text.toPay(MoneyFormatter.format(salary, currency)),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory(text: AppText) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text.emptyMonthTitle, style = MaterialTheme.typography.titleMedium)
        Text(
            text.emptyMonthBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableShift(
    shift: Shift,
    language: AppLanguage,
    text: AppText,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Danger)
                    .padding(end = 22.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = text.delete, tint = Color.White)
            }
        },
    ) {
        ShiftCard(shift = shift, language = language, onOpen = onOpen, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ShiftCard(
    shift: Shift,
    language: AppLanguage,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (badge, badgeInk) = badgeColors(shift.hours)
    Card(
        onClick = onOpen,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(6.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(badge),
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    RussianDateFormatter.recordLine(shift.date, shift.hours, language),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    shiftCaption(shift, language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                HoursFormatter.formatCompact(shift.hours, language),
                color = badgeInk,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badge)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private fun shiftCaption(shift: Shift, language: AppLanguage): String {
    val text = AppText.of(language)
    val entry = shift.entryTime?.let { text.entryAt(RussianDateFormatter.formatTime(it)) }
    val exit = shift.exitTime?.let { text.exitAt(RussianDateFormatter.formatTime(it)) }
    val times = listOfNotNull(entry, exit).joinToString(" · ")
    val source = shift.source.label(language)
    return if (times.isBlank()) source else "$times · $source"
}

private fun badgeColors(hours: Double): Pair<Color, Color> {
    return when {
        hours >= 10.0 -> Gold to GoldInk
        abs(hours - 7.5) < 0.051 || hours in 6.0..8.5 -> Emerald to Ink
        else -> Violet to VioletInk
    }
}
