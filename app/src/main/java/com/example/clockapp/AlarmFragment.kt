package com.example.clockapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class AlarmFragment : Fragment() {

    private val alarms = mutableListOf<AlarmData>()
    private lateinit var adapter: AlarmAdapter
    private var nextId = 1
    private var showBedtime = true
    private lateinit var tvAllOff: TextView

    // ✅ Lưu tạm dialog view để cập nhật sau khi chọn nhạc
    private var currentDialogTvRingtone: TextView? = null
    private var currentRingtoneUri: String = "default"
    private var currentRingtoneName: String = "Nhạc chuông mặc định"

    // ✅ Launcher mở RingtoneManager picker
    private val ringtoneLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data
                ?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

            if (uri != null) {
                currentRingtoneUri = uri.toString()
                // Lấy tên nhạc
                val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
                currentRingtoneName = ringtone?.getTitle(requireContext())
                    ?: "Nhạc đã chọn"
            } else {
                // Người dùng chọn "Không có"
                currentRingtoneUri = "none"
                currentRingtoneName = "Không có"
            }
            currentDialogTvRingtone?.text = currentRingtoneName
        }
    }

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlarmService.ACTION_ALARM_DISMISSED) {
                val alarmId = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1)
                if (alarmId == -1) return
                val alarm = alarms.find { it.id == alarmId }
                if (alarm != null) {
                    alarm.isEnabled = false
                    AlarmStorage.saveAlarms(requireContext(), alarms)
                    adapter.notifyDataSetChanged()
                    updateAllOffVisibility()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)

        tvAllOff = view.findViewById(R.id.tv_all_alarms_off)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_alarms)
        val btnAdd = view.findViewById<ImageButton>(R.id.btn_add_alarm)

        alarms.clear()
        alarms.addAll(AlarmStorage.loadAlarms(requireContext()))
        nextId = AlarmStorage.loadNextId(requireContext())

        adapter = AlarmAdapter(
            alarms,
            onToggle = { alarm, enabled ->
                if (enabled) scheduleAlarm(alarm) else cancelAlarm(alarm)
                AlarmStorage.saveAlarms(requireContext(), alarms)
                updateAllOffVisibility()
            },
            onClick = { alarm -> showAlarmDialog(alarm) },
            onDelete = { alarm -> showDeleteDialog(alarm) },
            onCloseBedtime = {
                showBedtime = false
                adapter.notifyDataSetChanged()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        btnAdd.setOnClickListener { showAlarmDialog(null) }
        updateAllOffVisibility()
        return view
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(AlarmService.ACTION_ALARM_DISMISSED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                dismissReceiver, filter, Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            requireContext().registerReceiver(dismissReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try { requireContext().unregisterReceiver(dismissReceiver) }
        catch (e: Exception) { e.printStackTrace() }
    }

    // ✅ Dùng chung 1 dialog cho cả thêm và sửa
    private fun showAlarmDialog(editAlarm: AlarmData?) {
        val isEdit = editAlarm != null
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_alarm, null)

        // Ánh xạ view
        val timePicker      = dialogView.findViewById<TimePicker>(R.id.time_picker)
        val etLabel         = dialogView.findViewById<EditText>(R.id.et_alarm_label)
        val rgRingtoneType  = dialogView.findViewById<RadioGroup>(R.id.rg_ringtone_type)
        val rbDefault       = dialogView.findViewById<RadioButton>(R.id.rb_ringtone_default)
        val rbDevice        = dialogView.findViewById<RadioButton>(R.id.rb_ringtone_device)
        val tvRingtoneName  = dialogView.findViewById<TextView>(R.id.tv_selected_ringtone)
        val swVibrate       = dialogView.findViewById<SwitchMaterial>(R.id.sw_vibrate)
        val dayCbs = listOf(
            dialogView.findViewById<CheckBox>(R.id.cb_mon),
            dialogView.findViewById<CheckBox>(R.id.cb_tue),
            dialogView.findViewById<CheckBox>(R.id.cb_wed),
            dialogView.findViewById<CheckBox>(R.id.cb_thu),
            dialogView.findViewById<CheckBox>(R.id.cb_fri),
            dialogView.findViewById<CheckBox>(R.id.cb_sat),
            dialogView.findViewById<CheckBox>(R.id.cb_sun)
        )

        timePicker.setIs24HourView(true)

        // Reset giá trị tạm
        currentDialogTvRingtone = tvRingtoneName

        // ── Điền dữ liệu nếu là chỉnh sửa ──
        if (isEdit && editAlarm != null) {
            timePicker.hour   = editAlarm.hour
            timePicker.minute = editAlarm.minute
            etLabel.setText(editAlarm.label)
            swVibrate.isChecked = editAlarm.vibrate
            currentRingtoneUri  = editAlarm.ringtoneUri
            currentRingtoneName = editAlarm.ringtoneName
            tvRingtoneName.text = editAlarm.ringtoneName
            editAlarm.repeatDays.forEachIndexed { i, v -> dayCbs[i].isChecked = v }

            if (editAlarm.ringtoneUri == "default") rbDefault.isChecked = true
            else rbDevice.isChecked = true
        } else {
            currentRingtoneUri  = "default"
            currentRingtoneName = "Nhạc chuông mặc định"
            tvRingtoneName.text = "Nhạc chuông mặc định"
            rbDefault.isChecked = true
        }

        // ── Xử lý chọn nhạc ──
        rgRingtoneType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_ringtone_default -> {
                    currentRingtoneUri  = "default"
                    currentRingtoneName = "Nhạc chuông mặc định"
                    tvRingtoneName.text = "Nhạc chuông mặc định"
                }
                R.id.rb_ringtone_device -> {
                    // ✅ Mở RingtoneManager picker — nhạc có sẵn trên máy
                    openRingtonePicker()
                }
            }
        }

        // ── Hiển thị dialog ──
        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Sửa chuông báo" else "Thêm chuông báo")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val hour       = timePicker.hour
                val minute     = timePicker.minute
                val label      = etLabel.text.toString()
                val vibrate    = swVibrate.isChecked
                val repeatDays = BooleanArray(7) { dayCbs[it].isChecked }

                if (isEdit && editAlarm != null) {
                    cancelAlarm(editAlarm)
                    editAlarm.hour         = hour
                    editAlarm.minute       = minute
                    editAlarm.label        = label
                    editAlarm.ringtoneUri  = currentRingtoneUri
                    editAlarm.ringtoneName = currentRingtoneName
                    editAlarm.vibrate      = vibrate
                    editAlarm.repeatDays   = repeatDays
                    if (editAlarm.isEnabled) scheduleAlarm(editAlarm)
                } else {
                    val alarm = AlarmData(
                        id           = nextId++,
                        hour         = hour,
                        minute       = minute,
                        isEnabled    = true,
                        label        = label,
                        ringtoneUri  = currentRingtoneUri,
                        ringtoneName = currentRingtoneName,
                        vibrate      = vibrate,
                        repeatDays   = repeatDays
                    )
                    alarms.add(alarm)
                    scheduleAlarm(alarm)
                    AlarmStorage.saveNextId(requireContext(), nextId)
                    tvAllOff.visibility = View.GONE
                }

                AlarmStorage.saveAlarms(requireContext(), alarms)
                adapter.notifyDataSetChanged()
                Toast.makeText(
                    requireContext(),
                    "${if (isEdit) "Đã cập nhật" else "Đã đặt"} báo lúc ${
                        String.format("%02d:%02d", timePicker.hour, timePicker.minute)
                    }",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Hủy") { _, _ ->
                currentDialogTvRingtone = null
            }
            .show()
    }

    // ✅ Mở picker nhạc chuông có sẵn trên máy
    private fun openRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_ALARM or
                        RingtoneManager.TYPE_RINGTONE or
                        RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn nhạc chuông")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)

            // Nếu đang có URI được chọn thì highlight nó
            if (currentRingtoneUri != "default" && currentRingtoneUri != "none") {
                putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    Uri.parse(currentRingtoneUri)
                )
            }
        }
        ringtoneLauncher.launch(intent)
    }

    private fun showDeleteDialog(alarm: AlarmData) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa chuông báo")
            .setMessage(
                "Xóa báo thức lúc ${String.format("%02d:%02d", alarm.hour, alarm.minute)}?"
            )
            .setPositiveButton("Xóa") { _, _ ->
                cancelAlarm(alarm)
                alarms.remove(alarm)
                AlarmStorage.saveAlarms(requireContext(), alarms)
                adapter.notifyDataSetChanged()
                updateAllOffVisibility()
                Toast.makeText(requireContext(), "Đã xóa chuông báo", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateAllOffVisibility() {
        tvAllOff.visibility =
            if (alarms.none { it.isEnabled }) View.VISIBLE else View.GONE
    }

    private fun scheduleAlarm(alarm: AlarmData) {
        val alarmManager =
            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            action = "com.example.clockapp.ALARM_TRIGGER"
            putExtra(AlarmService.EXTRA_ALARM_ID, alarm.id)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_ringtone", alarm.ringtoneUri)
        }
        val pi = PendingIntent.getBroadcast(
            requireContext(), alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, alarm.hour)
            set(java.util.Calendar.MINUTE, alarm.minute)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis())
                add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi
        )
    }

    private fun cancelAlarm(alarm: AlarmData) {
        val alarmManager =
            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            requireContext(), alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }
}