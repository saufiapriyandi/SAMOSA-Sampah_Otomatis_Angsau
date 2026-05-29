package com.example.sdn4angsau.samosa

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.example.sdn4angsau.samosa.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (12 * resources.displayMetrics.density).toInt()
            val extraBottomPadding = (24 * resources.displayMetrics.density).toInt()

            binding.headerProfileBar.updatePadding(top = systemBars.top + extraTopPadding)
            binding.scrollProfile.updatePadding(bottom = systemBars.bottom + extraBottomPadding)

            insets
        }

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding.btnBackProfile.setOnClickListener {
            finish()
        }

        // Navigasi ke Edit Profil saat mengetuk kartu profil
        binding.cardProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnTutorialProfile.setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }

        binding.btnManageBinsProfile.setOnClickListener {
            startActivity(Intent(this, BinManagementActivity::class.java))
        }

        binding.btnReportProfile.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        binding.btnLogoutProfile.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(this, gso).signOut()

            val sharedPref = getSharedPreferences("SesiSamosa", MODE_PRIVATE)
            sharedPref.edit {
                putBoolean("SUDAH_LOGIN", false)
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // PENTING: Refresh data otomatis setiap kali kembali ke halaman ini
        displayUserProfile()
    }

    private fun displayUserProfile() {
        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        
        // 1. Muat data teks dari SharedPreferences
        val name = sharedPref.getString("full_name", "Kepala Sekolah")
        val position = sharedPref.getString("position", "Kepala Sekolah")
        val employeeId = sharedPref.getString("employee_id", "STAF-ANGSAU-001")
        val institution = sharedPref.getString("institution", "UPTD SDN 4 Angsau")
        
        binding.tvProfileName.text = name
        binding.tvProfilePosition.text = position
        binding.tvProfileId.text = "ID: $employeeId"
        binding.tvProfileInstitution.text = institution

        // 2. Muat foto profil langsung dari file fisik di Internal Storage
        val file = File(filesDir, "profil_user.jpg")
        if (file.exists()) {
            try {
                // Baca file fisik menjadi bitmap
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.ivProfilePhotoDisplay.setImageBitmap(bitmap)
                    
                    // Hilangkan padding ikon default dan filter warna agar foto asli terlihat jelas
                    binding.ivProfilePhotoDisplay.setPadding(0, 0, 0, 0)
                    binding.ivProfilePhotoDisplay.imageTintList = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Tampilan default jika file belum ada
            binding.ivProfilePhotoDisplay.setImageResource(R.drawable.ic_profile)
            val p = (20 * resources.displayMetrics.density).toInt()
            binding.ivProfilePhotoDisplay.setPadding(p, p, p, p)
            binding.ivProfilePhotoDisplay.imageTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#20B273")
            )
        }
    }
}
