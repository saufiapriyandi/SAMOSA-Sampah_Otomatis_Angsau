# Analisis Keamanan Aplikasi SAMOSA
## (Sampah Otomatis Angsau)

**Dokumen**: Analisis dan Justifikasi Temuan Keamanan  
**Versi**: 1.0  
**Tanggal**: Juni 2026  
**Alat Uji**: MobSF (Mobile Security Framework)  
**Tipe APK yang Diuji**: Debug Build → Release Build (setelah perbaikan)

---

## 1. Ringkasan Eksekutif

Berdasarkan hasil pengujian keamanan menggunakan MobSF, ditemukan **4 temuan High**, **9 temuan Medium**, dan **1 temuan Info**. Setelah analisis mendalam, kami mengklasifikasikan temuan menjadi:

| Klasifikasi | Jumlah | Tindakan |
|---|---|---|
| ✅ **Diperbaiki di kode** | 6 temuan | Perubahan kode dan konfigurasi |
| 📋 **False positive (library)** | 6 temuan | Didokumentasikan dengan justifikasi |
| 🔐 **Perilaku normal Firebase** | 2 temuan | Diamankan melalui Firebase Security Rules |

---

## 2. Temuan yang Telah Diperbaiki

### 2.1 [HIGH] Application Signed with Debug Certificate (H1)
- **Kategori**: CERTIFICATE
- **Deskripsi**: APK ditandatangani dengan sertifikat debug bawaan Android Studio.
- **Analisis**: Ini terjadi karena APK yang diuji adalah **debug build**. Sertifikat debug hanya digunakan saat development dan **tidak boleh digunakan untuk distribusi**.
- **Perbaikan**: 
  - Build release APK menggunakan keystore produksi melalui menu **Build > Generate Signed Bundle/APK** di Android Studio.
  - Sertifikat debug secara otomatis diganti dengan sertifikat produksi pada release build.
- **Status**: ✅ Teratasi saat build release.

### 2.2 [HIGH] Vulnerable Android Version — minSdk=24 (H2)
- **Kategori**: MANIFEST
- **Deskripsi**: Aplikasi dapat diinstal pada Android 7.0 (API 24) yang memiliki kerentanan keamanan yang sudah diketahui dan tidak lagi menerima patch keamanan.
- **Perbaikan**: 
  - `minSdk` dinaikkan dari **24** menjadi **26** (Android 8.0 Oreo).
  - Android 8.0+ mendukung Network Security Config secara native, perbaikan TLS, dan autofill framework.
- **File**: `app/build.gradle.kts`
- **Status**: ✅ Diperbaiki.

### 2.3 [HIGH] Debug Enabled — android:debuggable=true (H3)
- **Kategori**: MANIFEST
- **Deskripsi**: Flag `debuggable=true` ditemukan di manifest merged.
- **Analisis**: Flag ini **secara otomatis di-set oleh Gradle** untuk debug build variant. Pada release build, flag ini **secara default bernilai false**.
- **Perbaikan**: 
  - Ditambahkan `isDebuggable = false` secara eksplisit pada release build type.
  - Ditambahkan komentar dokumentasi pada debug build type.
- **File**: `app/build.gradle.kts`
- **Status**: ✅ Diperbaiki (eksplisit di release).

### 2.4 [HIGH] Test Mode — android:testOnly=true (H4)
- **Kategori**: MANIFEST
- **Deskripsi**: Flag `testOnly=true` ditemukan di manifest merged.
- **Analisis**: Sama seperti H3, flag ini **secara otomatis di-set oleh Android Studio** untuk debug build agar bisa di-sideload tanpa melalui Play Store. Pada release build, flag ini **tidak ada**.
- **Perbaikan**: Build release APK secara otomatis menghilangkan flag ini.
- **Status**: ✅ Teratasi saat build release.

### 2.5 [MEDIUM] Application Data Can Be Backed Up — allowBackup=true (M1)
- **Kategori**: MANIFEST
- **Deskripsi**: Data aplikasi dapat di-backup ke Google Drive, berpotensi mengekspos data sensitif.
- **Perbaikan**: 
  - `android:allowBackup` diubah dari `true` menjadi **`false`**.
  - Backup rules (`backup_rules.xml`, `data_extraction_rules.xml`) tetap dipertahankan sebagai fallback.
- **File**: `app/src/main/AndroidManifest.xml`
- **Status**: ✅ Diperbaiki.

### 2.6 [MEDIUM] App Creates Temp File (M8)
- **Kategori**: CODE
- **Deskripsi**: Aplikasi membuat file temporer yang berpotensi menyimpan data sensitif.
- **Analisis**: File yang ditulis adalah laporan rekap sampah (non-sensitif), namun penulisan langsung ke `cacheDir` root tidak ideal.
- **Perbaikan**: 
  - File laporan sekarang ditulis ke subdirectory `cacheDir/reports/` yang terdaftar di `file_paths.xml`.
  - File ditimpa (bukan membuat file baru) setiap kali laporan digenerate.
