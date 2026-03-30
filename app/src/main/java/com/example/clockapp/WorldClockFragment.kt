package com.example.clockapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class WorldClockFragment : Fragment() {

    private lateinit var adapter: WorldClockAdapter
    private val addedCities = mutableListOf<WorldCity>()
    private val handler = Handler(Looper.getMainLooper())
    private var tvLocalTime: TextView? = null
    private var tvLocalLabel: TextView? = null

    // ✅ Dùng ActivityResultLauncher thay startActivityForResult (không bị deprecated)
    private val mapLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val city = WorldCity(
                name     = data.getStringExtra("city_name") ?: return@registerForActivityResult,
                country  = data.getStringExtra("city_country") ?: "",
                timeZone = data.getStringExtra("city_timezone") ?: return@registerForActivityResult,
                lat      = data.getDoubleExtra("city_lat", 0.0),
                lng      = data.getDoubleExtra("city_lng", 0.0)
            )
            if (addedCities.none { it.name == city.name }) {
                addedCities.add(city)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Đã thêm ${city.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            tvLocalTime?.text = sdf.format(Date())
            tvLocalLabel?.text = TimeZone.getDefault()
                .getDisplayName(false, TimeZone.LONG, Locale("vi", "VN"))
            adapter.notifyDataSetChanged()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_world_clock, container, false)

        tvLocalTime  = view.findViewById(R.id.tv_local_time)
        tvLocalLabel = view.findViewById(R.id.tv_local_label)
        val rv    = view.findViewById<RecyclerView>(R.id.rv_world_clocks)
        val btnAdd = view.findViewById<ImageButton>(R.id.btn_add_city)

        // Mặc định thêm Hà Nội
        if (addedCities.isEmpty()) {
            ALL_CITIES.firstOrNull { it.name == "Hà Nội" }?.let {
                addedCities.add(it)
            }
        }

        adapter = WorldClockAdapter(addedCities) { city ->
            addedCities.remove(city)
            adapter.notifyDataSetChanged()
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // ✅ Mở Activity riêng thay vì Dialog
        btnAdd.setOnClickListener {
            val intent = Intent(requireContext(), MapPickerActivity::class.java).apply {
                // Gửi danh sách thành phố đã thêm để tô màu marker
                putStringArrayListExtra(
                    "added_cities",
                    ArrayList(addedCities.map { it.name })
                )
            }
            mapLauncher.launch(intent)
        }

        handler.post(tickRunnable)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(tickRunnable)
        tvLocalTime  = null
        tvLocalLabel = null
    }
}