package com.example.intervalclock.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.intervalclock.R
import com.example.intervalclock.ui.trigger.AlarmTriggerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmService : Service() {

    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var ringtone: Ringtone? = null

    @javax.inject.Inject
    lateinit var repository: com.example.intervalclock.data.AlarmRepository

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
         val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
         wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "IntervalAlarmClock::AlarmServiceWakeLock"
         )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val action = intent?.action

        if (action == "STOP_ALARM") {
            val dismissIntent = Intent("ACTION_DISMISS_ALARM_UI")
            dismissIntent.setPackage(packageName)
            sendBroadcast(dismissIntent)
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Acquire wake lock to keep CPU running while alarm rings
        // Use a timeout (e.g., 10 minutes) to be safe against battery drain if service not stopped
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)

        if (alarmId != -1) {
            playRingtone()
            showNotification(alarmId)
            
            // Auto Dismiss Logic
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val alarm = repository.getAlarmById(alarmId)
                if (alarm != null && alarm.isAutoDismissEnabled && alarm.autoDismissSeconds > 0) {
                     kotlinx.coroutines.delay(alarm.autoDismissSeconds * 1000L)
                     if (ringtone?.isPlaying == true) { 
                         // Check if still playing to avoid stopping if user already dismissed
                         val dismissIntent = Intent("ACTION_DISMISS_ALARM_UI")
                         dismissIntent.setPackage(packageName)
                         sendBroadcast(dismissIntent)
                         stopSelf()
                     }
                }
            }
        }

        return START_STICKY
    }

    private fun playRingtone() {
        if (ringtone == null) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ringtone = RingtoneManager.getRingtone(applicationContext, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone?.isLooping = true
                }
                ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            if (ringtone?.isPlaying == false) {
                ringtone?.play()
            }
        }
    }

    private fun showNotification(alarmId: Int) {
        val fullScreenIntent = Intent(this, AlarmTriggerActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop the service from the notification
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            this.action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "ALARM_CHANNEL_ID_V4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Interval Alarm High Priority"
            val descriptionText = "Immediate alarm notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                // Disable sound on the notification channel to prevent double ringing (echo)
                // because we are playing the ringtone manually in playRingtone()
                setSound(null, null) 
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Interval Alarm")
            .setContentText("Alarm is ringing")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_launcher, "Dismiss", stopPendingIntent)
            .setOngoing(true)
            .build()
            
        startForeground(alarmId, notification)
        
        // Force Activity Start for Unlocked state (Best effort for Android 10+)
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
             // Fallback handled by FullScreenIntent
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}
