package com.example.sdn4angsau.samosa

enum class TempatSampahStatus {
    AMAN,
    WASPADA,
    PENUH
}

data class TempatSampah(
    val binId: String,
    val lokasi: String,
    val persentase: Int,
    val isActive: Boolean = true
) {
    val status: TempatSampahStatus
        get() = when {
            persentase >= 90 -> TempatSampahStatus.PENUH
            persentase >= 60 -> TempatSampahStatus.WASPADA
            else -> TempatSampahStatus.AMAN
        }

    val isFull: Boolean
        get() = status == TempatSampahStatus.PENUH
}
