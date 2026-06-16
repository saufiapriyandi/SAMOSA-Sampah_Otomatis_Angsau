package com.example.sdn4angsau.samosa

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface TempatSampahRepository {
    fun getDaftarTempatSampahRealtime(): Flow<List<TempatSampah>>
}

class FirebaseTempatSampahRepository : TempatSampahRepository {

    private val database = FirebaseDatabase.getInstance().getReference("tempat_sampah")

    override fun getDaftarTempatSampahRealtime(): Flow<List<TempatSampah>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listBins = mutableListOf<TempatSampah>()

                for (data in snapshot.children) {
                    val binId = data.key ?: ""
                    val lokasi = data.child("lokasi").getValue(String::class.java) ?: "Tanpa Lokasi"
                    val isActive = data.child("isActive").getValue(Boolean::class.java) ?: true

                    // OPSI A: Jika di Firebase Anda sudah menyimpan field "persentase" secara langsung
                    val persenData = data.child("persentase").getValue(Int::class.java) ?: 0

                    /*
                    // OPSI B: Jika di Firebase berupa jarak cm (Contoh: Tinggi total tong = 50cm)
                    val jarak = data.child("jarakSensor").getValue(Int::class.java) ?: 50
                    val tinggiTongMaksimal = 50
                    val persenData = (((tinggiTongMaksimal - jarak).toFloat() / tinggiTongMaksimal) * 100).toInt().coerceIn(0, 100)
                    */

                    val tempatSampah = TempatSampah(
                        binId = binId,
                        lokasi = lokasi,
                        isActive = isActive,
                        persentase = persenData
                    )
                    listBins.add(tempatSampah)
                }
                trySend(listBins)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.addValueEventListener(listener)
        awaitClose { database.removeEventListener(listener) }
    }
}