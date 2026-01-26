package com.example.intervalclock.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intervalclock.alarm.AlarmScheduler
import com.example.intervalclock.data.AlarmEntity
import com.example.intervalclock.data.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = repository.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // Check only once on init, not continuously
            val currentAlarms = repository.getAllAlarms().first()
            if (currentAlarms.isEmpty()) {
                val defaultAlarm = AlarmEntity(
                    startTimeInMinutes = 9 * 60,
                    endTimeInMinutes = 17 * 60,
                    intervalInMinutes = 30,
                    daysOfWeek = java.time.DayOfWeek.values().toSet(),
                    isEnabled = false,
                    label = "Work Hours"
                )
                repository.insertAlarm(defaultAlarm)
            }
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
             if (isEnabled) {
                 alarmScheduler.schedule(updatedAlarm) // This will update DB with next trigger time
             } else {
                 alarmScheduler.cancel(updatedAlarm)
                 repository.updateAlarm(updatedAlarm.copy(nextTriggerTime = null))
             }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmScheduler.cancel(alarm)
            repository.deleteAlarm(alarm)
        }
    }
}
