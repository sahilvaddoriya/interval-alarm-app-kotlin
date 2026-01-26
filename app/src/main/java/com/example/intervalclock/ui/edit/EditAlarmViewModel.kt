package com.example.intervalclock.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intervalclock.alarm.AlarmScheduler
import com.example.intervalclock.data.AlarmEntity
import com.example.intervalclock.data.AlarmRepository
import com.example.intervalclock.data.Converters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class EditAlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val alarmId: Int = savedStateHandle["alarmId"] ?: -1

    private val _uiState = MutableStateFlow(EditAlarmUiState())
    val uiState: StateFlow<EditAlarmUiState> = _uiState.asStateFlow()

    init {
        // Optional: Load initial if savedstate handles it, but we primarily expect manual load call now.
        if (alarmId != -1) {
            loadAlarm(alarmId)
        } else {
             _uiState.value = EditAlarmUiState(isEditing = false) // explicit default
        }
    }

    fun loadAlarm(id: Int?) {
        if (id == null || id == -1) {
            _uiState.value = EditAlarmUiState(isEditing = false)
            return
        }
        
        viewModelScope.launch {
            val alarm = repository.getAlarmById(id)
            if (alarm != null) {
                _uiState.value = EditAlarmUiState(
                    id = alarm.id,
                    label = alarm.label,
                    startTimeInMinutes = alarm.startTimeInMinutes,
                    endTimeInMinutes = alarm.endTimeInMinutes,
                    intervalInMinutes = alarm.intervalInMinutes,
                    daysOfWeek = alarm.daysOfWeek,
                    isEditing = true,
                    isAutoDismissEnabled = alarm.isAutoDismissEnabled,
                    autoDismissSeconds = alarm.autoDismissSeconds
                )
            } else {
                 _uiState.value = EditAlarmUiState(isEditing = false)
            }
        }
    }

    fun onLabelChange(label: String) {
        _uiState.value = _uiState.value.copy(label = label)
    }

    fun onStartTimeChange(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(startTimeInMinutes = hour * 60 + minute)
    }

    fun onEndTimeChange(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(endTimeInMinutes = hour * 60 + minute)
    }

    fun onIntervalChange(interval: Int) {
        _uiState.value = _uiState.value.copy(intervalInMinutes = interval)
    }

    fun onDayToggle(day: DayOfWeek) {
        val currentDays = _uiState.value.daysOfWeek.toMutableSet()
        if (currentDays.contains(day)) {
            currentDays.remove(day)
        } else {
            currentDays.add(day)
        }
        _uiState.value = _uiState.value.copy(daysOfWeek = currentDays)
    }

    fun onAutoDismissToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isAutoDismissEnabled = enabled,
            // Reset to default 30 if enabled and current is 0/invalid? Or just keep current.
            autoDismissSeconds = if (enabled && _uiState.value.autoDismissSeconds <= 0) 30 else _uiState.value.autoDismissSeconds
        )
    }

    fun onAutoDismissSecondsChange(seconds: Int) {
        _uiState.value = _uiState.value.copy(autoDismissSeconds = seconds)
    }

    fun saveAlarm(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val alarm = AlarmEntity(
                id = if (state.isEditing) state.id else 0,
                label = state.label,
                startTimeInMinutes = state.startTimeInMinutes,
                endTimeInMinutes = state.endTimeInMinutes,
                intervalInMinutes = state.intervalInMinutes,
                daysOfWeek = state.daysOfWeek,
                isEnabled = true,
                isAutoDismissEnabled = state.isAutoDismissEnabled,
                autoDismissSeconds = state.autoDismissSeconds
            )

            if (state.isEditing) {
                repository.updateAlarm(alarm)
            } else {
                val newId = repository.insertAlarm(alarm)
                // If ID is needed for scheduling immediately, we might need to fetch it or use the returned ID
                // For scheduler, we need the ID inside the entity
                alarmScheduler.schedule(alarm.copy(id = newId.toInt()))
            }
            
            if (state.isEditing) {
                 alarmScheduler.schedule(alarm)
            }
            
            onSuccess()
        }
    }
    
    fun deleteAlarm(onSuccess: () -> Unit) {
         if (!_uiState.value.isEditing) return
         viewModelScope.launch {
             val state = _uiState.value
             // We construct a temporary entity to cancel/delete
             val alarm = AlarmEntity(
                id = state.id,
                startTimeInMinutes = state.startTimeInMinutes,
                endTimeInMinutes = state.endTimeInMinutes,
                intervalInMinutes = state.intervalInMinutes,
                daysOfWeek = state.daysOfWeek
             )
             alarmScheduler.cancel(alarm)
             repository.deleteAlarm(alarm)
             onSuccess()
         }
    }
}

data class EditAlarmUiState(
    val id: Int = 0,
    val label: String = "",
    val startTimeInMinutes: Int = 9 * 60, // 9:00 AM
    val endTimeInMinutes: Int = 17 * 60, // 5:00 PM
    val intervalInMinutes: Int = 30,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val isEditing: Boolean = false,
    val isAutoDismissEnabled: Boolean = false,
    val autoDismissSeconds: Int = 30
)
