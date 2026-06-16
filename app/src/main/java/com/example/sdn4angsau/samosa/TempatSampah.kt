package com.example.sdn4angsau.samosa

import com.google.firebase.database.PropertyName

enum class TempatSampahStatus { AMAN, WASPADA, PENUH }

data class TempatSampah(
    var binId: String = "",
    var lokasi: String = "",
    var isActive: Boolean = true,

    @get:PropertyName("persentase") @set:PropertyName("persentase")
    var persentase: Int = 0,

    @get:PropertyName("notifThreshold") @set:PropertyName("notifThreshold")
    var notifThreshold: Int = 80,

    var jarakSensor: Int = 0
) {
    val status: TempatSampahStatus
        get() = when {
            persentase >= 90 -> TempatSampahStatus.PENUH
            persentase >= 70 -> TempatSampahStatus.WASPADA
            else -> TempatSampahStatus.AMAN
        }

    val isFull: Boolean get() = status == TempatSampahStatus.PENUH
}