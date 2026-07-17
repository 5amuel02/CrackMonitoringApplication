package com.example.seismicaplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo  = findViewById<ImageView>(R.id.iv_splash_logo)
        val title = findViewById<TextView>(R.id.tv_splash_title)
        val sub   = findViewById<TextView>(R.id.tv_splash_subtitle)

        // Animasi muncul
        listOf(logo, title, sub).forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 40f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay((i * 150).toLong())
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // ✅ Selalu ke Login setiap buka app — tidak cek isLoggedIn
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }, 2000)
    }
}