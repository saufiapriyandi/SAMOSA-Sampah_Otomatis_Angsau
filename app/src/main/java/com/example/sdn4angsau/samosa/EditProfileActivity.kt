package com.example.sdn4angsau.samosa

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.sdn4angsau.samosa.databinding.ActivityEditProfileBinding
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var selectedImageUri: Uri? = null

    // 2. Logika Akses Foto Langsung ke Galeri (Tanpa Dialog Kamera)
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivEditProfilePhoto.setImageURI(it)
            binding.ivEditProfilePhoto.setPadding(0, 0, 0, 0)
            binding.ivEditProfilePhoto.imageTintList = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FIX BUG UI: Header terlalu mepet ke atas
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (12 * resources.displayMetrics.density).toInt()
            binding.headerEditProfile.updatePadding(top = systemBars.top + extraTopPadding)
            insets
        }

        // 4. Logika Menampilkan Data (Load Data)
        loadUserData()

        // 1. Logika Tombol Kembali (Panah Back)
        binding.btnBackEditProfile.setOnClickListener {
            finish()
        }

        // 2. Klik foto atau tombol kamera langsung buka galeri
        val openGallery = { getContent.launch("image/*") }
        binding.ivEditProfilePhoto.setOnClickListener { openGallery() }
        binding.btnChangePhoto.setOnClickListener { openGallery() }

        // 3. Logika Penyimpanan Lokal
        binding.btnUpdateProfile.setOnClickListener {
            saveUserData()
        }
    }

    private fun loadUserData() {
        // [FIX M-4] Gunakan EncryptedSharedPreferences untuk melindungi data PII pengguna
        val sharedPref = SecurityHelper.getEncryptedPrefs(this)
        binding.etFullName.setText(sharedPref.getString("full_name", ""))
        binding.etEmployeeId.setText(sharedPref.getString("employee_id", ""))
        binding.etPosition.setText(sharedPref.getString("position", ""))
        binding.etInstitution.setText(sharedPref.getString("institution", ""))

        // Load foto dari Internal Storage jika ada
        val file = File(filesDir, "profil_user.jpg")
        if (file.exists()) {
            binding.ivEditProfilePhoto.setImageURI(Uri.fromFile(file))
            binding.ivEditProfilePhoto.setPadding(0, 0, 0, 0)
            binding.ivEditProfilePhoto.imageTintList = null
        }
    }

    private fun saveUserData() {
        val name = binding.etFullName.text.toString().trim()
        val id = binding.etEmployeeId.text.toString().trim()
        val position = binding.etPosition.text.toString().trim()
        val institution = binding.etInstitution.text.toString().trim()

        if (name.isEmpty() || institution.isEmpty()) {
            Toast.makeText(this, "Nama Lengkap dan Instansi wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // [FIX M-4] Simpan data teks ke EncryptedSharedPreferences (bukan plain SharedPreferences)
        val sharedPref = SecurityHelper.getEncryptedPrefs(this)
        val editor = sharedPref.edit()
        editor.putString("full_name", name)
        editor.putString("employee_id", id)
        editor.putString("position", position)
        editor.putString("institution", institution)

        // 3. FIX BUG LOGIKA: Simpan File Foto dan UPDATE profile_photo_uri agar tersinkronisasi
        selectedImageUri?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    val file = File(filesDir, "profil_user.jpg")
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                    // Simpan path URI ke EncryptedSharedPreferences untuk dibaca ProfileActivity
                    editor.putString("profile_photo_uri", Uri.fromFile(file).toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        editor.apply()

        Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
        finish()
    }
}
