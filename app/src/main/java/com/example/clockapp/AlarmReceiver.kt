package com.example.clockapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "com.example.clockapp.ALARM_TRIGGER" ||
            action == Intent.ACTION_BOOT_COMPLETED) {

            val alarmId = intent.getIntExtra("alarm_id", -1)
            val label = intent.getStringExtra("alarm_label") ?: ""
            val ringtone = intent.getStringExtra("alarm_ringtone") ?: "default"

            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("alarm_label", label)
                putExtra("alarm_ringtone", ringtone)
            }
            context.startForegroundService(serviceIntent)
        }
    }
}