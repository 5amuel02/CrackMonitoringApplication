package com.example.seismicaplication

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

// ✅ extend BaseActivity
class ProfileActivity : BaseActivity() {

    private lateinit var ivProfilePhoto: ImageView
    private lateinit var btnEditPhoto: ImageButton
    private lateinit var tvName: TextView
    private lateinit var tvRole: TextView

    private lateinit var layoutView: LinearLayout
    private lateinit var tvViewName: TextView
    private lateinit var tvViewNim: TextView
    private lateinit var tvViewUniversity: TextView
    private lateinit var tvViewRole: TextView
    private lateinit var tvViewEmail: TextView
    private lateinit var tvViewPhone: TextView
    private lateinit var btnEdit: MaterialButton

    private lateinit var layoutEdit: LinearLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etNim: TextInputEditText
    private lateinit var etUniversity: TextInputEditText
    private lateinit var etRole: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private lateinit var prefs: SharedPreferences
    private var photoUriString: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                photoUriString = it.toString()
                ivProfilePhoto.setImageURI(it)
                prefs.edit().putString("photo_uri", photoUriString).apply()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)

        initViews()
        loadProfile()
        setupBottomNavigation()

        // ✅ Apply theme setelah layout siap
        applyCurrentTheme()
    }

    private fun initViews() {
        ivProfilePhoto = findViewById(R.id.iv_profile_photo)
        btnEditPhoto   = findViewById(R.id.btn_edit_photo)
        tvName         = findViewById(R.id.tv_profile_name)
        tvRole         = findViewById(R.id.tv_profile_role)

        layoutView       = findViewById(R.id.layout_view_mode)
        tvViewName       = findViewById(R.id.tv_view_name)
        tvViewNim        = findViewById(R.id.tv_view_nim)
        tvViewUniversity = findViewById(R.id.tv_view_university)
        tvViewRole       = findViewById(R.id.tv_view_role)
        tvViewEmail      = findViewById(R.id.tv_view_email)
        tvViewPhone      = findViewById(R.id.tv_view_phone)
        btnEdit          = findViewById(R.id.btn_edit)

        layoutEdit   = findViewById(R.id.layout_edit_mode)
        etName       = findViewById(R.id.et_name)
        etNim        = findViewById(R.id.et_nim)
        etUniversity = findViewById(R.id.et_university)
        etRole       = findViewById(R.id.et_role)
        etEmail      = findViewById(R.id.et_email)
        etPhone      = findViewById(R.id.et_phone)
        btnSave      = findViewById(R.id.btn_save)
        btnCancel    = findViewById(R.id.btn_cancel)

        btnEditPhoto.setOnClickListener { openGallery() }
        ivProfilePhoto.setOnClickListener { openGallery() }

        btnEdit.setOnClickListener {
            fillEditForm()
            layoutView.visibility = View.GONE
            layoutEdit.visibility = View.VISIBLE
        }

        btnSave.setOnClickListener {
            saveProfile()
            layoutEdit.visibility = View.GONE
            layoutView.visibility = View.VISIBLE
        }

        btnCancel.setOnClickListener {
            layoutEdit.visibility = View.GONE
            layoutView.visibility = View.VISIBLE
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        pickImageLauncher.launch(intent)
    }

    private fun loadProfile() {
        val name       = prefs.getString("name", "") ?: ""
        val nim        = prefs.getString("nim", "") ?: ""
        val university = prefs.getString("university", "") ?: ""
        val role       = prefs.getString("role", "") ?: ""
        val email      = prefs.getString("email", "") ?: ""
        val phone      = prefs.getString("phone", "") ?: ""
        photoUriString = prefs.getString("photo_uri", null)

        photoUriString?.let {
            try { ivProfilePhoto.setImageURI(Uri.parse(it)) } catch (_: Exception) {}
        }

        tvName.text           = if (name.isNotEmpty()) name else "Nama Operator"
        tvRole.text           = if (role.isNotEmpty()) role else "Role"
        tvViewName.text       = if (name.isNotEmpty()) name else "-"
        tvViewNim.text        = if (nim.isNotEmpty()) nim else "-"
        tvViewUniversity.text = if (university.isNotEmpty()) university else "-"
        tvViewRole.text       = if (role.isNotEmpty()) role else "-"
        tvViewEmail.text      = if (email.isNotEmpty()) email else "-"
        tvViewPhone.text      = if (phone.isNotEmpty()) phone else "-"
    }

    private fun fillEditForm() {
        etName.setText(prefs.getString("name", ""))
        etNim.setText(prefs.getString("nim", ""))
        etUniversity.setText(prefs.getString("university", ""))
        etRole.setText(prefs.getString("role", ""))
        etEmail.setText(prefs.getString("email", ""))
        etPhone.setText(prefs.getString("phone", ""))
    }

    private fun saveProfile() {
        val name       = etName.text.toString().trim()
        val nim        = etNim.text.toString().trim()
        val university = etUniversity.text.toString().trim()
        val role       = etRole.text.toString().trim()
        val email      = etEmail.text.toString().trim()
        val phone      = etPhone.text.toString().trim()

        prefs.edit().apply {
            putString("name", name)
            putString("nim", nim)
            putString("university", university)
            putString("role", role)
            putString("email", email)
            putString("phone", phone)
            apply()
        }

        // ✅ Gunakan updateProfile — tidak menyentuh password & email
        AuthPreferences.updateProfile(
            context    = this,
            name       = name,
            nim        = nim,
            university = university,
            role       = role,
            phone      = phone
        )

        tvName.text           = if (name.isNotEmpty()) name else "Nama Operator"
        tvRole.text           = if (role.isNotEmpty()) role else "Role"
        tvViewName.text       = if (name.isNotEmpty()) name else "-"
        tvViewNim.text        = if (nim.isNotEmpty()) nim else "-"
        tvViewUniversity.text = if (university.isNotEmpty()) university else "-"
        tvViewRole.text       = if (role.isNotEmpty()) role else "-"
        tvViewEmail.text      = if (email.isNotEmpty()) email else "-"
        tvViewPhone.text      = if (phone.isNotEmpty()) phone else "-"

        Toast.makeText(this, "Profil berhasil disimpan", Toast.LENGTH_SHORT).show()
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private fun doLogout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Yakin ingin logout?")
            .setPositiveButton("Ya") { _, _ ->
                // ✅ Clear sesi akun yang aktif
                AuthPreferences.clearCurrentEmail(this)
                MascotService.stop(this)
                startActivity(
                    Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupBottomNavigation() {
        // ✅ Tombol logout — tambahkan di activity_profile.xml dengan id btn_logout
        findViewById<com.google.android.material.button.MaterialButton?>(R.id.btn_logout)
            ?.setOnClickListener { doLogout() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
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
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0); finish(); true
                }
                else -> false
            }
        }
    }
}