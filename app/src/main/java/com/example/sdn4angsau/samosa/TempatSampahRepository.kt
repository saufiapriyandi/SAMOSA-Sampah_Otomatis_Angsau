package com.example.sdn4angsau.samosa

import android.content.Context
import kotlinx.coroutines.delay

interface TempatSampahRepository {
    suspend fun getDaftarTempatSampah(): List<TempatSampah>
}

class MockTempatSampahRepository(
    private val context: Context
) : TempatSampahRepository {

    override suspend fun getDaftarTempatSampah(): List<TempatSampah> {
        // Mock delay supaya state loading bisa terlihat saat sumber data asli belum siap.
        delay(650)
        return TempatSampahLocalStore.getActive(context)
    }
}
