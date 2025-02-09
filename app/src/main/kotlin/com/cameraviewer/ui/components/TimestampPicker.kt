package com.cameraviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimestampPicker(
    onTimestampSelected: (LocalDateTime) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDateTime.now()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Selected: ${selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Button(onClick = { showDatePicker = true }) {
            Text("Select Time")
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                ),
                showModeToggle = false
            )
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = { hour, minute ->
                selectedDate = selectedDate.withHour(hour).withMinute(minute)
                onTimestampSelected(selectedDate)
                showTimePicker = false
            }
        )
    }
}