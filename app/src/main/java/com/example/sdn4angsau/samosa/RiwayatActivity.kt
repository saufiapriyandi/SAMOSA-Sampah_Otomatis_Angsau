package com.example.sdn4angsau.samosa

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.example.sdn4angsau.samosa.databinding.ActivityRiwayatBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Model Data untuk Log
data class LogSamosa(
    val pesan: String = "",
    val waktu: String = "",
    val tipe: String = "info"
)

// Adapter untuk RecyclerView
class LogAdapter(private val logList: List<LogSamosa>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPesan: TextView = view.findViewById(R.id.tvLogPesan)
        val tvWaktu: TextView = view.findViewById(R.id.tvLogWaktu)
        val dotStatus: CardView = view.findViewById(R.id.dotStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logList[position]
        holder.tvPesan.text = log.pesan
        holder.tvWaktu.text = log.waktu

        val warnaTitik = when (log.tipe) {
            "warning" -> "#FFA500" // Orange (Terbuka)
            "success" -> "#20B273" // Hijau (Tertutup)
            else -> "#5C6BC0" // Ungu/Biru (Bawaan)
        }
        holder.dotStatus.setCardBackgroundColor(Color.parseColor(warnaTitik))
    }

    override fun getItemCount(): Int = logList.size
}

class RiwayatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatBinding
    private val listRiwayat = mutableListOf<LogSamosa>()
    private lateinit var logAdapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRiwayatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.headerRiwayatBar.updatePadding(top = systemBars.top + 16)
            insets
        }

        binding.btnBackRiwayat.setOnClickListener { finish() }

        // Mengambil Nama Profil (Gunakan "Kepala Sekolah" sebagai default jika kosong)
        val sharedPref = getSharedPreferences("SamosaPrefs", Context.MODE_PRIVATE)

        // Catatan: Pastikan "USER_NAME" ini adalah kunci yang sama dengan saat Anda menyimpan profil
        val namaProfil = sharedPref.getString("USER_NAME", "Kepala Sekolah")
        binding.tvHaloNama.text = "Halo, $namaProfil"

        logAdapter = LogAdapter(listRiwayat)
        binding.rvFullRiwayat.adapter = logAdapter

        // Tarik data dari Firebase
        val dbRef = FirebaseDatabase.getInstance().getReference("Logs")

        dbRef.orderByKey().limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listRiwayat.clear()
                for (logSnapshot in snapshot.children) {
                    val pesan = logSnapshot.child("pesan").getValue(String::class.java) ?: ""
                    val waktu = logSnapshot.child("waktu").getValue(String::class.java) ?: ""
                    val tipe = logSnapshot.child("tipe").getValue(String::class.java) ?: "info"

                    listRiwayat.add(0, LogSamosa(pesan, waktu, tipe))
                }
                logAdapter.notifyDataSetChanged()

                // Cek apakah data kosong untuk menampilkan Empty State
                if (listRiwayat.isEmpty()) {
                    binding.rvFullRiwayat.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvFullRiwayat.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Tombol Hapus Log
        binding.btnHapusLog.setOnClickListener {
            dbRef.removeValue().addOnSuccessListener {
                Toast.makeText(this, "Riwayat berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
        }
    }
}