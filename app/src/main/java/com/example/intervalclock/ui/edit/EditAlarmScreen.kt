package com.example.intervalclock.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.intervalclock.data.AlarmEntity
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmSheetContent(
    onDismiss: () -> Unit,
    viewModel: EditAlarmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val focusRequester = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }

    // State for Time Picker Dialogs
    var showStartPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showEndPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        // We delay slightly to let the TimeInputs compose and try to take focus, then we steal it back
        kotlinx.coroutines.delay(100) 
        focusRequester.requestFocus()
        focusManager.clearFocus() 
    }

    if (showStartPicker) {
        AlarmTimePickerCustomDialog(
            onDismiss = { showStartPicker = false },
            onConfirm = { hour, minute ->
                viewModel.onStartTimeChange(hour, minute)
                showStartPicker = false
            },
            initialHour = uiState.startTimeInMinutes / 60,
            initialMinute = uiState.startTimeInMinutes % 60
        )
    }

    if (showEndPicker) {
        AlarmTimePickerCustomDialog(
            onDismiss = { showEndPicker = false },
            onConfirm = { hour, minute ->
                viewModel.onEndTimeChange(hour, minute)
                showEndPicker = false
            },
            initialHour = uiState.endTimeInMinutes / 60,
            initialMinute = uiState.endTimeInMinutes % 60
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dummy focusable to catch initial focus
        Box(
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .size(1.dp)
                .alpha(0f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
            // Top Row with Time Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeCard(
                    label = "Start Time",
                    timeInMinutes = uiState.startTimeInMinutes,
                    onClick = { showStartPicker = true }
                )
                TimeCard(
                    label = "End Time",
                    timeInMinutes = uiState.endTimeInMinutes,
                    onClick = { showEndPicker = true }
                )
            }
        Spacer(modifier = Modifier.height(24.dp))

        // Days
        DayOfWeekSelector(
            selectedDays = uiState.daysOfWeek,
            onDayToggle = { viewModel.onDayToggle(it) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Settings Group
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Label
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Default.Edit, 
                    title = "Label",
                    value = uiState.label.ifEmpty { "Alarm" },
                    onClick = { /* TODO: Open Dialog for Label */ } 
                ) {
                    OutlinedTextField(
                        value = uiState.label,
                        onValueChange = { viewModel.onLabelChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Interval
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Default.Refresh, 
                    title = "Interval",
                    value = "${uiState.intervalInMinutes} min",
                    onClick = {}
                ) {
                     OutlinedTextField(
                        value = uiState.intervalInMinutes.toString(),
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                viewModel.onIntervalChange(it.toIntOrNull() ?: 0)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                // Auto Dismiss
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Default.Notifications,
                    title = "Auto Dismiss",
                    value = if (uiState.isAutoDismissEnabled) "${uiState.autoDismissSeconds}s" else "Off",
                    onClick = { viewModel.onAutoDismissToggle(!uiState.isAutoDismissEnabled) },
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = uiState.isAutoDismissEnabled,
                            onCheckedChange = { viewModel.onAutoDismissToggle(it) }
                        )
                    }
                )
                
                if (uiState.isAutoDismissEnabled) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Spacer(modifier = Modifier.width(48.dp))
                         OutlinedTextField(
                            value = uiState.autoDismissSeconds.toString(),
                            onValueChange = {
                                if (it.all { char -> char.isDigit() }) {
                                    viewModel.onAutoDismissSecondsChange(it.toIntOrNull() ?: 0)
                                }
                            },
                            label = { Text("Seconds") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Bottom Buttons (Delete Left, Save Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.isEditing) {
                Button(
                    onClick = { viewModel.deleteAlarm(onSuccess = onDismiss) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                 TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Button(
                onClick = { viewModel.saveAlarm(onSuccess = onDismiss) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun TimeCard(
    label: String,
    timeInMinutes: Int,
    onClick: () -> Unit
) {
    val hour = timeInMinutes / 60
    val minute = timeInMinutes % 60
    val isPm = hour >= 12
    val displayHour = if (hour % 12 == 0) 12 else hour % 12
    val displayMinute = minute.toString().padStart(2, '0')
    val amPm = if (isPm) "PM" else "AM"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .width(120.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
             Text(
                text = "$displayHour:$displayMinute", 
                style = MaterialTheme.typography.headlineLarge, 
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = amPm, 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTimePickerCustomDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    var inputType by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) } // false = Dial

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = androidx.compose.material3.AlertDialogDefaults.containerColor,
            tonalElevation = androidx.compose.material3.AlertDialogDefaults.TonalElevation,
            modifier = Modifier.width(androidx.compose.ui.unit.IntOffset(0, 0).run { 320.dp }) // Fixed width for standard picker look
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Select time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (inputType) {
                        TimeInput(state = timePickerState)
                    } else {
                        androidx.compose.material3.TimePicker(state = timePickerState)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.material3.IconButton(onClick = { inputType = !inputType }) {
                        androidx.compose.material3.Icon(
                            imageVector = if (inputType) androidx.compose.material.icons.Icons.Default.Refresh else androidx.compose.material.icons.Icons.Default.Edit, // Edit for keyboard, Schedule/Refresh for dial
                            contentDescription = "Toggle Input"
                        )
                    }
                    
                    Row {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) { Text("OK") }
                    }
                }
            }
        }
    }
}

// Helper to provide Icons.Default.Schedule since it might be missing
// Usually it's in Extended or we can use another icon.
// Refresh is a decent fallback for "Back to Dial". Or 'AccessTime'.

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null // For inline editing
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            //.clickable { onClick() } // Disable row click if we have inline input for now
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (content == null) {
                Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                 content()
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}


