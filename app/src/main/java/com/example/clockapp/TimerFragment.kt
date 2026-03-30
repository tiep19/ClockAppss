package com.example.clockapp

import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class TimerFragment : Fragment() {

    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false
    private var remainingMs = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timer, container, false)

        val npHour = view.findViewById<NumberPicker>(R.id.np_hour)
        val npMin = view.findViewById<NumberPicker>(R.id.np_minute)
        val npSec = view.findViewById<NumberPicker>(R.id.np_second)
        val tvCountdown = view.findViewById<TextView>(R.id.tv_countdown)
        val btnStart = view.findViewById<Button>(R.id.btn_timer_start)
        val pickerLayout = view.findViewById<View>(R.id.picker_layout)

        npHour.minValue = 0; npHour.maxValue = 99
        npMin.minValue = 0; npMin.maxValue = 59
        npSec.minValue = 0; npSec.maxValue = 59

        // ✅ Ép màu chữ NumberPicker thành trắng bằng code
        setNumberPickerTextColor(npHour)
        setNumberPickerTextColor(npMin)
        setNumberPickerTextColor(npSec)

        fun updateCountdown(ms: Long) {
            val h = ms / 3600000
            val m = (ms % 3600000) / 60000
            val s = (ms % 60000) / 1000
            tvCountdown.text = String.format("%02d:%02d:%02d", h, m, s)
        }

        btnStart.setOnClickListener {
            if (!isRunning) {
                if (remainingMs == 0L) {
                    val h = npHour.value.toLong()
                    val m = npMin.value.toLong()
                    val s = npSec.value.toLong()
                    remainingMs = (h * 3600 + m * 60 + s) * 1000
                }
                if (remainingMs == 0L) {
                    Toast.makeText(requireContext(), "Vui lòng chọn thời gian", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                pickerLayout.visibility = View.GONE
                tvCountdown.visibility = View.VISIBLE
                isRunning = true
                btnStart.text = "Tạm dừng"

                countDownTimer = object : CountDownTimer(remainingMs, 100) {
                    override fun onTick(ms: Long) {
                        remainingMs = ms
                        updateCountdown(ms)
                    }
                    override fun onFinish() {
                        remainingMs = 0
                        isRunning = false
                        btnStart.text = "Bắt đầu"
                        tvCountdown.text = "00:00:00"
                        pickerLayout.visibility = View.VISIBLE
                        tvCountdown.visibility = View.GONE
                        Toast.makeText(requireContext(), "⏰ Hết giờ!", Toast.LENGTH_LONG).show()
                    }
                }.start()

            } else {
                countDownTimer?.cancel()
                isRunning = false
                btnStart.text = "Tiếp tục"
            }
        }

        // Nhấn giữ để đặt lại
        btnStart.setOnLongClickListener {
            countDownTimer?.cancel()
            isRunning = false
            remainingMs = 0
            btnStart.text = "Bắt đầu"
            tvCountdown.visibility = View.GONE
            pickerLayout.visibility = View.VISIBLE
            npHour.value = 0
            npMin.value = 0
            npSec.value = 0
            true
        }

        return view
    }

    // ✅ Hàm ép màu chữ NumberPicker thành trắng
    private fun setNumberPickerTextColor(picker: NumberPicker) {
        try {
            val field = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
            field.isAccessible = true
            (field.get(picker) as android.graphics.Paint).color =
                android.graphics.Color.WHITE
            picker.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}