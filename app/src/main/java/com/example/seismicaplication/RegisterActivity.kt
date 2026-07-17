package com.example.seismicaplication

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilName      : TextInputLayout
    private lateinit var tilNim       : TextInputLayout
    private lateinit var tilUniversity: TextInputLayout
    private lateinit var tilRole      : TextInputLayout
    private lateinit var tilPhone     : TextInputLayout
    private lateinit var tilEmail     : TextInputLayout
    private lateinit var tilPassword  : TextInputLayout
    private lateinit var tilConfirm   : TextInputLayout

    private lateinit var etName      : TextInputEditText
    private lateinit var etNim       : TextInputEditText
    private lateinit var etUniversity: TextInputEditText
    private lateinit var etRole      : TextInputEditText
    private lateinit var etPhone     : TextInputEditText
    private lateinit var etEmail     : TextInputEditText
    private lateinit var etPassword  : TextInputEditText
    private lateinit var etConfirm   : TextInputEditText

    private lateinit var btnRegister : MaterialButton
    private lateinit var tvLogin     : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tilName       = findViewById(R.id.til_reg_name)
        tilNim        = findViewById(R.id.til_reg_nim)
        tilUniversity = findViewById(R.id.til_reg_university)
        tilRole       = findViewById(R.id.til_reg_role)
        tilPhone      = findViewById(R.id.til_reg_phone)
        tilEmail      = findViewById(R.id.til_reg_email)
        tilPassword   = findViewById(R.id.til_reg_password)
        tilConfirm    = findViewById(R.id.til_reg_confirm)

        etName       = findViewById(R.id.et_reg_name)
        etNim        = findViewById(R.id.et_reg_nim)
        etUniversity = findViewById(R.id.et_reg_university)
        etRole       = findViewById(R.id.et_reg_role)
        etPhone      = findViewById(R.id.et_reg_phone)
        etEmail      = findViewById(R.id.et_reg_email)
        etPassword   = findViewById(R.id.et_reg_password)
        etConfirm    = findViewById(R.id.et_reg_confirm)

        btnRegister  = findViewById(R.id.btn_register)
        tvLogin      = findViewById(R.id.tv_login)
    }

    private fun setupListeners() {

        btnRegister.setOnClickListener {
            if (!validateForm()) return@setOnClickListener

            val name       = etName.text.toString().trim()
            val nim        = etNim.text.toString().trim()
            val university = etUniversity.text.toString().trim()
            val role       = etRole.text.toString().trim()
            val phone      = etPhone.text.toString().trim()
            val email      = etEmail.text.toString().trim()
            val password   = etPassword.text.toString()

            // Cek email sudah dipakai
            if (AuthPreferences.isEmailRegistered(this, email)) {
                tilEmail.error = "Email sudah terdaftar"
                return@setOnClickListener
            }

            AuthPreferences.register(
                context    = this,
                name       = name,
                email      = email,
                password   = password,
                nim        = nim,
                university = university,
                role       = role,
                phone      = phone
            )

            Toast.makeText(this, "✓ Registrasi berhasil! Silakan login.", Toast.LENGTH_SHORT).show()

            // Balik ke Login
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }

        tvLogin.setOnClickListener { finish() }
    }

    private fun validateForm(): Boolean {
        var valid = true

        fun check(til: TextInputLayout, et: TextInputEditText, msg: String): Boolean {
            return if (et.text.toString().trim().isEmpty()) {
                til.error = msg; valid = false; false
            } else { til.error = null; true }
        }

        check(tilName,       etName,       "Nama tidak boleh kosong")
        check(tilNim,        etNim,        "NIM tidak boleh kosong")
        check(tilUniversity, etUniversity, "Universitas tidak boleh kosong")
        check(tilRole,       etRole,       "Role tidak boleh kosong")
        check(tilPhone,      etPhone,      "Nomor HP tidak boleh kosong")
        check(tilEmail,      etEmail,      "Email tidak boleh kosong")

        val email = etEmail.text.toString().trim()
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Format email tidak valid"; valid = false
        }

        val pass    = etPassword.text.toString()
        val confirm = etConfirm.text.toString()

        if (pass.isEmpty()) {
            tilPassword.error = "Password tidak boleh kosong"; valid = false
        } else if (pass.length < 6) {
            tilPassword.error = "Password minimal 6 karakter"; valid = false
        } else {
            tilPassword.error = null
        }

        if (confirm.isEmpty()) {
            tilConfirm.error = "Konfirmasi password tidak boleh kosong"; valid = false
        } else if (pass != confirm) {
            tilConfirm.error = "Password tidak cocok"; valid = false
        } else {
            tilConfirm.error = null
        }

        return valid
    }
}