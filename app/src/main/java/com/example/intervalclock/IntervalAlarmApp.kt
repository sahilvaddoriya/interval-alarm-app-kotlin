package com.example.intervalclock

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class IntervalAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Interval Alarm"
            val descriptionText = "Channel for Interval Alarms"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("ALARM_CHANNEL_ID", name, importance).apply {
                description = descriptionText
                setSound(null, null) // Custom sound management in Activity usually, or set sound here
            }
            val notificationManager: android.app.NotificationManager =
                getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
