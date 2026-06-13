package com.example.sdn4angsau.samosa

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity host sederhana untuk DeviceSetupFragment.
 */
class DeviceSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_setup)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DeviceSetupFragment())
                .commit()
        }
    }
}
