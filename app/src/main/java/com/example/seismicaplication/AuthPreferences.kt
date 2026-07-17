package com.example.seismicaplication

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * AuthPreferences — multi-account support.
 *
 * Setiap akun disimpan dengan key "account_{email}" berisi JSON:
 * {
 *   "email": "...",
 *   "password": "...",
 *   "name": "...",
 *   "nim": "...",
 *   "university": "...",
 *   "role": "...",
 *   "phone": "..."
 * }
 *
 * "current_email" menyimpan email akun yang sedang aktif (sesi terakhir login).
 */
object AuthPreferences {

    private const val PREFS_NAME      = "auth_prefs"
    private const val KEY_CURRENT     = "current_email"  // email akun yang sedang login

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Simpan & baca data akun ───────────────────────────────────────────────

    private fun accountKey(email: String) = "account_${email.trim().lowercase()}"

    private fun saveAccount(context: Context, data: JSONObject) {
        val email = data.getString("email").trim().lowercase()
        prefs(context).edit()
            .putString(accountKey(email), data.toString())
            .apply()
    }

    private fun loadAccount(context: Context, email: String): JSONObject? {
        val raw = prefs(context).getString(accountKey(email.trim().lowercase()), null)
            ?: return null
        return try { JSONObject(raw) } catch (_: Exception) { null }
    }

    // ── Register ──────────────────────────────────────────────────────────────

    fun register(
        context: Context,
        name: String,
        email: String,
        password: String,
        nim: String,
        university: String,
        role: String,
        phone: String
    ) {
        val data = JSONObject().apply {
            put("email",      email.trim().lowercase())
            put("password",   password)
            put("name",       name)
            put("nim",        nim)
            put("university", university)
            put("role",       role)
            put("phone",      phone)
        }
        saveAccount(context, data)
    }

    // ── Validasi login ────────────────────────────────────────────────────────

    fun isEmailRegistered(context: Context, email: String): Boolean =
        loadAccount(context, email) != null

    fun validateLogin(context: Context, email: String, password: String): Boolean {
        val account = loadAccount(context, email) ?: return false
        return account.getString("password") == password
    }

    // ── Session akun aktif ────────────────────────────────────────────────────

    /** Simpan email akun yang sedang login (untuk ProfileActivity baca data) */
    fun setCurrentEmail(context: Context, email: String) {
        prefs(context).edit()
            .putString(KEY_CURRENT, email.trim().lowercase())
            .apply()
    }

    fun getCurrentEmail(context: Context): String =
        prefs(context).getString(KEY_CURRENT, "") ?: ""

    fun clearCurrentEmail(context: Context) {
        prefs(context).edit().remove(KEY_CURRENT).apply()
    }

    // ── Update profile (tidak ubah password & email) ──────────────────────────

    fun updateProfile(
        context: Context,
        name: String,
        nim: String,
        university: String,
        role: String,
        phone: String
    ) {
        val email   = getCurrentEmail(context)
        val account = loadAccount(context, email) ?: return

        account.apply {
            put("name",       name)
            put("nim",        nim)
            put("university", university)
            put("role",       role)
            put("phone",      phone)
        }
        saveAccount(context, account)

        // Sync ke profile_prefs agar ProfileActivity bisa baca langsung
        syncToProfilePrefs(context, account)
    }

    /** Sync data akun aktif ke profile_prefs (dipakai ProfileActivity) */
    fun syncToProfilePrefs(context: Context, account: JSONObject? = null) {
        val data = account ?: loadAccount(context, getCurrentEmail(context)) ?: return
        context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
            .edit().apply {
                putString("name",       data.optString("name"))
                putString("email",      data.optString("email"))
                putString("nim",        data.optString("nim"))
                putString("university", data.optString("university"))
                putString("role",       data.optString("role"))
                putString("phone",      data.optString("phone"))
                apply()
            }
    }

    // ── Getters untuk akun aktif ──────────────────────────────────────────────

    private fun get(context: Context, key: String): String {
        val email   = getCurrentEmail(context)
        val account = loadAccount(context, email) ?: return ""
        return account.optString(key, "")
    }

    fun getName(context: Context)       = get(context, "name")
    fun getEmail(context: Context)      = get(context, "email")
    fun getNim(context: Context)        = get(context, "nim")
    fun getUniversity(context: Context) = get(context, "university")
    fun getRole(context: Context)       = get(context, "role")
    fun getPhone(context: Context)      = get(context, "phone")
}