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

    private val database = FirebaseDatabase.getInstance("https://samosa-sampah-otomatis-angsau-default-rtdb.asia-southeast1.firebasedatabase.app/")
    // Mengubah referensi ke root ("") karena data Tempat_Sampah_1 ada di tingkat paling atas
    private val rootRef = database.getReference("")

    override fun getDaftarTempatSampahRealtime(): Flow<List<TempatSampah>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bins = mutableListOf<TempatSampah>()
                
                // Pertama, cek apakah ada folder "tempat_sampah"
                val folderTempatSampah = snapshot.child("tempat_sampah")
                if (folderTempatSampah.exists()) {
                    for (child in folderTempatSampah.children) {
                        parseBinData(child)?.let { bins.add(it) }
                    }
                }

                // Kedua, cek data yang ada di root (seperti Tempat_Sampah_1)
                for (child in snapshot.children) {
                    // Hindari folder "tempat_sampah" itu sendiri agar tidak double
                    if (child.key == "tempat_sampah") continue
                    
                    // Kita hanya ambil node yang memiliki field kapasitas_persen atau persentase
                    if (child.hasChild("kapasitas_persen") || child.hasChild("persentase") || child.hasChild("lokasi")) {
                        parseBinData(child)?.let { bin ->
                            // Pastikan ID unik (tidak duplikat dengan yang di folder)
                            if (bins.none { it.binId == bin.binId }) {
                                bins.add(bin)
                            }
                        }
                    }
                }
                
                trySend(bins)
            }

            private fun parseBinData(child: DataSnapshot): TempatSampah? {
                val binId = child.child("binId").getValue(String::class.java) ?: child.key ?: return null
                val lokasi = child.child("lokasi").getValue(String::class.java) ?: "Alat SAMOSA Asli"
                
                // Mendukung kunci 'persentase' atau 'kapasitas_persen' dari IoT
                val persentase = child.child("persentase").getValue(Int::class.java) 
                    ?: child.child("kapasitas_persen").getValue(Int::class.java) 
                    ?: 0
                    
                val isActive = child.child("isActive").getValue(Boolean::class.java) ?: true
                val notifThreshold = child.child("notifThreshold").getValue(Int::class.java) ?: 90
                
                return TempatSampah(binId, lokasi, persentase, isActive, notifThreshold)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        rootRef.addValueEventListener(listener)
        awaitClose { rootRef.removeEventListener(listener) }
    }
}
