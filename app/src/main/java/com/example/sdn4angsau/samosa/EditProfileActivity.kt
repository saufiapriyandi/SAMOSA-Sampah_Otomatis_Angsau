package com.example.sdn4angsau.samosa

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sdn4angsau.samosa.databinding.ActivityEditProfileBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var currentPhotoUri: Uri? = null
    private var tempImageUri: Uri? = null

    // Launcher for Gallery
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            currentPhotoUri = uri
            binding.ivEditProfilePhoto.setImageURI(uri)
            binding.ivEditProfilePhoto.setPadding(0, 0, 0, 0)
            binding.ivEditProfilePhoto.imageTintList = null
        }
    }

    // Launcher for Camera
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { uri ->
                currentPhotoUri = uri
                binding.ivEditProfilePhoto.setImageURI(uri)
                binding.ivEditProfilePhoto.setPadding(0, 0, 0, 0)
                binding.ivEditProfilePhoto.imageTintList = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        loadUserData()

        binding.btnBackEditProfile.setOnClickListener {
            finish()
        }

        binding.btnChangePhoto.setOnClickListener {
            showImageSourceDialog()
        }

        binding.btnUpdateProfile.setOnClickListener {
            saveUserData()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.edit_profile_camera),
            getString(R.string.edit_profile_gallery)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_profile_choose_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoFile?.also { file ->
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            tempImageUri = uri
            takePicture.launch(uri)
        }
    }

    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        binding.etFullName.setText(sharedPref.getString("full_name", "Kepala Sekolah"))
        binding.etEmployeeId.setText(sharedPref.getString("employee_id", "STAF-ANGSAU-001"))
        binding.etPosition.setText(sharedPref.getString("position", "Kepala Sekolah"))
        binding.etInstitution.setText(sharedPref.getString("institution", "UPTD SDN 4 Angsau"))

        val photoUriString = sharedPref.getString("profile_photo_uri", null)
        if (photoUriString != null) {
            val uri = Uri.parse(photoUriString)
            binding.ivEditProfilePhoto.setImageURI(uri)
            binding.ivEditProfilePhoto.setPadding(0, 0, 0, 0)
            binding.ivEditProfilePhoto.imageTintList = null
            currentPhotoUri = uri
        }
    }

    private fun saveUserData() {
        val name = binding.etFullName.text.toString().trim()
        val id = binding.etEmployeeId.text.toString().trim()
        val position = binding.etPosition.text.toString().trim()
        val institution = binding.etInstitution.text.toString().trim()

        if (name.isEmpty() || id.isEmpty() || position.isEmpty() || institution.isEmpty()) {
            Toast.makeText(this, getString(R.string.edit_profile_error_empty), Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        
        // Fix for Argument type mismatch by using ?.let
        val finalPhotoUri = currentPhotoUri?.let { saveImageToInternalStorage(it) }

        with(sharedPref.edit()) {
            putString("full_name", name)
            putString("employee_id", id)
            putString("position", position)
            putString("institution", institution)
            if (finalPhotoUri != null) {
                putString("profile_photo_uri", finalPhotoUri.toString())
            }
            apply()
        }

        Toast.makeText(this, getString(R.string.edit_profile_success), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        // If it's already an internal file from our app, no need to copy
        if (uri.scheme == "file" && uri.path?.contains(filesDir.path) == true) return uri

        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "profile_photo.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
