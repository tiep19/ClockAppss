package com.example.clockapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import java.text.SimpleDateFormat
import java.util.*

class CityInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? = null

    override fun getInfoContents(marker: Marker): View? {
        val city = marker.tag as? WorldCity ?: return null
        val view = LayoutInflater.from(context).inflate(R.layout.item_city_info_window, null)

        val tvName = view.findViewById<TextView>(R.id.tv_iw_city_name)
        val tvTime = view.findViewById<TextView>(R.id.tv_iw_city_time)
        val tvDate = view.findViewById<TextView>(R.id.tv_iw_city_date)
        val tvDiff = view.findViewById<TextView>(R.id.tv_iw_city_diff)
        val tvAdd  = view.findViewById<TextView>(R.id.tv_iw_add)

        val tz = TimeZone.getTimeZone(city.timeZone)
        val now = Date()

        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = tz }
        val sdfDate = SimpleDateFormat("EEE, dd/MM", Locale("vi", "VN")).apply { timeZone = tz }

        tvName.text = city.name
        tvTime.text = sdfTime.format(now)

        val localCal = Calendar.getInstance()
        val cityCal  = Calendar.getInstance(tz)
        val dayDiff  = cityCal.get(Calendar.DAY_OF_YEAR) - localCal.get(Calendar.DAY_OF_YEAR)
        tvDate.text = when (dayDiff) {
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

        tvDiff.text = when {
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

        tvAdd.text = "Thêm"
        return view
    }
}