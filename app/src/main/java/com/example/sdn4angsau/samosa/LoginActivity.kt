package com.example.sdn4angsau.samosa

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sdn4angsau.samosa.databinding.ActivityLoginBinding
import java.security.MessageDigest

/**
 * LoginActivity — Halaman login offline berbasis hash SHA-256.
 *
 * Fitur keamanan:
 *  • Kredensial default disimpan sebagai konstanta hash (bukan plaintext).
 *  • Verifikasi dilakukan dengan membandingkan SHA-256(input) vs hash tersimpan.
 *  • Fitur lihat/sembunyikan password via ikon mata (ivTogglePassword).
 *  • Anti-brute force: setelah 5× gagal → lockout 60 detik, countdown di teks tombol.
 *  • State lockout persisten via SharedPreferences (bertahan meskipun app di-kill).
 *
 * Komponen layout yang digunakan (hanya 4 ID):
 *  - etUsername          : EditText untuk input email
 *  - etPassword          : EditText untuk input password
 *  - ivTogglePassword    : ImageView ikon mata untuk toggle visibility
 *  - btnLogin            : Button untuk proses masuk
 */
class LoginActivity : AppCompatActivity() {

    // ──────────────────────────────────────────────
    //  Konstanta Kredensial (SHA-256 hash, BUKAN plaintext)
    // ──────────────────────────────────────────────
    companion object {
        /** Username default (email) — disimpan sebagai konstanta */
        private const val DEFAULT_USERNAME = "samosaofficiall@gmail.com"

        /**
         * SHA-256 hash dari password yang benar.
         * Dihitung: SHA-256("SamosaSejak2026") → hex string di bawah ini.
         * Password asli TIDAK pernah ditulis dalam kode.
         */
        private const val DEFAULT_PASSWORD_HASH =
            "534b17937d174d8de1cf2ebf3a38ce3e7ce0a44d0144f83b4c10abceb819f6a7"

        /** Batas percobaan gagal sebelum lockout */
        private const val MAX_FAILED_ATTEMPTS = 5

        /** Durasi lockout dalam milidetik (60 detik = 1 menit) */
        private const val LOCKOUT_DURATION_MS = 60_000L

        /** Teks default tombol login */
        private const val BUTTON_TEXT_DEFAULT = "Masuk"

        // SharedPreferences keys
        private const val PREFS_NAME = "samosa_login_prefs"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIME = "lockout_time"
    }

    // ──────────────────────────────────────────────
    //  Properties
    // ──────────────────────────────────────────────
    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginPrefs: SharedPreferences
    private var countDownTimer: CountDownTimer? = null

    /** Status visibilitas password (true = terlihat, false = tersembunyi) */
    private var isPasswordVisible = false

    // ──────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupPasswordToggle()
        setupLoginButton()

        // Cek apakah sedang dalam masa lockout (state persisten)
        checkLockoutState()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    // ──────────────────────────────────────────────
    //  Setup UI Listeners
    // ──────────────────────────────────────────────

    /**
     * Toggle lihat/sembunyikan password.
     * Klik ivTogglePassword → ubah TransformationMethod pada etPassword.
     * Kursor selalu dijaga tetap di akhir teks setelah transformasi berubah.
     */
    private fun setupPasswordToggle() {
        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // Tampilkan password (teks terlihat)
                binding.etPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                // Ganti ikon ke "mata terbuka"
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                // Sembunyikan password (teks tersensor ●●●●)
                binding.etPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                // Ganti ikon ke "mata tertutup" (dicoret)
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }

