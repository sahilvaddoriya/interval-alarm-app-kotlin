package com.example.intervalclock.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.intervalclock.data.AlarmEntity
import kotlinx.coroutines.launch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import com.example.intervalclock.ui.edit.EditAlarmViewModel
import com.example.intervalclock.ui.edit.EditAlarmSheetContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel = hiltViewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // State for permissions
    var hasExactAlarmPermission by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    var canDrawOverlays by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    var canUseFullScreenIntent by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
                notificationManager.canUseFullScreenIntent()
            } else {
                true
            }
        )
    }
    
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                 // Check Exact Alarm Permission
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                    hasExactAlarmPermission = alarmManager.canScheduleExactAlarms()
                }
                
                // Check Overlay Permission
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    canDrawOverlays = android.provider.Settings.canDrawOverlays(context)
                }
                
                // Check Full Screen Intent Permission
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
                    canUseFullScreenIntent = notificationManager.canUseFullScreenIntent()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // We need the EditViewModel here to pass manual loading commands
    val editViewModel: EditAlarmViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms", style = MaterialTheme.typography.titleLarge) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    editViewModel.loadAlarm(null) // New Alarm
                    showBottomSheet = true
                },
                modifier = Modifier.size(72.dp), 
                containerColor = com.example.intervalclock.ui.theme.ClockPink,
                contentColor = com.example.intervalclock.ui.theme.ClockDarkIcon
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(42.dp))
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasExactAlarmPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        )
                        context.startActivity(intent)
                    }
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Warning, contentDescription = null)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                        Text("Permission needed to schedule exact alarms. Tap to grant.", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            

            
            
            if (!canDrawOverlays) {
                 androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                        )
                        intent.data = android.net.Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    }
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                        Text("Permission needed to show alarm while using phone. Tap to grant.", color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }

            if (!canUseFullScreenIntent) {
                 androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                        )
                        intent.data = android.net.Uri.parse("package:${context.packageName}")
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to general settings
                            val fallback = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            fallback.data = android.net.Uri.parse("package:${context.packageName}")
                            context.startActivity(fallback)
                        }
                    }
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Warning, contentDescription = null)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                        Text("Permission needed for Full Screen Alarm. Tap to grant.", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            
            if (alarms.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add, // Or a better "Empty" icon like AlarmOff
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = "No alarms",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to add one",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp, start = 16.dp, end = 16.dp), // Add bottom padding for FAB
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmItem(
                            alarm = alarm,
                            onToggle = { isEnabled -> viewModel.toggleAlarm(alarm, isEnabled) },
                            onClick = { 
                                editViewModel.loadAlarm(alarm.id)
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }
        }
        
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = com.example.intervalclock.ui.theme.ClockDarkGrey, // Match card color or darker
                tonalElevation = 0.dp
            ) {
                EditAlarmSheetContent(
                    onDismiss = {
                        scope.launch { 
                            try {
                                sheetState.hide() 
                            } catch (e: Exception) {
                                // Ignore illegal state or cancellation if already hidden
                            }
                        }.invokeOnCompletion { 
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    },
                    viewModel = editViewModel
                )
            }
        }
    }
}
