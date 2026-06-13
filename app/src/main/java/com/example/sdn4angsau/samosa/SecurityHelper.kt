package com.example.sdn4angsau.samosa

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Helper untuk mengelola Confidentiality (EncryptedSharedPreferences)
 * dan Integrity (HMAC SHA-256) sesuai CIA Triad.
 */
object SecurityHelper {

    private const val PREFS_NAME = "samosa_secure_prefs"

    /**
     * Mendapatkan instance EncryptedSharedPreferences untuk kerahasiaan data (Confidentiality).
     */
    fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Memverifikasi integritas data menggunakan HMAC SHA-256 (Integrity).
     */
    fun verifyIntegrity(payload: String, receivedHmac: String, secretKey: String): Boolean {
        return try {
            val calculatedHmac = generateHmac(payload, secretKey)
            calculatedHmac == receivedHmac
        } catch (e: Exception) {
            false
        }
    }

    private fun generateHmac(data: String, key: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(key.toByteArray(), algorithm))
        return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
