package com.example.sdn4angsau.samosa

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.sdn4angsau.samosa.databinding.ActivityTutorialBinding

class TutorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding.btnBackTutorial.setOnClickListener {
            finish()
        }

        binding.btnSelesaiTutorial.setOnClickListener {
            finish()
        }
    }
}
