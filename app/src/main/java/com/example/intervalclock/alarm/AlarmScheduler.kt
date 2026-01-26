package com.example.intervalclock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.intervalclock.data.AlarmEntity
import com.example.intervalclock.data.AlarmRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

interface AlarmScheduler {
    suspend fun schedule(alarm: AlarmEntity)
    fun cancel(alarm: AlarmEntity)
}

class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmRepository: AlarmRepository
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun schedule(alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        val nextTime = calculateNextAlarmTime(alarm)
        if (nextTime != null) {
            // Update the alarm entity with the next trigger time so UI can show it
            alarmRepository.updateAlarm(alarm.copy(nextTriggerTime = nextTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond()))

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_ID", alarm.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use setExactAndAllowWhileIdle for reliability
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Scheduled alarm ${alarm.id} for $nextTime")
            } catch (e: SecurityException) {
                Log.e("AlarmScheduler", "Permission not granted for exact alarm", e)
                // Fallback or request permission - for now we assume permission is granted via manifest
            }
        } else {
             Log.d("AlarmScheduler", "No valid next time found for alarm ${alarm.id}, possibly no days selected")
        }
    }

    override fun cancel(alarm: AlarmEntity) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }
    
    // Public for testing visibility if needed, or keeping private.
    // Calculations use LocalDateTime to handle day transitions correctly.
    fun calculateNextAlarmTime(alarm: AlarmEntity, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (alarm.daysOfWeek.isEmpty()) return null

        val startTime = LocalTime.ofSecondOfDay(alarm.startTimeInMinutes * 60L)
        val endTime = LocalTime.ofSecondOfDay(alarm.endTimeInMinutes * 60L)
        val interval = alarm.intervalInMinutes

        // Check if we are currently within the active window for today
        val todayStart = now.with(startTime).withSecond(0).withNano(0)
        val todayEnd = now.with(endTime).withSecond(0).withNano(0)

        // Case 1: Before today's start time and today is a selected day
        if (now.isBefore(todayStart) && alarm.daysOfWeek.contains(now.dayOfWeek)) {
            return todayStart
        }

        // Case 2: Within today's window and today is a selected day
        if (!now.isBefore(todayStart) && now.isBefore(todayEnd) && alarm.daysOfWeek.contains(now.dayOfWeek)) {
             // Calculate next interval
             if (interval <= 0) return null 
             val minutesSinceStart = ChronoUnit.MINUTES.between(todayStart, now)
             val intervalsPassed = minutesSinceStart / interval
             val nextIntervalStart = todayStart.plusMinutes((intervalsPassed + 1) * interval.toLong())
             
             if (nextIntervalStart.isBefore(todayEnd) || nextIntervalStart.isEqual(todayEnd)) {
                 return nextIntervalStart
             }
        }
        
        // Case 3: After today's window or today is not selected -> Find next day
        var nextDay = now.plusDays(1)
        while (true) {
            if (alarm.daysOfWeek.contains(nextDay.dayOfWeek)) {
                return nextDay.with(startTime).withSecond(0).withNano(0)
            }
            nextDay = nextDay.plusDays(1)
            // Safety break to prevent infinite loop
            if (ChronoUnit.DAYS.between(now, nextDay) > 8) return null
        }
    }
}
