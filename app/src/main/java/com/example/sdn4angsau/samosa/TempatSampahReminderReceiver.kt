package com.example.sdn4angsau.samosa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TempatSampahReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val lokasi = intent.getStringExtra(TempatSampahNotificationHelper.EXTRA_LOKASI) ?: return
        val binId = intent.getStringExtra(TempatSampahNotificationHelper.EXTRA_BIN_ID) ?: return
        val persentase = intent.getIntExtra(TempatSampahNotificationHelper.EXTRA_PERSENTASE, 0)

        TempatSampahNotificationHelper.showReminderNotification(
            context = context,
            lokasi = lokasi,
            binId = binId,
            persentase = persentase
        )
    }
}
