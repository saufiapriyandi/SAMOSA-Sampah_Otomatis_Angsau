package com.example.sdn4angsau.samosa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.sdn4angsau.samosa.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // MEMAKSA IKON JAM & BATERAI MENJADI PUTIH TERANG
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding.btnBackProfile.setOnClickListener {
            finish()
        }

        // FUNGSI TOMBOL LOGOUT
        binding.btnLogoutProfile.setOnClickListener {
            val sharedPref = getSharedPreferences("SesiSamosa", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("SUDAH_LOGIN", false)
            editor.apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}