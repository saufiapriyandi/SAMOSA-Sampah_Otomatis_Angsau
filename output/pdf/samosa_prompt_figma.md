# Prompt Figma SAMOSA

Buat satu board Figma berbahasa Indonesia yang merangkum dokumentasi visual aplikasi Android **SAMOSA (Sampah Otomatis Angsau)** berdasarkan **bukti yang benar-benar ada di repo**. Jangan mengarang backend, tabel server, sensor IoT, API, atau sinkronisasi cloud yang tidak terbukti di kode. Jika ada elemen yang tidak ditemukan, beri label tegas **"Not found in repo"**.

## Arah visual

- Gaya: profesional, rapi, mudah dibaca, cocok untuk presentasi akademik atau proyek sekolah.
- Layout: 3 kolom x 3 baris di satu board besar, masing-masing sel untuk satu diagram.
- Warna utama: hijau `#20B273`, hijau gelap `#166A4A`, oranye `#FFA500`, merah `#FF4B4B`, abu teks `#5F6B66`, latar `#F7FBF9`.
- Gunakan label komponen yang konsisten, ikon Android ringan, panah alur jelas, dan catatan kecil "berdasarkan repo" di footer board.

## Fakta sistem yang wajib dipakai

- Platform: aplikasi Android native Kotlin, satu modul `app`.
- Autentikasi:
  - Login lokal dengan username `admin123` dan password `admin123`.
  - Google Sign-In terhubung ke Firebase Auth.
- Layar utama:
  - `MainActivity` untuk login.
  - `DashboardActivity` untuk monitoring.
  - `DetailActivity` untuk riwayat per tong.
  - `ProfileActivity` untuk menu profil.
  - `TutorialActivity` untuk panduan.
  - `BinManagementActivity` untuk kelola tong.
  - `ReportActivity` untuk laporan.
- Lapisan data:
  - `DashboardViewModel` memakai `TempatSampahRepository`.
  - Implementasi aktif yang ada adalah `MockTempatSampahRepository`.
  - Sumber data runtime yang dipakai saat ini berasal dari `TempatSampahLocalStore`.
- Penyimpanan lokal:
  - SharedPreferences `SamosaLocalBins` menyimpan daftar bin sebagai JSON.
  - SharedPreferences `SesiSamosa` menyimpan status login lokal `SUDAH_LOGIN`.
  - SharedPreferences `SamosaNotificationState` menyimpan waktu notifikasi terakhir per bin.
- Model data inti:
  - `TempatSampah { binId, lokasi, persentase, isActive }`.
  - Status turunan: `AMAN`, `WASPADA`, `PENUH`.
- Logika simulasi:
  - `TempatSampahHistoryHelper` menghasilkan pola harian dan mingguan berdasarkan lokasi.
- Laporan:
  - `LaporanSampahHelper` membangun snapshot laporan harian dan mingguan.
  - Ekspor laporan berupa file `.txt` ke `cache/reports`.
  - Berbagi file memakai `FileProvider`.
- Notifikasi:
  - `TempatSampahNotificationHelper` membuat notifikasi langsung dan pengingat berkala.
  - `AlarmManager` menjadwalkan pengingat.
  - `TempatSampahReminderReceiver` menerima alarm lalu menampilkan reminder.
- Firebase:
  - `google-services.json` ada dan menunjuk ke project Firebase.
  - Dependency Firebase Realtime Database ada di Gradle.
  - CRUD runtime ke Firebase Realtime Database: **Not found in repo**.

## Instruksi diagram per panel

### 1. ERD database / model persistensi

Karena repo tidak menunjukkan database relasional nyata, tampilkan **ERD logis dari penyimpanan yang benar-benar ada**:

- Entity `TempatSampah`
  - `binId: String` (PK logis)
  - `lokasi: String`
  - `persentase: Int`
  - `isActive: Boolean`
  - `status: derived enum`
- Entity `SamosaLocalBins`
  - `bins_json: JSONArray`
  - relasi logis: menyimpan banyak `TempatSampah`
- Entity `SesiSamosa`
  - `SUDAH_LOGIN: Boolean`
- Entity `SamosaNotificationState`
  - `last_alert_<binId>: Long`
  - relasi logis ke `TempatSampah.binId`
