package com.example.seismicaplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.switchmaterial.SwitchMaterial

// ✅ PERUBAHAN: extend BaseActivity
class SettingsActivity : BaseActivity() {

    private lateinit var etStreamIp: TextInputEditText
    private lateinit var etStreamPort: TextInputEditText
    private lateinit var etDataIp: TextInputEditText
    private lateinit var etDataPort: TextInputEditText
    private lateinit var etInterval: TextInputEditText
    private lateinit var btnSaveServer: MaterialButton
    private lateinit var btnResetServer: MaterialButton
    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var tvAppVersion: TextView
    private lateinit var tvDeveloper: TextView
    private lateinit var tvContact: TextView

    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "server_prefs"

        const val KEY_STREAM_IP   = "stream_ip"
        const val KEY_STREAM_PORT = "stream_port"
        const val KEY_DATA_IP     = "data_ip"
        const val KEY_DATA_PORT   = "data_port"
        const val KEY_INTERVAL    = "poll_interval"
        const val KEY_DARK_MODE   = "dark_mode"

        const val DEFAULT_STREAM_IP   = "0.0.0.0"
        const val DEFAULT_STREAM_PORT = "5000"
        const val DEFAULT_DATA_IP     = "0.0.0.0"
        const val DEFAULT_DATA_PORT   = "5000"
        const val DEFAULT_INTERVAL    = "5"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        initViews()
        loadSettings()
        setupListeners()
        setupBottomNavigation()

        // ✅ Apply theme setelah layout siap
        applyCurrentTheme()
    }

    private fun initViews() {
        etStreamIp    = findViewById(R.id.et_stream_ip)
        etStreamPort  = findViewById(R.id.et_stream_port)
        etDataIp      = findViewById(R.id.et_data_ip)
        etDataPort    = findViewById(R.id.et_data_port)
        etInterval    = findViewById(R.id.et_poll_interval)
        btnSaveServer = findViewById(R.id.btn_save_server)
        btnResetServer = findViewById(R.id.btn_reset_server)
        switchDarkMode = findViewById(R.id.switch_dark_mode)
        tvAppVersion  = findViewById(R.id.tv_app_version)
        tvDeveloper   = findViewById(R.id.tv_developer)
        tvContact     = findViewById(R.id.tv_contact)
    }

    private fun loadSettings() {
        etStreamIp.setText(prefs.getString(KEY_STREAM_IP, DEFAULT_STREAM_IP))
        etStreamPort.setText(prefs.getString(KEY_STREAM_PORT, DEFAULT_STREAM_PORT))
        etDataIp.setText(prefs.getString(KEY_DATA_IP, DEFAULT_DATA_IP))
        etDataPort.setText(prefs.getString(KEY_DATA_PORT, DEFAULT_DATA_PORT))
        etInterval.setText(prefs.getString(KEY_INTERVAL, DEFAULT_INTERVAL))
        switchDarkMode.isChecked = prefs.getBoolean(KEY_DARK_MODE, false)

        tvAppVersion.text = "Versi 1.0.0"
        tvDeveloper.text  = "Samuel A.S. Manik"
        tvContact.text    = "samuelm@telkomuniversity.ac.id"
    }

    private fun setupListeners() {
        btnSaveServer.setOnClickListener {
            val streamIp   = etStreamIp.text.toString().trim()
            val streamPort = etStreamPort.text.toString().trim()
            val dataIp     = etDataIp.text.toString().trim()
            val dataPort   = etDataPort.text.toString().trim()
            val interval   = etInterval.text.toString().trim()

            if (streamIp.isEmpty() || streamPort.isEmpty() ||
                dataIp.isEmpty() || dataPort.isEmpty() || interval.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().apply {
                putString(KEY_STREAM_IP, streamIp)
                putString(KEY_STREAM_PORT, streamPort)
                putString(KEY_DATA_IP, dataIp)
                putString(KEY_DATA_PORT, dataPort)
                putString(KEY_INTERVAL, interval)
                apply()
            }
            Toast.makeText(this, "✓ Konfigurasi server disimpan", Toast.LENGTH_SHORT).show()
        }

        btnResetServer.setOnClickListener {
            etStreamIp.setText(DEFAULT_STREAM_IP)
            etStreamPort.setText(DEFAULT_STREAM_PORT)
            etDataIp.setText(DEFAULT_DATA_IP)
            etDataPort.setText(DEFAULT_DATA_PORT)
            etInterval.setText(DEFAULT_INTERVAL)

            prefs.edit().apply {
                putString(KEY_STREAM_IP, DEFAULT_STREAM_IP)
                putString(KEY_STREAM_PORT, DEFAULT_STREAM_PORT)
                putString(KEY_DATA_IP, DEFAULT_DATA_IP)
                putString(KEY_DATA_PORT, DEFAULT_DATA_PORT)
                putString(KEY_INTERVAL, DEFAULT_INTERVAL)
                apply()
            }
            Toast.makeText(this, "Reset ke pengaturan default", Toast.LENGTH_SHORT).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // ✅ Simpan preferensi
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()

            // ✅ Langsung terapkan theme tanpa restart Activity
            applyCurrentTheme()

            val msg = if (isChecked) "Mode Gelap aktif" else "Mode Terang aktif"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_settings

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> true
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_logs -> {
                    startActivity(Intent(this, DetectionLogActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_notif -> {
                    startActivity(Intent(this, NotificationActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0); finish(); true
                }
                else -> false
            }
        }
    }
}