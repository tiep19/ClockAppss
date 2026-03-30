package com.example.clockapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class LapData(
    val index: Int,
    val lapTimeMs: Long,    // thời gian của riêng vòng này
    val totalTimeMs: Long   // tổng thời gian tính đến vòng này
)

class StopwatchFragment : Fragment() {

    // ── Trạng thái ──────────────────────────────────────────────────────────
    private var isRunning = false
    private var startTime = 0L
    private var elapsed   = 0L          // tổng ms đã chạy trước lần start gần nhất
    private var lapStart  = 0L          // ms tổng khi bắt đầu vòng hiện tại

    private val laps = mutableListOf<LapData>()

    // ── Handler tick 10ms ───────────────────────────────────────────────────
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            val total   = currentTotal()
            val lapTime = total - lapStart
            tvMainTime?.text    = formatTime(total)
            tvCurrentLap?.text  = formatTime(lapTime)
            handler.postDelayed(this, 10)
        }
    }

    // ── View refs (nullable để tránh leak) ──────────────────────────────────
    private var tvMainTime:   TextView? = null
    private var tvCurrentLap: TextView? = null
    private var btnLeft:      Button?   = null
    private var btnRight:     Button?   = null
    private lateinit var lapAdapter: LapAdapter

    // ────────────────────────────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stopwatch, container, false)

        tvMainTime   = view.findViewById(R.id.tv_stopwatch_time)
        tvCurrentLap = view.findViewById(R.id.tv_current_lap)
        btnLeft      = view.findViewById(R.id.btn_lap_reset)
        btnRight     = view.findViewById(R.id.btn_start_stop)

        val rvLaps = view.findViewById<RecyclerView>(R.id.rv_laps)
        lapAdapter = LapAdapter(laps)
        rvLaps.layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout    = true
            stackFromEnd     = false
        }
        rvLaps.adapter = lapAdapter

        // Trạng thái ban đầu
        updateUI()

        btnRight?.setOnClickListener { onRightClick() }
        btnLeft?.setOnClickListener  { onLeftClick()  }

        return view
    }

    // ── Nút phải: Start / Stop ───────────────────────────────────────────────
    private fun onRightClick() {
        if (!isRunning) {
            // Bắt đầu / Tiếp tục
            startTime = System.currentTimeMillis()
            if (elapsed == 0L) lapStart = 0L   // lần đầu reset cả lapStart
            isRunning = true
            handler.post(ticker)
        } else {
            // Dừng
            elapsed  += System.currentTimeMillis() - startTime
            isRunning = false
            handler.removeCallbacks(ticker)
            // Cập nhật hiển thị tĩnh
            val total = elapsed
            tvMainTime?.text   = formatTime(total)
            tvCurrentLap?.text = formatTime(total - lapStart)
        }
        updateUI()
    }

    // ── Nút trái: Bấm vòng / Đặt lại ────────────────────────────────────────
    private fun onLeftClick() {
        if (isRunning) {
            // Bấm vòng
            val total   = currentTotal()
            val lapTime = total - lapStart
            laps.add(LapData(laps.size + 1, lapTime, total))
            lapStart = total
            lapAdapter.notifyItemInserted(laps.size - 1)
            lapAdapter.highlightExtremes()
        } else {
            // Đặt lại
            elapsed  = 0L
            lapStart = 0L
            laps.clear()
            lapAdapter.notifyDataSetChanged()
            tvMainTime?.text   = "00:00.00"
            tvCurrentLap?.text = "00:00.00"
        }
        updateUI()
    }

    // ── Cập nhật màu & text nút ──────────────────────────────────────────────
    private fun updateUI() {
        val ctx = context ?: return
        when {
            // Đang chạy
            isRunning -> {
                btnRight?.text = "Dừng"
                btnRight?.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.button_stop)
                    )
                btnLeft?.text      = "Bấm"
                btnLeft?.isEnabled = true
            }
            // Đã dừng, có thời gian
            elapsed > 0L -> {
                btnRight?.text = "Tiếp tục"
                btnRight?.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.button_start)
                    )
                btnLeft?.text      = "Đặt lại"
                btnLeft?.isEnabled = true
            }
            // Chưa bắt đầu
            else -> {
                btnRight?.text = "Bắt đầu"
                btnRight?.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.button_start)
                    )
                btnLeft?.text      = "Bấm"
                btnLeft?.isEnabled = false
            }
        }
    }

    private fun currentTotal(): Long =
        if (isRunning) elapsed + (System.currentTimeMillis() - startTime)
        else elapsed

    // ── Format mm:ss.SS ──────────────────────────────────────────────────────
    private fun formatTime(ms: Long): String {
        val minutes    = (ms / 60000) % 60
        val seconds    = (ms / 1000)  % 60
        val hundredths = (ms / 10)    % 100
        return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    override fun onStop() {
        super.onStop()
        // Dừng tick khi fragment không visible (tránh memory leak)
        if (isRunning) {
            elapsed += System.currentTimeMillis() - startTime
            startTime = 0L
        }
        handler.removeCallbacks(ticker)
    }

    override fun onStart() {
        super.onStart()
        // Resume tick nếu đang chạy
        if (isRunning) {
            startTime = System.currentTimeMillis()
            handler.post(ticker)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(ticker)
        tvMainTime   = null
        tvCurrentLap = null
        btnLeft      = null
        btnRight     = null
    }

    // ────────────────────────────────────────────────────────────────────────
    // RecyclerView Adapter cho bảng vòng lặp
    // ────────────────────────────────────────────────────────────────────────
    inner class LapAdapter(
        private val data: List<LapData>
    ) : RecyclerView.Adapter<LapAdapter.VH>() {

        private var fastestIdx = -1   // index trong list (0-based)
        private var slowestIdx = -1

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvIndex: TextView = v.findViewById(R.id.tv_lap_index)
            val tvLap:   TextView = v.findViewById(R.id.tv_lap_time)
            val tvTotal: TextView = v.findViewById(R.id.tv_lap_total)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_lap, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val lap = data[position]
            holder.tvIndex.text = String.format("%02d", lap.index)
            holder.tvLap.text   = formatTime(lap.lapTimeMs)
            holder.tvTotal.text = formatTime(lap.totalTimeMs)

            // Màu highlight
            val ctx = holder.itemView.context
            val color = when (position) {
                fastestIdx -> ContextCompat.getColor(ctx, R.color.lap_fastest) // xanh lam
                slowestIdx -> ContextCompat.getColor(ctx, R.color.lap_slowest) // đỏ
                else       -> ContextCompat.getColor(ctx, R.color.text_primary)
            }
            holder.tvIndex.setTextColor(color)
            holder.tvLap.setTextColor(color)
            holder.tvTotal.setTextColor(color)
        }

        override fun getItemCount() = data.size

        /** Tính lại vòng nhanh nhất / chậm nhất và notify */
        fun highlightExtremes() {
            if (data.size < 2) {
                fastestIdx = -1
                slowestIdx = -1
                notifyDataSetChanged()
                return
            }
            var minMs = Long.MAX_VALUE
            var maxMs = Long.MIN_VALUE
            data.forEachIndexed { i, lap ->
                if (lap.lapTimeMs < minMs) { minMs = lap.lapTimeMs; fastestIdx = i }
                if (lap.lapTimeMs > maxMs) { maxMs = lap.lapTimeMs; slowestIdx = i }
            }
            notifyDataSetChanged()
        }
    }
}