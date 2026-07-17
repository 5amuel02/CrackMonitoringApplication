package com.example.seismicaplication

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class DetectionDetailActivity : AppCompatActivity() {

    /** Warna severity berdasarkan confidence — selaras dengan ConfidenceGaugeView & DetectionLogAdapter. */
    private fun severityColor(confidence: Double): Int = when {
        confidence >= 0.80 -> Color.parseColor("#22C55E")
        confidence >= 0.50 -> Color.parseColor("#F59E0B")
        else                -> Color.parseColor("#EF4444")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_detail)

        val imgDetail = findViewById<ImageView>(R.id.img_detail)
        val tvLabel = findViewById<TextView>(R.id.tv_detail_label)
        val tvConfidence = findViewById<TextView>(R.id.tv_detail_confidence)
        val tvTimestamp = findViewById<TextView>(R.id.tv_detail_timestamp)
        val tvImageUrl = findViewById<TextView>(R.id.tv_detail_image_url)

        // 🔥 GANTI JADI DOUBLE
        val label = intent.getStringExtra("label")
        val confidence = intent.getDoubleExtra("confidence", 0.0)
        val timestamp = intent.getStringExtra("timestamp")
        val imageUrl = intent.getStringExtra("image_url")

        tvLabel.text = label?.uppercase()
        tvConfidence.text = String.format("%.1f%%", confidence * 100)
        tvConfidence.setTextColor(severityColor(confidence))
        tvTimestamp.text = timestamp?.replace("T", " ")?.take(16)
        tvImageUrl.text = imageUrl

        Glide.with(this)
            .load(imageUrl)
            .placeholder(android.R.drawable.progress_indeterminate_horizontal)
            .error(android.R.drawable.stat_notify_error)
            .into(imgDetail)
    }
}