package com.example.intervalclock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.intervalclock.data.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Boot completed, rescheduling alarms")
            CoroutineScope(Dispatchers.IO).launch {
                val alarms = alarmRepository.getAllAlarms().first()
                alarms.forEach { alarm ->
                    if (alarm.isEnabled) {
                        alarmScheduler.schedule(alarm)
                    }
                }
            }
        }
    }
}
