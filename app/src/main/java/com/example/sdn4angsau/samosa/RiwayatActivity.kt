package com.example.sdn4angsau.samosa

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sdn4angsau.samosa.databinding.ActivityRiwayatBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RiwayatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: RiwayatAdapter
    private val logList = mutableListOf<LogItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRiwayatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Window Insets (Padding Status Bar agar tidak tertutup notch)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (12 * resources.displayMetrics.density).toInt()
            binding.headerRiwayatBar.updatePadding(top = systemBars.top + extraTopPadding)
            insets
        }
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Setup RecyclerView
        adapter = RiwayatAdapter(logList)
        binding.rvFullRiwayat.layoutManager = LinearLayoutManager(this)
        binding.rvFullRiwayat.adapter = adapter

        // Hubungkan ke folder "Logs" di Firebase
        database = FirebaseDatabase.getInstance().getReference("Logs")

        // Tombol Kembali
        binding.btnBackRiwayat.setOnClickListener { finish() }

        // Tombol Hapus Semua Log
        binding.btnHapusLog.setOnClickListener { hapusSemuaRiwayat() }

        muatDataRiwayat()
    }

    private fun muatDataRiwayat() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                logList.clear()

                for (data in snapshot.children) {
                    val item = data.getValue(LogItem::class.java)
                    if (item != null) {
                        logList.add(item.copy(id = data.key ?: ""))
                    }
                }

                // Urutkan data berdasarkan timestamp terbaru (menurun)
                logList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()

                // Logika Tampilan Kosong (Empty State)
                if (logList.isEmpty()) {
                    binding.rvFullRiwayat.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvFullRiwayat.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RiwayatActivity, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun hapusSemuaRiwayat() {
        if (logList.isNotEmpty()) {
            database.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Seluruh riwayat berhasil dihapus", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Gagal menghapus riwayat", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Riwayat sudah kosong", Toast.LENGTH_SHORT).show()
        }
    }
}