- **File**: `app/src/main/java/.../SamosaReportGenerator.kt`
- **Status**: ✅ Diperbaiki.

---

## 3. Temuan False Positive — Komponen Library Eksternal

Temuan-temuan berikut berasal dari **library pihak ketiga** (Firebase SDK, Google Play Services, AndroidX) yang **tidak dapat dan tidak seharusnya diubah** oleh developer aplikasi.

### 3.1 [MEDIUM] Firebase GenericIdpActivity — exported=true (M2)
- **Kategori**: MANIFEST  
- **Komponen**: `com.google.firebase.auth.internal.GenericIdpActivity`
- **Analisis**: Activity ini adalah **komponen internal Firebase Authentication SDK** yang menangani alur autentikasi Identity Provider (Google, Facebook, dll). Activity ini **harus** di-export agar dapat menerima callback dari browser/intent autentikasi eksternal.
- **Justifikasi**: Jika `exported` diubah ke `false`, fitur **Google Sign-In akan berhenti berfungsi** karena callback dari Intent autentikasi tidak dapat diterima oleh aplikasi.
- **Mitigasi**: 
  - Firebase Authentication SDK mengimplementasikan validasi internal pada callback yang diterima.
  - Akses data dilindungi oleh Firebase Security Rules yang memerlukan autentikasi (`auth != null`).
- **Status**: 📋 False positive — komponen library yang berfungsi sesuai desain.

### 3.2 [MEDIUM] Firebase RecaptchaActivity — exported=true (M3)
- **Kategori**: MANIFEST
- **Komponen**: `com.google.firebase.auth.internal.RecaptchaActivity`
- **Analisis**: Activity ini menangani tantangan reCAPTCHA dalam alur autentikasi Firebase (verifikasi nomor telepon, rate limiting). Activity ini **harus** di-export untuk menerima hasil reCAPTCHA dari Google Play Services.
- **Justifikasi**: Sama seperti M2 — mengubah `exported` akan merusak fungsionalitas autentikasi.
- **Status**: 📋 False positive — komponen library yang berfungsi sesuai desain.

### 3.3 [MEDIUM] RevocationBoundService — Permission Level (M4)
- **Kategori**: MANIFEST
- **Komponen**: `com.google.android.gms.auth.api.signin.RevocationBoundService`
- **Analisis**: Service ini adalah bagian dari **Google Play Services Auth SDK** yang menangani revokasi token. Service ini dilindungi oleh permission `com.google.android.gms.auth.api.signin.permission.REVOCATION_NOTIFICATION` yang didefinisikan oleh Google Play Services.
- **Justifikasi**: Permission ini bersifat **signature-level** — hanya aplikasi yang ditandatangani oleh Google yang dapat mengakses service ini. MobSF memberikan peringatan karena tidak dapat memverifikasi protection level permission dari library eksternal.
- **Status**: 📋 False positive — dilindungi oleh signature-level permission.

### 3.4 [MEDIUM] ProfileInstallReceiver — Permission Level (M5)
- **Kategori**: MANIFEST
- **Komponen**: `androidx.profileinstaller.ProfileInstallReceiver`
- **Analisis**: Receiver ini adalah bagian dari **AndroidX Profile Installer** yang digunakan untuk mengoptimalkan performa aplikasi melalui Baseline Profiles. Receiver ini dilindungi oleh permission `android.permission.DUMP`.
- **Justifikasi**: Permission `DUMP` bersifat **signature|privileged** — hanya dapat diakses oleh aplikasi sistem atau aplikasi yang ditandatangani dengan sertifikat platform.
- **Status**: 📋 False positive — dilindungi oleh signature-level permission.

### 3.5 [MEDIUM] Insecure Random Number Generator (M7)
- **Kategori**: CODE
- **Analisis**: MobSF mendeteksi penggunaan `java.util.Random` di dalam **bytecode APK**. Setelah analisis kode sumber, **tidak ada** penggunaan `java.util.Random` di kode aplikasi SAMOSA. Penggunaan ini berasal dari:
  - **HiveMQ MQTT Client** — untuk pembuatan client ID.
  - **Firebase SDK** — untuk internal randomization.
  - **Netty** — untuk event loop dan connection management.
- **Justifikasi**: Library-library tersebut tidak menggunakan Random untuk keperluan kriptografi. Untuk keperluan kriptografi, aplikasi SAMOSA menggunakan `MasterKey` dengan `AES256_GCM` (melalui `SecurityHelper.kt`).
- **Mitigasi Tambahan**: ProGuard/R8 mengoptimasi dan meng-obfuscate bytecode pada release build, mempersulit analisis statis.
- **Status**: 📋 False positive — berasal dari library, bukan kode aplikasi.

