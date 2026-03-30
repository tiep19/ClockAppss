package com.example.clockapp

import java.io.Serializable

data class AlarmData(
    val id: Int,
    var hour: Int,
    var minute: Int,
    var isEnabled: Boolean = true,
    var label: String = "",
    var ringtoneUri: String = "default", // ✅ Lưu URI thay vì tên
    var ringtoneName: String = "Nhạc chuông mặc định", // ✅ Tên hiển thị
    var vibrate: Boolean = true,
    var repeatDays: BooleanArray = BooleanArray(7) { false }
) : Serializable