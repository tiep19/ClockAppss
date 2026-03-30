package com.example.clockapp

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISMISS = "com.example.clockapp.DISMISS"
        const val ACTION_SNOOZE  = "com.example.clockapp.SNOOZE"
        const val ACTION_ALARM_DISMISSED = "com.example.clockapp.ALARM_DISMISSED"
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
                stopAlarm(alarmId)
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val alarmId      = intent.getIntExtra(EXTRA_ALARM_ID, -1)
                val snoozeMinutes = intent.getIntExtra("alarm_snooze_minutes", 5)
                stopAlarm(alarmId, sendBroadcast = false)
                scheduleSnooze(intent, snoozeMinutes)
                return START_NOT_STICKY
            }
        }

        val alarmId          = intent?.getIntExtra(EXTRA_ALARM_ID, -1) ?: -1
        val label            = intent?.getStringExtra("alarm_label") ?: "Chuông báo"
        val ringtone         = intent?.getStringExtra("alarm_ringtone") ?: "default"
        val ringtoneEnabled  = intent?.getBooleanExtra("alarm_ringtone_enabled", true) ?: true
        val vibrateEnabled   = intent?.getBooleanExtra("alarm_vibrate", true) ?: true
        val snoozeMinutes    = intent?.getIntExtra("alarm_snooze_minutes", 5) ?: 5

        startForeground(NOTIFICATION_ID, buildNotification(label, alarmId, snoozeMinutes))
        if (ringtoneEnabled) startRingtone(ringtone)
        if (vibrateEnabled)  startVibration()

        // Tự tắt sau 1 phút
        Handler(Looper.getMainLooper()).postDelayed({ stopAlarm(alarmId) }, 60_000)

        return START_STICKY
    }

    private fun buildNotification(label: String, alarmId: Int, snoozeMinutes: Int): Notification {
        // BUG FIX #5: truyền alarmId vào dismiss PendingIntent
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPI = PendingIntent.getService(
            this, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra("alarm_snooze_minutes", snoozeMinutes)
        }
        val snoozePI = PendingIntent.getService(
            this, 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("alarm_label", label)
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra("alarm_snooze_minutes", snoozeMinutes)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPI = PendingIntent.getActivity(
            this, 2, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeLabel = "Tạm dừng $snoozeMinutes phút"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setColor(ContextCompat.getColor(this, R.color.accent_purple))
            .setContentTitle("Chuông báo")
            .setContentText(if (label.isNotEmpty()) label else "Đang reo chuông...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPI, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Bỏ qua", dismissPI)
            .addAction(android.R.drawable.ic_popup_sync, snoozeLabel, snoozePI)
            .build()
    }

    private fun startRingtone(ringtoneUri: String) {
        try {
            val uri: Uri = when (ringtoneUri) {
                "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                "none"    -> return // Không phát nhạc
                else      -> Uri.parse(ringtoneUri) // ✅ URI nhạc từ máy
            }
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback về nhạc mặc định nếu URI lỗi
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(applicationContext, defaultUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun scheduleSnooze(intent: Intent, snoozeMinutes: Int) {
        val alarmId     = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val triggerTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = "com.example.clockapp.ALARM_TRIGGER"
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra("alarm_label", "Báo lại")
            putExtra("alarm_snooze_minutes", snoozeMinutes)
        }
        val pi = PendingIntent.getBroadcast(
            this, alarmId + 1000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (getSystemService(ALARM_SERVICE) as AlarmManager)
            .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
    }

    private fun stopAlarm(alarmId: Int, sendBroadcast: Boolean = true) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        if (sendBroadcast && alarmId != -1) {
            val broadcast = Intent(ACTION_ALARM_DISMISSED).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                setPackage(packageName)
            }
            sendBroadcast(broadcast)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Thông báo chuông báo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi chuông báo reo"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}