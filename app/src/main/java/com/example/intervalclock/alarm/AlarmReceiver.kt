package com.example.intervalclock.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.intervalclock.R
import com.example.intervalclock.data.AlarmRepository
import com.example.intervalclock.ui.trigger.AlarmTriggerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        if (alarmId == -1) return

        Log.d("AlarmReceiver", "Alarm received: $alarmId")

        // Start Foreground Service to play sound and show notification
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Reschedule the next occurrence
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm != null && alarm.isEnabled) {
               alarmScheduler.schedule(alarm)
            }
        }
    }
}
