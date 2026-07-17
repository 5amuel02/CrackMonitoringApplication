package com.example.seismicaplication

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DetectionLogAdapter(private val list: List<DetectionLog>) :
    RecyclerView.Adapter<DetectionLogAdapter.ViewHolder>() {

    private val DARK_CARD            = Color.parseColor("#121A33")
    private val LIGHT_CARD           = Color.parseColor("#FFFFFF")
    private val DARK_TEXT_PRIMARY    = Color.parseColor("#F1F5F9")
    private val DARK_TEXT_SECONDARY  = Color.parseColor("#94A3B8")
    private val LIGHT_TEXT_PRIMARY   = Color.parseColor("#0F172A")
    private val LIGHT_TEXT_SECONDARY = Color.parseColor("#64748B")

    /** Warna severity berdasarkan confidence — selaras dengan ambang di ConfidenceGaugeView. */
    private fun severityColor(confidence: Double): Int = when {
        confidence >= 0.80 -> Color.parseColor("#22C55E")   // tinggi — hijau
        confidence >= 0.50 -> Color.parseColor("#F59E0B")   // sedang — amber
        else                -> Color.parseColor("#EF4444")   // rendah — merah
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView          = view as CardView
        val imgDetection: ImageView = view.findViewById(R.id.img_log)
        val tvLabel: TextView       = view.findViewById(R.id.tv_label)
        val tvConfidence: TextView  = view.findViewById(R.id.tv_confidence)
        val tvTime: TextView        = view.findViewById(R.id.tv_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detection_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item    = list[position].data
        val context = holder.itemView.context
        val dark    = ThemeManager.isDarkMode(context)

        holder.card.setCardBackgroundColor(if (dark) DARK_CARD else LIGHT_CARD)
        holder.tvLabel.setTextColor(if (dark) DARK_TEXT_PRIMARY else LIGHT_TEXT_PRIMARY)
        holder.tvTime.setTextColor(if (dark) DARK_TEXT_SECONDARY else LIGHT_TEXT_SECONDARY)

        holder.tvLabel.text      = item.label.uppercase()
        holder.tvConfidence.text = String.format("%.1f%%", item.confidence * 100)
        holder.tvConfidence.setTextColor(severityColor(item.confidence))
        holder.tvTime.text       = item.timestamp.replace("T", " ").take(16)

        Glide.with(context)
            .load(item.image_url)
            .placeholder(android.R.drawable.progress_indeterminate_horizontal)
            .error(android.R.drawable.stat_notify_error)
            .into(holder.imgDetection)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetectionDetailActivity::class.java).apply {
                putExtra("label",      item.label)
                putExtra("confidence", item.confidence)
                putExtra("timestamp",  item.timestamp)
                putExtra("image_url",  item.image_url)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = list.size

    // ✅ Dipakai DetectionLogActivity untuk cek apakah ada data baru masuk
    fun getFirstTimestamp(): String = if (list.isNotEmpty()) list[0].data.timestamp else ""
}