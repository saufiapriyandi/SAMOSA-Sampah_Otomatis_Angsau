package com.example.sdn4angsau.samosa

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowInsetsControllerCompat
import com.example.sdn4angsau.samosa.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding.btnBackProfile.setOnClickListener {
            finish()
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
}
