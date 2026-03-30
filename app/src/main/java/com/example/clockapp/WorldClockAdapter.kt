package com.example.clockapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class WorldClockAdapter(
    private val cities: MutableList<WorldCity>,
    private val onDelete: (WorldCity) -> Unit
) : RecyclerView.Adapter<WorldClockAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvCity: TextView    = v.findViewById(R.id.tv_city_name)
        val tvCountry: TextView = v.findViewById(R.id.tv_city_country)
        val tvTime: TextView    = v.findViewById(R.id.tv_city_time)
        val tvDiff: TextView    = v.findViewById(R.id.tv_city_diff)
        val tvDate: TextView    = v.findViewById(R.id.tv_city_date)
        val btnDelete: ImageButton = v.findViewById(R.id.btn_delete_city)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_world_clock, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val city = cities[position]
        val tz   = TimeZone.getTimeZone(city.timeZone)
        val now  = Date()

        // Giờ hiện tại
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = tz }
        holder.tvTime.text = sdfTime.format(now)

        holder.tvCity.text    = city.name
        holder.tvCountry.text = city.country

        // Ngày
        val localCal = Calendar.getInstance()
        val cityCal  = Calendar.getInstance(tz)
        val dayDiff  = cityCal.get(Calendar.DAY_OF_YEAR) - localCal.get(Calendar.DAY_OF_YEAR)
        val sdfDate  = SimpleDateFormat("EEE, dd/MM", Locale("vi", "VN")).apply { timeZone = tz }
        holder.tvDate.text = when (dayDiff) {
            0  -> "Hôm nay"
            1  -> "Ngày mai"
            -1 -> "Hôm qua"
            else -> sdfDate.format(now)
        }

        // ✅ Ép kiểu rõ ràng thành Long
        val cityOffset  = tz.getOffset(now.time).toLong()
        val localOffset = TimeZone.getDefault().getOffset(now.time).toLong()
        val diffMs      = cityOffset - localOffset
        val diffHours   = (diffMs / 3600000L).toInt()
        val diffMins    = Math.abs((diffMs % 3600000L) / 60000L).toInt()

        holder.tvDiff.text = when {
            diffMs == 0L -> "Múi giờ địa phương"
            diffMins == 0 ->
                if (diffHours > 0) "Đi trước $diffHours giờ"
                else "Đi sau ${Math.abs(diffHours)} giờ"
            else -> {
                val h = Math.abs(diffHours)
                if (diffMs > 0L) "Đi trước $h giờ $diffMins phút"
                else "Đi sau $h giờ $diffMins phút"
            }
        }

        holder.btnDelete.setOnClickListener { onDelete(city) }
    }

    override fun getItemCount() = cities.size
}