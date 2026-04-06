package com.example.sdn4angsau.samosa

import kotlinx.coroutines.delay

interface TempatSampahRepository {
    suspend fun getDaftarTempatSampah(): List<TempatSampah>
}

class MockTempatSampahRepository : TempatSampahRepository {

    override suspend fun getDaftarTempatSampah(): List<TempatSampah> {
        // Mock delay supaya state loading bisa terlihat saat sumber data asli belum siap.
        delay(650)

        return listOf(
            TempatSampah("3", "Laboratorium", 100),
            TempatSampah("4", "Ruang Kantor", 95),
            TempatSampah("5", "Kantin SDN 4", 78),
            TempatSampah("1", "Perpustakaan", 45),
            TempatSampah("2", "Ruang Guru", 12)
        )
    }
}
