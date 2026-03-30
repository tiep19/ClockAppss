package com.example.clockapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var etSearch: EditText
    private lateinit var rvSearch: RecyclerView
    private lateinit var btnSearchToggle: ImageButton
    private lateinit var btnBack: ImageButton

    private val searchResults = mutableListOf<WorldCity>()
    private lateinit var searchAdapter: CitySearchAdapter

    // Danh sách thành phố đã thêm (nhận từ WorldClockFragment)
    private val addedCityNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        // Nhận danh sách thành phố đã thêm
        intent.getStringArrayListExtra("added_cities")?.let {
            addedCityNames.addAll(it)
        }

        etSearch = findViewById(R.id.et_search_city)
        rvSearch = findViewById(R.id.rv_search_results)
        btnSearchToggle = findViewById(R.id.btn_search_toggle)
        btnBack = findViewById(R.id.btn_back_city)

        // Setup search adapter
        searchAdapter = CitySearchAdapter(searchResults) { city ->
            returnCity(city)
        }
        rvSearch.layoutManager = LinearLayoutManager(this)
        rvSearch.adapter = searchAdapter

        // Nút back
        btnBack.setOnClickListener { finish() }

        // Toggle tìm kiếm
        btnSearchToggle.setOnClickListener {
            if (etSearch.visibility == View.GONE) {
                etSearch.visibility = View.VISIBLE
                rvSearch.visibility = View.VISIBLE
                etSearch.requestFocus()
            } else {
                etSearch.visibility = View.GONE
                rvSearch.visibility = View.GONE
                etSearch.setText("")
                searchResults.clear()
                searchAdapter.notifyDataSetChanged()
            }
        }

        // Lắng nghe gõ tìm kiếm
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                searchResults.clear()
                if (query.isNotEmpty()) {
                    ALL_CITIES.filter {
                        it.name.lowercase().contains(query) ||
                                it.country.lowercase().contains(query)
                    }.also { searchResults.addAll(it) }
                }
                searchAdapter.notifyDataSetChanged()

                // Focus map vào kết quả đầu tiên
                if (searchResults.isNotEmpty() && ::googleMap.isInitialized) {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(searchResults[0].lat, searchResults[0].lng), 5f
                        )
                    )
                }
            }
        })

        // Khởi tạo bản đồ
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_picker) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = false

        // Cắm marker tất cả thành phố
        for (city in ALL_CITIES) {
            val pos = LatLng(city.lat, city.lng)
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(city.name)
                    .snippet(getTimeString(city))
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            if (addedCityNames.contains(city.name))
                                BitmapDescriptorFactory.HUE_VIOLET  // đã thêm → tím
                            else
                                BitmapDescriptorFactory.HUE_AZURE   // chưa → xanh
                        )
                    )
            )
            marker?.tag = city
        }

        // Custom InfoWindow
        googleMap.setInfoWindowAdapter(CityInfoWindowAdapter(this))

        // Nhấn marker → hiện InfoWindow
        googleMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }

        // Nhấn InfoWindow → trả về thành phố được chọn
        googleMap.setOnInfoWindowClickListener { marker ->
            val city = marker.tag as? WorldCity ?: return@setOnInfoWindowClickListener
            if (addedCityNames.contains(city.name)) {
                Toast.makeText(this, "${city.name} đã có trong danh sách", Toast.LENGTH_SHORT).show()
            } else {
                returnCity(city)
            }
        }

        // Camera mặc định
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(15.0, 100.0), 3f)
        )
    }

    private fun getTimeString(city: WorldCity): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(city.timeZone)
        }
        return sdf.format(Date())
    }

    // Trả kết quả về WorldClockFragment
    private fun returnCity(city: WorldCity) {
        val intent = Intent().apply {
            putExtra("city_name", city.name)
            putExtra("city_country", city.country)
            putExtra("city_timezone", city.timeZone)
            putExtra("city_lat", city.lat)
            putExtra("city_lng", city.lng)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}