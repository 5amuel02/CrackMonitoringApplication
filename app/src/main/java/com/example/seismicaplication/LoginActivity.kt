package com.example.seismicaplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail   : TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail    : TextInputEditText
    private lateinit var etPassword : TextInputEditText
    private lateinit var btnLogin   : MaterialButton
    private lateinit var tvRegister : TextView
    private lateinit var tvForgot   : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tilEmail    = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail     = findViewById(R.id.et_email)
        etPassword  = findViewById(R.id.et_password)
        btnLogin    = findViewById(R.id.btn_login)
        tvRegister  = findViewById(R.id.tv_register)
        tvForgot    = findViewById(R.id.tv_forgot)
    }

    private fun setupListeners() {

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            // Validasi kosong
            if (email.isEmpty()) {
                tilEmail.error = "Email tidak boleh kosong"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                tilPassword.error = "Password tidak boleh kosong"
                return@setOnClickListener
            }
            tilEmail.error    = null
            tilPassword.error = null

            // Cek belum pernah register
            if (!AuthPreferences.isEmailRegistered(this, email)) {
                tilEmail.error = "Email belum terdaftar. Silakan Register dulu."
                return@setOnClickListener
            }

            // Validasi email + password
            if (AuthPreferences.validateLogin(this, email, password)) {
                // ✅ Simpan email akun yang sedang aktif
                AuthPreferences.setCurrentEmail(this, email)
                // Sync data akun ke profile_prefs agar ProfileActivity bisa baca
                AuthPreferences.syncToProfilePrefs(this)

                Toast.makeText(this, "Selamat datang, ${AuthPreferences.getName(this)}!", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
            } else {
                tilPassword.error = "Email atau password salah"
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgot.setOnClickListener {
            Toast.makeText(this, "Hubungi admin untuk reset password.", Toast.LENGTH_LONG).show()
        }

        // Hapus error saat user mulai mengetik
        etEmail.setOnFocusChangeListener   { _, _ -> tilEmail.error = null }
        etPassword.setOnFocusChangeListener { _, _ -> tilPassword.error = null }
    }
}