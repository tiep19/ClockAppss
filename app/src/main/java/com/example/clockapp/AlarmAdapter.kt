package com.example.clockapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class AlarmAdapter(
    private val alarms: MutableList<AlarmData>,
    private val onToggle: (AlarmData, Boolean) -> Unit,
    private val onClick: (AlarmData) -> Unit,
    private val onDelete: (AlarmData) -> Unit,
    private val onCloseBedtime: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_BEDTIME = 0
        const val VIEW_TYPE_ALARM = 1
    }

    inner class BedtimeViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvWake: TextView = v.findViewById(R.id.tv_bedtime_wake)
        val btnClose: ImageButton = v.findViewById(R.id.btn_close_bedtime)
    }

    inner class AlarmViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTime: TextView = v.findViewById(R.id.tv_alarm_time)
        val tvDays: TextView = v.findViewById(R.id.tv_alarm_days)
        val swEnable: SwitchMaterial = v.findViewById(R.id.sw_alarm_enable)
        // ✅ Nút xóa trên mỗi item
        val btnDelete: ImageButton = v.findViewById(R.id.btn_delete_alarm)
    }

    override fun getItemViewType(position: Int) =
        if (position == 0) VIEW_TYPE_BEDTIME else VIEW_TYPE_ALARM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_BEDTIME) {
            BedtimeViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_alarm_bedtime, parent, false)
            )
        } else {
            AlarmViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_alarm, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BedtimeViewHolder) {
            holder.tvWake.text = "07:00"
            holder.btnClose.setOnClickListener { onCloseBedtime() }
            return
        }

        val alarm = alarms[position - 1]
        val h = holder as AlarmViewHolder

        h.tvTime.text = String.format("%02d:%02d", alarm.hour, alarm.minute)
        h.tvDays.text = getNextAlarmDay(alarm)

        val ctx = h.itemView.context
        val textColor = if (alarm.isEnabled)
            ctx.getColor(R.color.white)
        else
            ctx.getColor(R.color.text_secondary)
        h.tvTime.setTextColor(textColor)

        h.swEnable.setOnCheckedChangeListener(null)
        h.swEnable.isChecked = alarm.isEnabled
        h.swEnable.setOnCheckedChangeListener { _, checked ->
            alarm.isEnabled = checked
            h.tvTime.setTextColor(
                if (checked) ctx.getColor(R.color.white)
                else ctx.getColor(R.color.text_secondary)
            )
            onToggle(alarm, checked)
        }

        // ✅ Click vào item để sửa
        h.itemView.setOnClickListener { onClick(alarm) }

        // ✅ Nút thùng rác để xóa
        h.btnDelete.setOnClickListener { onDelete(alarm) }
    }

    override fun getItemCount() = alarms.size + 1

    private fun getNextAlarmDay(alarm: AlarmData): String {
        val cal = Calendar.getInstance()
        if (alarm.hour < cal.get(Calendar.HOUR_OF_DAY) ||
            (alarm.hour == cal.get(Calendar.HOUR_OF_DAY)
                    && alarm.minute <= cal.get(Calendar.MINUTE))
        ) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "T.2"
            Calendar.TUESDAY -> "T.3"
            Calendar.WEDNESDAY -> "T.4"
            Calendar.THURSDAY -> "T.5"
            Calendar.FRIDAY -> "T.6"
            Calendar.SATURDAY -> "T.7"
            else -> "CN"
        }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        return "$dayOfWeek, $day Th$month"
    }
}