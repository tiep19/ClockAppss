package com.example.clockapp

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Các thiết lập để hiển thị màn hình báo thức đè lên Lock Screen (khóa màn hình)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Giữ sáng màn hình khi chuông đang reo
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_alarm)

        // Lấy dữ liệu tên báo thức từ Service truyền sang
        val label = intent.getStringExtra("alarm_label") ?: ""
        val alarmId = intent.getIntExtra("alarm_id", -1)

        // Ánh xạ View
        val tvLabel = findViewById<TextView>(R.id.tv_alarm_label)
        val btnDismiss = findViewById<MaterialButton>(R.id.btn_dismiss)
        val btnSnooze = findViewById<MaterialButton>(R.id.btn_snooze)

        // Hiển thị tên báo thức
        tvLabel.text = if (label.isNotEmpty()) label else "Đồng hồ"

        // Xử lý sự kiện bấm nút Bỏ qua
        btnDismiss.setOnClickListener {
            stopAlarmService()
            finish()
        }

        // Xử lý sự kiện bấm nút Tạm dừng (Snooze)
        btnSnooze.setOnClickListener {
            val snoozeIntent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_SNOOZE
                putExtra("alarm_id", alarmId)
            }
            startService(snoozeIntent)
            finish()
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        startService(intent) // Gọi lệnh DISMISS đến service
    }
}