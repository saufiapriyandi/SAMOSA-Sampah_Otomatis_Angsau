# 5.1 Implementasi Relasi Antar Tabel

Implementasi relasi antar tabel pada sistem SAMOSA dirancang berdasarkan kebutuhan aplikasi pemantauan kapasitas tempat sampah otomatis. Berdasarkan hasil analisis project, sistem memiliki beberapa data utama yaitu data akun, profil, tempat sampah, sensor, data pembacaan sensor, notifikasi, laporan, dan detail laporan. Relasi antar tabel dibangun agar setiap data dapat saling terhubung secara logis serta memudahkan proses penyimpanan, pencarian, pengolahan, dan pelaporan data.

Tabel `akun` digunakan untuk menyimpan data pengguna yang dapat masuk ke dalam sistem. Setiap akun memiliki satu profil, sehingga relasi antara tabel `akun` dan tabel `profil` adalah one to one. Relasi ini digunakan agar data autentikasi pengguna dipisahkan dari data identitas pengguna, sehingga struktur database menjadi lebih rapi dan mudah dikelola.

Tabel `tempat_sampah` digunakan untuk menyimpan data utama setiap tong sampah yang dipantau oleh sistem, seperti kode bin, lokasi, kapasitas, status, dan kondisi aktif atau tidak aktif. Setiap tempat sampah dapat dipasangi satu atau lebih sensor, sehingga relasi antara tabel `tempat_sampah` dan tabel `sensor` adalah one to many. Pemisahan ini diperlukan karena pada implementasi sistem IoT, satu perangkat tempat sampah dapat memiliki beberapa komponen sensor sesuai kebutuhan monitoring.

Tabel `data_sensor` digunakan untuk menyimpan histori pembacaan sensor. Setiap data sensor berasal dari satu sensor dan berhubungan dengan satu tempat sampah. Oleh karena itu, tabel `data_sensor` memiliki foreign key `id_sensor` dan `id_tempat_sampah`. Relasi ini memungkinkan sistem menyimpan data pembacaan seperti persentase sampah, jarak sensor, status tutup, waktu baca, dan status kondisi tong dari waktu ke waktu. Struktur ini penting karena data pembacaan bersifat dinamis dan terus bertambah.

Tabel `notifikasi` digunakan untuk menyimpan informasi pemberitahuan yang diterima pengguna ketika tempat sampah berada dalam kondisi penuh atau ketika sistem mengirim pengingat berkala. Setiap notifikasi dikirim kepada satu akun dan berkaitan dengan satu tempat sampah. Dengan demikian, relasi tabel `akun` ke `notifikasi` adalah one to many, dan relasi tabel `tempat_sampah` ke `notifikasi` juga one to many.

Tabel `laporan` digunakan untuk menyimpan data laporan monitoring yang dibuat oleh sistem, baik dalam periode harian maupun mingguan. Setiap laporan dibuat oleh satu akun, sehingga relasi antara tabel `akun` dan `laporan` adalah one to many. Karena satu laporan dapat memuat ringkasan dari banyak tempat sampah, maka dibuat tabel `detail_laporan` sebagai tabel rincian. Relasi antara tabel `laporan` dan `detail_laporan` adalah one to many, sedangkan relasi antara tabel `tempat_sampah` dan `detail_laporan` juga one to many. Dengan struktur ini, satu tempat sampah dapat muncul pada banyak laporan yang berbeda sesuai periode pelaporan.

Secara keseluruhan, implementasi relasi antar tabel pada sistem SAMOSA bertujuan untuk menjaga integritas data, mengurangi redundansi, serta mendukung proses monitoring sampah secara real time dan terstruktur. Model relasi ini juga memudahkan pengembangan sistem di masa mendatang apabila aplikasi akan dipindahkan dari penyimpanan lokal dan Firebase ke database relasional seperti MySQL atau PostgreSQL.

## 5.2 Kardinalitas Relasi Antar Tabel

Kardinalitas relasi antar tabel pada sistem SAMOSA adalah sebagai berikut:

1. Satu `akun` memiliki satu `profil`.
2. Satu `akun` dapat membuat banyak `laporan`.
3. Satu `akun` dapat menerima banyak `notifikasi`.
4. Satu `tempat_sampah` dapat memiliki banyak `sensor`.
5. Satu `sensor` dapat menghasilkan banyak `data_sensor`.
6. Satu `tempat_sampah` dapat memiliki banyak `data_sensor`.
7. Satu `tempat_sampah` dapat menghasilkan banyak `notifikasi`.
8. Satu `laporan` memiliki banyak `detail_laporan`.
9. Satu `tempat_sampah` dapat muncul di banyak `detail_laporan`.

## 5.3 Deskripsi Singkat Fungsi Setiap Tabel

1. `akun` berfungsi menyimpan data login pengguna.
2. `profil` berfungsi menyimpan identitas lengkap pengguna.
3. `tempat_sampah` berfungsi menyimpan data master tong sampah.
4. `sensor` berfungsi menyimpan data sensor yang dipasang pada tong sampah.
5. `data_sensor` berfungsi menyimpan histori pembacaan sensor.
6. `notifikasi` berfungsi menyimpan riwayat notifikasi sistem.
7. `laporan` berfungsi menyimpan header laporan monitoring.
8. `detail_laporan` berfungsi menyimpan rincian isi laporan berdasarkan tempat sampah.
