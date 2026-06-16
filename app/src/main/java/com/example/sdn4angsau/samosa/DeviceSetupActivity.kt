package com.example.sdn4angsau.samosa

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class DeviceSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Memastikan mode edge-to-edge diaktifkan
        enableEdgeToEdge()
        setContentView(R.layout.activity_device_setup)

        // Mengatur warna Status Bar dan Nav Bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.green_primary)

        // Memastikan ikon di status bar berwarna putih
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Padding dinamis agar konten tidak tertutup status bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DeviceSetupFragment())
                .commit()
        }
    }
}
