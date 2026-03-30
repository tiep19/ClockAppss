package com.example.clockapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AlarmStorage {

    private const val PREF_NAME = "alarm_prefs"
    private const val KEY_ALARMS = "alarms"
    private const val KEY_NEXT_ID = "next_id"

    fun saveAlarms(context: Context, alarms: List<AlarmData>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (alarm in alarms) {
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("hour", alarm.hour)
                put("minute", alarm.minute)
                put("isEnabled", alarm.isEnabled)
                put("label", alarm.label)
                put("ringtoneUri", alarm.ringtoneUri)
                put("ringtoneName", alarm.ringtoneName)
                put("vibrate", alarm.vibrate)
                put("repeatDays", alarm.repeatDays.joinToString("") { if (it) "1" else "0" })
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_ALARMS, jsonArray.toString()).apply()
    }

    fun loadAlarms(context: Context): MutableList<AlarmData> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ALARMS, "[]") ?: "[]"
        val list = mutableListOf<AlarmData>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val repeatStr = obj.optString("repeatDays", "0000000")
                val repeatDays = BooleanArray(7) { idx ->
                    idx < repeatStr.length && repeatStr[idx] == '1'
                }
                list.add(
                    AlarmData(
                        id           = obj.getInt("id"),
                        hour         = obj.getInt("hour"),
                        minute       = obj.getInt("minute"),
                        isEnabled    = obj.getBoolean("isEnabled"),
                        label        = obj.getString("label"),
                        ringtoneUri  = obj.optString("ringtoneUri", "default"),
                        ringtoneName = obj.optString("ringtoneName", "Nhạc chuông mặc định"),
                        vibrate      = obj.getBoolean("vibrate"),
                        repeatDays   = repeatDays
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveNextId(context: Context, nextId: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_NEXT_ID, nextId).apply()
    }

    fun loadNextId(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_NEXT_ID, 1)
    }
}