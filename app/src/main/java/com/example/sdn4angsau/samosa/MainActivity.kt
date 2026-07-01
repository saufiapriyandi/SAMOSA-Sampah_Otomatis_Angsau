package com.example.sdn4angsau.samosa

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sdn4angsau.samosa.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // ┌─────────────────────────────────────────────┐
    // │  [TAMBAHAN] Properti untuk Brute Force       │
    // │             & Toggle Password                │
    // └─────────────────────────────────────────────┘
    private lateinit var loginPrefs: SharedPreferences
    private var countDownTimer: CountDownTimer? = null
    private var isPasswordVisible = false

    companion object {
        // SharedPreferences
        private const val PREFS_NAME = "samosa_login_prefs"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIME = "lockout_time"

        // Brute Force Config
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 60_000L // 1 menit

        // Teks default tombol
        private const val BUTTON_TEXT_DEFAULT = "Masuk"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Cek sesi menggunakan FirebaseAuth
        if (auth.currentUser != null) {
            pindahKeDashboard()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ════════════════════════════════════════════════
        //  [TAMBAHAN BARU] PERMINTAAN IZIN NOTIFIKASI
        // ════════════════════════════════════════════════
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
        // ════════════════════════════════════════════════

        // [TAMBAHAN] Inisialisasi SharedPreferences untuk brute force
        loginPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupGoogleSignIn()
        setupLoginListeners()

        // [TAMBAHAN] Setup toggle password & cek lockout
        setupPasswordToggle()
        checkLockoutState()
    }

    override fun onDestroy() {
        super.onDestroy()
        // [TAMBAHAN] Batalkan timer jika Activity dihancurkan
        countDownTimer?.cancel()
    }

    // ════════════════════════════════════════════════
    //  GOOGLE SIGN-IN (TIDAK DIUBAH — KODE ASLI)
    // ════════════════════════════════════════════════

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private val launcherGoogleSignIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let(::hubungkanKeFirebaseDenganGoogle)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In Gagal", Toast.LENGTH_LONG).show()
        }
    }

    private fun hubungkanKeFirebaseDenganGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Google Sign-In Berhasil", Toast.LENGTH_SHORT).show()
                    pindahKeDashboard()
                } else {
                    Toast.makeText(this, "Firebase Gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ════════════════════════════════════════════════
    //  LOGIN EMAIL/PASSWORD + BRUTE FORCE (DIMODIFIKASI)
    // ════════════════════════════════════════════════

    private fun setupLoginListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Pengecekan Firebase secara murni untuk semua akun
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            onLoginSuccess()
                        } else {
                            onLoginFailed(task.exception?.message)
                        }
                    }
            } else {
                Toast.makeText(this, "Email dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Google Sign-In (TIDAK DIUBAH) ──
        binding.btnGoogleLogin.setOnClickListener {
            launcherGoogleSignIn.launch(googleSignInClient.signInIntent)
        }
    }

    // ════════════════════════════════════════════════
    //  [TAMBAHAN] TOGGLE LIHAT/SEMBUNYIKAN PASSWORD
    // ════════════════════════════════════════════════

    /**
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
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                // Sembunyikan password (teks tersensor ●●●●)
                binding.etPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }

            // Pastikan kursor tetap di akhir teks
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    // ════════════════════════════════════════════════
    //  [TAMBAHAN] PROTEKSI BRUTE FORCE (5× → LOCKOUT)
    // ════════════════════════════════════════════════

    private fun onLoginSuccess() {
        // ✅ Reset failed_attempts saat berhasil
        loginPrefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_TIME, 0L)
            .apply()

        Toast.makeText(this, "Login Berhasil", Toast.LENGTH_SHORT).show()
        pindahKeDashboard()
    }

    /**
     * Dipanggil saat Login GAGAL (Offline maupun Firebase).
     * Tambahkan failed_attempts + 1, jika mencapai 5 → lockout 60 detik.
     */
    private fun onLoginFailed(errorMessage: String?) {
        val currentAttempts = loginPrefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        loginPrefs.edit().putInt(KEY_FAILED_ATTEMPTS, currentAttempts).apply()

        if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
            // Catat waktu lockout berakhir
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

    /**
     * Cek apakah saat ini masih dalam masa lockout.
     * Dipanggil di onCreate — state bertahan meskipun app di-force close.
     */
    private fun checkLockoutState() {
        val lockoutTime = loginPrefs.getLong(KEY_LOCKOUT_TIME, 0L)
        val now = System.currentTimeMillis()

        if (lockoutTime > now) {
            // Masih dalam masa lockout → lanjutkan countdown sisa
            startLockoutCountdown(lockoutTime - now)
        } else if (lockoutTime > 0L) {
            // Lockout sudah habis tapi belum di-reset
            resetLockout()
        }
    }

    /**
     * Jalankan CountDownTimer:
     * - btnLogin.isEnabled = false
     * - Teks tombol menjadi "Tunggu X detik"
     * - Setelah selesai → reset dan aktifkan kembali
     */
    private fun startLockoutCountdown(durationMs: Long) {
        binding.btnLogin.isEnabled = false

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
     * - failed_attempts → 0, lockout_time → 0
     * - Aktifkan kembali btnLogin, kembalikan teks ke "Masuk"
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

    // ════════════════════════════════════════════════
    //  NAVIGASI (TIDAK DIUBAH — KODE ASLI)
    // ════════════════════════════════════════════════

    private fun pindahKeDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}