            // Pastikan kursor tetap di akhir teks
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    /**
     * Setup tombol login.
     */
    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            onLoginClicked()
        }
    }

    // ──────────────────────────────────────────────
    //  Logika Autentikasi
    // ──────────────────────────────────────────────

    private fun onLoginClicked() {
        val inputEmail = binding.etUsername.text.toString().trim()
        val inputPassword = binding.etPassword.text.toString()

        // Validasi input kosong
        if (inputEmail.isEmpty() || inputPassword.isEmpty()) {
            Toast.makeText(this, "Email dan Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        // Hash password yang diinputkan pengguna menggunakan SHA-256
        val inputPasswordHash = hashSHA256(inputPassword)

        // Bandingkan email (lowercase) + hash password dengan konstanta
        if (inputEmail.lowercase() == DEFAULT_USERNAME && inputPasswordHash == DEFAULT_PASSWORD_HASH) {
            // ✅ Login berhasil
            onLoginSuccess()
        } else {
            // ❌ Login gagal
            onLoginFailed()
        }
    }

    /**
     * Login berhasil:
     *  1. Reset failed_attempts ke 0
     *  2. Tampilkan Toast konfirmasi
     *  3. Navigasi ke halaman Dashboard utama
     */
    private fun onLoginSuccess() {
        // Reset semua state lockout/gagal
        loginPrefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_TIME, 0L)
            .apply()

        Toast.makeText(this, "Login Berhasil", Toast.LENGTH_SHORT).show()

        // ──────────────────────────────────────────
        // Navigasi ke halaman Dashboard utama.
        // Ganti DashboardActivity::class.java dengan Activity tujuan Anda
        // jika nama class-nya berbeda.
        // ──────────────────────────────────────────
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Login gagal:
     *  1. Tambahkan failed_attempts + 1
     *  2. Jika mencapai MAX_FAILED_ATTEMPTS → aktifkan lockout
     *  3. Jika belum → tampilkan sisa percobaan via Toast
     */
    private fun onLoginFailed() {
        val currentAttempts = loginPrefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        loginPrefs.edit().putInt(KEY_FAILED_ATTEMPTS, currentAttempts).apply()

        if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
            // Aktifkan lockout: catat waktu berakhir lockout
            val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            loginPrefs.edit().putLong(KEY_LOCKOUT_TIME, lockoutUntil).apply()

            Toast.makeText(
                this,
                "Terlalu banyak percobaan gagal. Akses ditangguhkan 60 detik.",
                Toast.LENGTH_LONG
            ).show()

            startLockoutCountdown(LOCKOUT_DURATION_MS)
        } else {
            val remaining = MAX_FAILED_ATTEMPTS - currentAttempts
            Toast.makeText(
                this,
                "Login gagal. Sisa percobaan: $remaining",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ──────────────────────────────────────────────
    //  Lockout / Anti-Brute Force
    // ──────────────────────────────────────────────

    /**
     * Cek apakah saat ini masih dalam masa lockout.
     * State bersifat persisten di SharedPreferences sehingga bertahan
     * meskipun user menutup paksa (force close) aplikasi.
     */
    private fun checkLockoutState() {
        val lockoutTime = loginPrefs.getLong(KEY_LOCKOUT_TIME, 0L)
        val now = System.currentTimeMillis()

        if (lockoutTime > now) {
            // Masih dalam masa lockout — lanjutkan countdown sisa waktu
            val remainingMs = lockoutTime - now
            startLockoutCountdown(remainingMs)
        } else if (lockoutTime > 0L) {
            // Lockout sudah habis tapi belum di-reset — reset sekarang
            resetLockout()
        }
    }

    /**
     * Mulai CountDownTimer untuk lockout.
     * Selama lockout:
     *  - btnLogin.isEnabled = false
     *  - Teks tombol berubah menjadi "Tunggu X detik"
     * Setelah selesai:
     *  - Kembalikan teks tombol ke "Masuk"
     *  - btnLogin.isEnabled = true
     *  - Reset failed_attempts ke 0
     */
    private fun startLockoutCountdown(durationMs: Long) {
        // Nonaktifkan tombol login
        binding.btnLogin.isEnabled = false

        // Batalkan timer sebelumnya jika ada
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) + 1
                binding.btnLogin.text = "Tunggu $seconds detik"
            }

            override fun onFinish() {
                resetLockout()
            }
        }.start()
    }

    /**
     * Reset semua state lockout:
     *  - failed_attempts → 0
     *  - lockout_time → 0
     *  - Aktifkan kembali tombol login
     *  - Kembalikan teks tombol ke default ("Masuk")
     */
    private fun resetLockout() {
        loginPrefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_TIME, 0L)
            .apply()

        countDownTimer?.cancel()

        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = BUTTON_TEXT_DEFAULT
    }

    // ──────────────────────────────────────────────
    //  Utilitas Hash SHA-256
    // ──────────────────────────────────────────────

    /**
     * Menghitung SHA-256 hash dari string input.
     *
     * @param input teks yang akan di-hash (misalnya password dari EditText)
     * @return hex string lowercase 64 karakter dari digest SHA-256
     */
    private fun hashSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
