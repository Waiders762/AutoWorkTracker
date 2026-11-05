package com.autoworktracker.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.util.HoursFormatter
import com.autoworktracker.domain.util.RussianDateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftEditorDialog(
    state: EditorState,
    rules: List<ShiftRule>,
    language: AppLanguage = AppLanguage.RU,
    onDateChange: (LocalDate) -> Unit,
    onHoursChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val text = AppText.of(language)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val presets = listOf(7.5, 11.0, 11.5)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (state.originalDate == null) text.newShift else text.editShift,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = text.oneDayHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = RussianDateFormatter.fullDate(state.date, language),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.dateLabel) },
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) { Text(text.choose) }
                    },
                )
                Text(text.quickPick, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { hours ->
                        val selected = state.hoursText.replace(',', '.').toDoubleOrNull()?.let {
                            kotlin.math.abs(it - hours) < 0.051
                        } == true
                        FilterChip(
                            selected = selected,
                            onClick = { onHoursChange(HoursFormatter.formatNumber(hours, language)) },
                            label = { Text(HoursFormatter.formatCompact(hours, language)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.hoursText,
                    onValueChange = onHoursChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text.hoursLabel) },
                    singleLine = true,
                    supportingText = { Text(text.hoursFieldHint) },
                )
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text(text.save)
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text(text.deleteRecord, color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(text.close)
                }
            }
        }
    }
    if (showDatePicker) {
        val initial = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis
                        if (millis != null) {
                            onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showDatePicker = false
                    },
                ) { Text(text.done) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(text.cancel) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
