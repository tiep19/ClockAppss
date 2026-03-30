package com.example.clockapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    // ✅ Khởi tạo sẵn 4 fragment, không tạo lại mỗi lần chuyển tab
    private val alarmFragment = AlarmFragment()
    private val worldClockFragment = WorldClockFragment()
    private val stopwatchFragment = StopwatchFragment()
    private val timerFragment = TimerFragment()

    private var activeFragment: Fragment = alarmFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()

        // ✅ Add tất cả fragment một lần, ẩn 3 cái, chỉ hiện 1
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, alarmFragment, "alarm")
            add(R.id.fragment_container, worldClockFragment, "world")
            add(R.id.fragment_container, stopwatchFragment, "stopwatch")
            add(R.id.fragment_container, timerFragment, "timer")
            hide(worldClockFragment)
            hide(stopwatchFragment)
            hide(timerFragment)
        }.commit()

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selected = when (tab?.position) {
                    0 -> alarmFragment
                    1 -> worldClockFragment
                    2 -> stopwatchFragment
                    3 -> timerFragment
                    else -> alarmFragment
                }
                // ✅ Chỉ hide/show, không replace → dữ liệu không bị mất
                supportFragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(selected)
                    .commit()
                activeFragment = selected
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
}