package com.example.sdn4angsau.samosa

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.example.sdn4angsau.samosa.databinding.ActivityTutorialBinding

class TutorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (12 * resources.displayMetrics.density).toInt()
            val extraBottomPadding = (24 * resources.displayMetrics.density).toInt()

            binding.headerTutorialBar.updatePadding(top = systemBars.top + extraTopPadding)
            binding.scrollTutorial.updatePadding(bottom = systemBars.bottom + extraBottomPadding)

            insets
        }

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding.btnBackTutorial.setOnClickListener {
            finish()
        }

        binding.btnSelesaiTutorial.setOnClickListener {
            finish()
        }
    }
}
