package com.example.intervalclock.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String = "",
    val startTimeInMinutes: Int, // Minutes from midnight (0-1439)
    val endTimeInMinutes: Int,   // Minutes from midnight
    val intervalInMinutes: Int,
    val daysOfWeek: Set<DayOfWeek>, // Stored as a converter
    val isEnabled: Boolean = true,
    val nextTriggerTime: Long? = null, // Timestamp of the next scheduled occurrence
    val isAutoDismissEnabled: Boolean = false,
    val autoDismissSeconds: Int = 30
)
