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

    androidx.compose.runtime.LaunchedEffect(Unit) {
        // We delay slightly to let the TimeInputs compose and try to take focus, then we steal it back
        kotlinx.coroutines.delay(100) 
        focusRequester.requestFocus()
        focusManager.clearFocus() // Then clear it so no cursor is blinking anywhere
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
        
        // Handle is usually provided by ModalBottomSheet, so we might skip it or add Spacer
        Spacer(modifier = Modifier.height(16.dp))
        
        // Time Pickers Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Start Time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val startTimeState = rememberTimePickerState(
                initialHour = uiState.startTimeInMinutes / 60,
                initialMinute = uiState.startTimeInMinutes % 60,
                is24Hour = false
            )
            TimeInput(state = startTimeState)
            viewModel.onStartTimeChange(startTimeState.hour, startTimeState.minute)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("End Time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val endTimeState = rememberTimePickerState(
                initialHour = uiState.endTimeInMinutes / 60,
                initialMinute = uiState.endTimeInMinutes % 60,
                is24Hour = false
            )
            TimeInput(state = endTimeState)
            viewModel.onEndTimeChange(endTimeState.hour, endTimeState.minute)
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

