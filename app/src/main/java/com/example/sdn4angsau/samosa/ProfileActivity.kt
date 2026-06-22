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
            openEditProfile()
        }

        // Navigasi ke Edit Profil saat mengetuk tulisan "Edit"
        binding.tvEditProfile.setOnClickListener {
            openEditProfile()
        }

        binding.btnTutorialProfile.setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }

        binding.btnManageBinsProfile.setOnClickListener {
            startActivity(Intent(this, BinManagementActivity::class.java))
        }

        binding.btnDeviceSetupProfile.setOnClickListener {
            startActivity(Intent(this, DeviceSetupActivity::class.java))
        }

        binding.btnReportProfile.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        binding.btnLogoutProfile.setOnClickListener {
            // [FIX C-3] Bersihkan SEMUA storage sesi secara konsisten
            FirebaseAuth.getInstance().signOut()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(this, gso).signOut()

            // Bersihkan EncryptedSharedPreferences (konsisten dengan yang dibaca di MainActivity)
            SecurityHelper.getEncryptedPrefs(this).edit().clear().apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun openEditProfile() {
        val intent = Intent(this, EditProfileActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        displayUserProfile()
    }

    private fun displayUserProfile() {
        // [FIX M-4] Baca data profil dari EncryptedSharedPreferences (konsisten dengan EditProfileActivity)
        val sharedPref = SecurityHelper.getEncryptedPrefs(this)
        
        val name = sharedPref.getString("full_name", "Kepala Sekolah")
        val position = sharedPref.getString("position", "Kepala Sekolah")
        val employeeId = sharedPref.getString("employee_id", "STAF-ANGSAU-001")
        val institution = sharedPref.getString("institution", "UPTD SDN 4 Angsau")
        
        val photoUriString = sharedPref.getString("profile_photo_uri", null)

        binding.tvProfileName.text = name
        binding.tvProfilePosition.text = position
        binding.tvProfileId.text = "ID: $employeeId"
        binding.tvProfileInstitution.text = institution

        if (!photoUriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(photoUriString)
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            binding.ivProfilePhotoDisplay.setImageBitmap(bitmap)
                            binding.ivProfilePhotoDisplay.setPadding(0, 0, 0, 0)
                            binding.ivProfilePhotoDisplay.imageTintList = null
                        }
                    }
                } else {
                    binding.ivProfilePhotoDisplay.setImageURI(uri)
                    binding.ivProfilePhotoDisplay.setPadding(0, 0, 0, 0)
                    binding.ivProfilePhotoDisplay.imageTintList = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            binding.ivProfilePhotoDisplay.setImageResource(R.drawable.ic_profile)
            val p = (20 * resources.displayMetrics.density).toInt()
            binding.ivProfilePhotoDisplay.setPadding(p, p, p, p)
            binding.ivProfilePhotoDisplay.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#20B273"))
        }
    }
}