- Entity `ReportExportFile`
  - `filename`
  - `content`
  - `generatedAt`
  - lokasi file: `cache/reports/*.txt`
- Tambahkan note kecil:
  - "Firebase Realtime Database schema: Not found in repo"
  - "Sensor raw readings table: Not found in repo"

### 2. Object diagram

Tampilkan snapshot objek runtime berikut:

- `mainActivity: MainActivity`
- `dashboardActivity: DashboardActivity`
- `dashboardViewModel: DashboardViewModel`
- `repo: MockTempatSampahRepository`
- `localStore: TempatSampahLocalStore`
- `bin1: TempatSampah {binId=3, lokasi=Laboratorium, persentase=100, isActive=true, status=PENUH}`
- `bin2: TempatSampah {binId=4, lokasi=Ruang Kantor, persentase=95, isActive=true, status=PENUH}`
- `bin3: TempatSampah {binId=5, lokasi=Kantin SDN 4, persentase=78, isActive=true, status=WASPADA}`
- `notifHelper: TempatSampahNotificationHelper`
- Hubungan objek:
  - DashboardActivity mengamati DashboardViewModel
  - DashboardViewModel memakai repo
  - repo membaca localStore
  - notifHelper menerima daftar bin aktif

### 3. Class diagram

Gambar class diagram dengan relasi yang jelas:

- `MainActivity`
- `DashboardActivity`
- `DashboardViewModel`
- `TempatSampahRepository <<interface>>`
- `MockTempatSampahRepository`
- `TempatSampahLocalStore <<object>>`
- `TempatSampah`
- `TempatSampahStatus <<enum>>`
- `TempatSampahAdapter`
- `DetailActivity`
- `TempatSampahHistoryHelper <<object>>`
- `BinManagementActivity`
- `BinManagementAdapter`
- `ReportActivity`
- `LaporanSampahHelper <<object>>`
- `ReportSnapshot`
- `ReportTableRow`
- `TempatSampahNotificationHelper <<object>>`
- `TempatSampahReminderReceiver`
- `ProfileActivity`
- `TutorialActivity`

Relasi utama:

- `DashboardActivity -> DashboardViewModel`
- `DashboardViewModel -> TempatSampahRepository`
- `MockTempatSampahRepository ..|> TempatSampahRepository`
- `MockTempatSampahRepository -> TempatSampahLocalStore`
- `TempatSampahAdapter -> TempatSampah`
- `DetailActivity -> TempatSampahHistoryHelper`
- `ReportActivity -> LaporanSampahHelper`
- `LaporanSampahHelper -> TempatSampahHistoryHelper`
- `LaporanSampahHelper -> ReportSnapshot`
- `ReportSnapshot -> ReportTableRow`
- `TempatSampahNotificationHelper -> TempatSampah`
- `TempatSampahReminderReceiver -> TempatSampahNotificationHelper`

### 4. Sequence diagram

Buat sequence diagram untuk alur **Login -> Dashboard -> Notifikasi**:

1. Pengguna membuka `MainActivity`
2. Pengguna memilih login lokal atau Google
3. `MainActivity` memvalidasi lokal atau meminta Google Sign-In
4. Jika Google, `MainActivity` mengirim kredensial ke `FirebaseAuth`
5. `MainActivity` menyimpan sesi lokal `SUDAH_LOGIN=true`
6. `MainActivity` membuka `DashboardActivity`
7. `DashboardActivity` meminta data ke `DashboardViewModel`
8. `DashboardViewModel` memanggil `MockTempatSampahRepository`
9. `MockTempatSampahRepository` membaca `TempatSampahLocalStore`
10. Data bin kembali ke `DashboardViewModel`
11. `DashboardViewModel` mengirim `DashboardUiState` ke `DashboardActivity`
12. `DashboardActivity` meminta `TempatSampahNotificationHelper.syncNotifications`
13. `TempatSampahNotificationHelper` menjadwalkan `AlarmManager`
14. Saat alarm aktif, `TempatSampahReminderReceiver` memanggil notifikasi pengingat

Tambahkan catatan:

- "Firebase Realtime Database read/write flow: Not found in repo"

### 5. State machine diagram

Buat state machine untuk status tong sampah:

- `Nonaktif`
- `Aman`
- `Waspada`
- `Penuh`