### 3.6 [INFO] App Logs Information (I1)
- **Kategori**: CODE
- **Analisis**: Setelah audit menyeluruh terhadap **26 file kode sumber** aplikasi SAMOSA, **tidak ditemukan satupun** panggilan `Log.d()`, `Log.i()`, `Log.e()`, `Log.w()`, atau `Log.v()`. Temuan ini berasal dari library Firebase dan HiveMQ MQTT.
- **Mitigasi**: 
  - Ditambahkan rule ProGuard `-assumenosideeffects` yang **menghapus semua panggilan `android.util.Log`** dari bytecode APK release, termasuk yang berasal dari library.
  - Setelah build release, temuan ini **tidak akan muncul** karena bytecode Log sudah di-strip.
- **File Perbaikan**: `app/proguard-rules.pro`
- **Status**: 📋 Dimitigasi melalui ProGuard rule stripping.

---

## 4. Temuan Terkait Hardcoded Secrets

### 4.1 [MEDIUM] Hardcoded Sensitive Information / Secrets (M6, M9)
- **Kategori**: CODE, SECRETS
- **Deskripsi**: MobSF mendeteksi API key dan OAuth Client ID di file `google-services.json`.
- **Analisis**: File `google-services.json` adalah **konfigurasi standar Firebase** yang **harus** ada di project Android. Informasi di dalamnya meliputi:
  - `current_key`: Firebase API Key
  - `client_id`: OAuth 2.0 Client ID
  - `project_number`, `project_id`, `firebase_url`
- **Justifikasi Keamanan**:
  1. **API Key Firebase bersifat publik** — Google merancang API key ini untuk digunakan di client-side. Key ini hanya berfungsi sebagai identifier, bukan sebagai secret.
  2. **Keamanan data dilindungi oleh Firebase Security Rules**, bukan oleh kerahasiaan API key. Rules yang diterapkan (`database.rules.json`) mensyaratkan `auth != null` untuk setiap operasi read/write.
  3. **OAuth Client ID dibatasi oleh SHA-1 certificate** — hanya APK yang ditandatangani dengan certificate yang terdaftar di Firebase Console yang dapat menggunakan client ID ini.
  4. **Google merekomendasikan** untuk TIDAK menambahkan `google-services.json` ke `.gitignore` pada project private, karena file ini diperlukan untuk CI/CD.
- **Referensi**: [Firebase Documentation — Understanding Firebase Projects](https://firebase.google.com/docs/projects/learn-more#config-files-objects)
- **Status**: 📋 Perilaku normal Firebase — diamankan melalui Security Rules dan SHA-1 certificate binding.

---

## 5. Temuan Positif (Secure)

| Temuan | Kategori | Status |
|---|---|---|
| SSL Certificate Pinning terdeteksi | CODE | ✅ Secure |
| Firebase Remote Config disabled | FIREBASE | ✅ Secure |

---

## 6. Langkah Keamanan yang Sudah Diterapkan

| Aspek Keamanan | Implementasi | File |
|---|---|---|
| **Confidentiality** — Enkripsi data lokal | EncryptedSharedPreferences (AES-256-GCM) | `SecurityHelper.kt` |
| **Integrity** — Verifikasi data | HMAC SHA-256 | `SecurityHelper.kt` |
| **Availability** — Validasi input | Whitelist, `coerceIn()`, `.take(100)` | `DetailActivity.kt`, `TempatSampahRepository.kt` |
| **Obfuscation** — Code protection | ProGuard/R8 (isMinifyEnabled = true) | `build.gradle.kts` |
| **Network Security** — HTTPS only | Network Security Config | `network_security_config.xml` |
| **Log Stripping** — Hapus log di release | ProGuard `-assumenosideeffects` | `proguard-rules.pro` |
| **Auth** — Autentikasi pengguna | Firebase Auth + Google Sign-In | `MainActivity.kt` |
| **Authorization** — Akses data | Firebase Security Rules (`auth != null`) | `database.rules.json` |

---

## 7. Kesimpulan

Dari **14 temuan** MobSF:
- **6 temuan** telah **diperbaiki** melalui perubahan konfigurasi dan kode.
- **6 temuan** adalah **false positive** yang berasal dari library pihak ketiga dan tidak dapat diubah tanpa merusak fungsionalitas aplikasi.
- **2 temuan** terkait `google-services.json` yang merupakan **perilaku normal** Firebase Android SDK dan diamankan melalui mekanisme lain (Security Rules, SHA-1 binding).

Aplikasi SAMOSA telah menerapkan prinsip **CIA Triad** (Confidentiality, Integrity, Availability) dan best practice keamanan Android yang sesuai.
