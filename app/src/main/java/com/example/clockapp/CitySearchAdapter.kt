package com.example.clockapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CitySearchAdapter(
    private val cities: List<WorldCity>,
    private val onSelect: (WorldCity) -> Unit
) : RecyclerView.Adapter<CitySearchAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_search_city_name)
        val tvCountry: TextView = v.findViewById(R.id.tv_search_city_country)
        val tvGmt: TextView = v.findViewById(R.id.tv_search_city_gmt)
        val tvTime: TextView = v.findViewById(R.id.tv_search_city_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city_search, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val city = cities[position]
        val tz = TimeZone.getTimeZone(city.timeZone)
        val offsetMs = tz.getOffset(System.currentTimeMillis())
        val offsetHours = offsetMs / 3600000
        val offsetMins = Math.abs((offsetMs % 3600000) / 60000)
        val gmtStr = if (offsetMins == 0) "GMT${if (offsetHours >= 0) "+" else ""}$offsetHours"
        else "GMT${if (offsetHours >= 0) "+" else ""}$offsetHours:${String.format("%02d", offsetMins)}"

        holder.tvName.text = city.name
        holder.tvCountry.text = "${city.country} / ${city.name}"
        holder.tvGmt.text = gmtStr

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = tz }
        holder.tvTime.text = sdf.format(Date())

        holder.itemView.setOnClickListener { onSelect(city) }
    }

    override fun getItemCount() = cities.size
}