package com.sagar.babystatus.core.ui

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sagar.babystatus.core.designsystem.component.PickerField
import com.sagar.babystatus.core.ui.format.TimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Whether the user's device shows 24-hour time. */
@Composable
fun rememberIs24Hour(): Boolean {
    val context = LocalContext.current
    return remember(context) { DateFormat.is24HourFormat(context) }
}

/**
 * The log sheet's "When" field. Tapping picks a date, then a time. Only dates in
 * [minDate]..[today] can be picked; a time later than now is flagged by the form.
 */
@Composable
fun DateTimeField(
    label: String,
    value: Instant,
    onValueChange: (Instant) -> Unit,
    zone: ZoneId,
    today: LocalDate,
    minDate: LocalDate?,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val is24Hour = rememberIs24Hour()
    var pickedDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val current = value.atZone(zone)

    PickerField(
        label = label,
        value = TimeFormatter.dateTime(value, zone, today, is24Hour),
        onClick = { showDatePicker = true },
        error = error,
        modifier = modifier,
    )
    if (showDatePicker) {
        DatePickerDialogFor(
            initial = current.toLocalDate(),
            minDate = minDate,
            maxDate = today,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                pickedDate = date
            },
        )
    }
    pickedDate?.let { date ->
        TimePickerDialogFor(
            initial = current.toLocalTime(),
            is24Hour = is24Hour,
            onDismiss = { pickedDate = null },
            onConfirm = { time ->
                pickedDate = null
                onValueChange(date.atTime(time).atZone(zone).toInstant())
            },
        )
    }
}

/** A date-only field ("Date", "Birth date"). */
@Composable
fun DateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    today: LocalDate,
    minDate: LocalDate?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    showRelativeDay: Boolean = true,
    error: String? = null,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    PickerField(
        label = label,
        value = when {
            value == null -> ""
            showRelativeDay -> TimeFormatter.dateWithDay(value, today)
            else -> TimeFormatter.fullDate(value)
        },
        placeholder = placeholder,
        onClick = { showPicker = true },
        error = error,
        modifier = modifier,
    )
    if (showPicker) {
        DatePickerDialogFor(
            initial = value ?: today,
            minDate = minDate,
            maxDate = today,
            onDismiss = { showPicker = false },
            onConfirm = {
                showPicker = false
                onValueChange(it)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogFor(
    initial: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // DatePicker works in UTC millis at the start of the day.
    val selectable = remember(minDate, maxDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = utcTimeMillis.toUtcDate()
                return !date.isAfter(maxDate) && (minDate == null || !date.isBefore(minDate))
            }

            override fun isSelectableYear(year: Int): Boolean =
                year <= maxDate.year && (minDate == null || year >= minDate.year)
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it.toUtcDate()) } },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogFor(
    initial: LocalTime,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        text = { TimePicker(state = state) },
    )
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