Transisi:

- `Nonaktif -> Aman/Waspada/Penuh` saat `isActive=true` dan status dihitung dari `persentase`
- `Aman -> Waspada` saat `persentase >= 60`
- `Waspada -> Penuh` saat `persentase >= 90`
- `Penuh -> Waspada` saat `persentase < 90`
- `Waspada -> Aman` saat `persentase < 60`
- `Aman/Waspada/Penuh -> Nonaktif` saat tong dinonaktifkan dari manajemen

### 6. Activity diagram

Buat activity diagram untuk alur **monitoring harian**:

- Mulai
- Login
- Buka Dashboard
- Muat data bin aktif
- Tampilkan ringkasan dan daftar tong
- Keputusan: ada tong penuh?
- Jika ya, tampilkan peringatan dan sinkronkan notifikasi
- Pengguna bisa cari lokasi
- Pengguna bisa buka detail tong
- Pengguna bisa buka profil
- Dari profil, pengguna bisa pilih tutorial / manajemen tong / laporan / logout
- Selesai

### 7. Use case diagram

Actor utama:

- `Kepala Sekolah / Admin Sekolah`

Actor eksternal pendukung:

- `Firebase Auth`
- `Google Sign-In`
- `Android AlarmManager / NotificationManager`
- `Aplikasi berbagi file lain`

Use case:

- Login lokal
- Login dengan Google
- Lihat dashboard
- Cari lokasi tong
- Lihat detail tong
- Kelola data tong
- Aktifkan / nonaktifkan tong
- Lihat tutorial
- Lihat laporan harian
- Lihat laporan mingguan
- Bagikan laporan
- Terima notifikasi tong penuh
- Logout

### 8. Deployment diagram

Gambarkan deployment sederhana:

- Node `Android Device`
  - App `SAMOSA APK`
  - `Activities`
  - `ViewModel`
  - `SharedPreferences`
  - `AlarmManager`
  - `BroadcastReceiver`
  - `NotificationManager`
  - `Cache/reports`
- Node eksternal `Firebase Project`
  - `Firebase Auth`
  - `Google Sign-In config`
  - `Realtime Database config only`
- Node eksternal `Share Target Apps`

Alur deployment:

- Android app menyimpan data lokal di SharedPreferences
- Android app mengakses Firebase Auth untuk login Google
- Android app membagikan file laporan melalui FileProvider ke app lain
- Tambahkan garis putus-putus dari app ke Firebase Realtime Database dengan label:
  - "Dependency/config ada, runtime CRUD Not found in repo"

### 9. Diagram arsitektur

Buat diagram arsitektur berlapis:

- Lapisan Presentasi:
  - MainActivity
  - DashboardActivity
  - DetailActivity
  - ProfileActivity
  - TutorialActivity
  - BinManagementActivity
  - ReportActivity
  - RecyclerView adapters
- Lapisan State / Logic:
  - DashboardViewModel
  - TempatSampahHistoryHelper
  - LaporanSampahHelper
  - TempatSampahNotificationHelper
- Lapisan Data:
  - TempatSampahRepository
  - MockTempatSampahRepository
  - TempatSampahLocalStore
  - SharedPreferences
  - Cache report file
- Layanan eksternal:
  - Firebase Auth
  - Google Sign-In
  - Android AlarmManager
  - NotificationManager
  - FileProvider

Alur data yang harus tampak:

- Login -> Firebase Auth / sesi lokal
- Dashboard -> ViewModel -> Repository -> LocalStore -> SharedPreferences
- Detail / Report -> HistoryHelper -> data simulasi
- Tong penuh -> NotificationHelper -> AlarmManager / Receiver / NotificationManager
- Share report -> ReportActivity -> LaporanSampahHelper -> file txt -> FileProvider -> app lain

## Catatan penting untuk Figma AI

- Semua label, keterangan, dan note gunakan bahasa Indonesia.
- Tulis note kecil di board: "Repo saat ini lebih menonjolkan simulasi lokal daripada integrasi sensor/backend."
- Jangan membuat tabel SQL, service API, atau pipeline IoT detail tanpa label **Not found in repo**.
- Tampilkan legenda status:
  - Hijau = Aman
  - Oranye = Waspada
  - Merah = Penuh
