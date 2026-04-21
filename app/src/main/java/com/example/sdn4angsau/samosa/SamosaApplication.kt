package com.example.sdn4angsau.samosa

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SamosaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
