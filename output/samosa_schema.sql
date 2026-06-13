CREATE TABLE akun (
    id_akun INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100),
    password VARCHAR(255),
    provider_login VARCHAR(20) DEFAULT 'local',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE profil (
    id_profil INT AUTO_INCREMENT PRIMARY KEY,
    id_akun INT NOT NULL UNIQUE,
    nama VARCHAR(100),
    jabatan VARCHAR(100),
    no_hp VARCHAR(20),
    alamat TEXT,
    CONSTRAINT fk_profil_akun
        FOREIGN KEY (id_akun) REFERENCES akun(id_akun)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE tempat_sampah (
    id_tempat_sampah INT AUTO_INCREMENT PRIMARY KEY,
    kode_bin VARCHAR(30) NOT NULL UNIQUE,
    lokasi VARCHAR(100) NOT NULL,
    kapasitas_persen INT NOT NULL CHECK (kapasitas_persen BETWEEN 0 AND 100),
    status ENUM('AMAN','WASPADA','PENUH') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sensor (
    id_sensor INT AUTO_INCREMENT PRIMARY KEY,
    id_tempat_sampah INT NOT NULL,
    jenis_sensor VARCHAR(50) NOT NULL,
    status_sensor VARCHAR(30) NOT NULL,
    posisi_sensor VARCHAR(50),
    CONSTRAINT fk_sensor_tempat_sampah
        FOREIGN KEY (id_tempat_sampah) REFERENCES tempat_sampah(id_tempat_sampah)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE data_sensor (
    id_data_sensor INT AUTO_INCREMENT PRIMARY KEY,
    id_sensor INT NOT NULL,
    id_tempat_sampah INT NOT NULL,
    persentase_sampah INT NOT NULL CHECK (persentase_sampah BETWEEN 0 AND 100),
    jarak_cm INT,
    status_tutup VARCHAR(30),
    waktu_baca DATETIME NOT NULL,
    status_bin ENUM('AMAN','WASPADA','PENUH') NOT NULL,
    CONSTRAINT fk_data_sensor_sensor
        FOREIGN KEY (id_sensor) REFERENCES sensor(id_sensor)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_data_sensor_tempat_sampah
        FOREIGN KEY (id_tempat_sampah) REFERENCES tempat_sampah(id_tempat_sampah)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE notifikasi (
    id_notifikasi INT AUTO_INCREMENT PRIMARY KEY,
    id_akun INT NOT NULL,
    id_tempat_sampah INT NOT NULL,
    judul VARCHAR(100) NOT NULL,
    pesan TEXT NOT NULL,
    jenis_notifikasi ENUM('PENUH','REMINDER') NOT NULL,
    status_baca ENUM('BELUM_DIBACA','SUDAH_DIBACA') DEFAULT 'BELUM_DIBACA',
    waktu_kirim DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifikasi_akun
        FOREIGN KEY (id_akun) REFERENCES akun(id_akun)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_notifikasi_tempat_sampah
        FOREIGN KEY (id_tempat_sampah) REFERENCES tempat_sampah(id_tempat_sampah)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE laporan (
    id_laporan INT AUTO_INCREMENT PRIMARY KEY,
    id_akun INT NOT NULL,
    periode ENUM('HARIAN','MINGGUAN') NOT NULL,
    tanggal_laporan DATE NOT NULL,
    total_kejadian_penuh INT DEFAULT 0,
    jumlah_tempat_terdampak INT DEFAULT 0,
    insight TEXT,
    keterangan TEXT,
    CONSTRAINT fk_laporan_akun
        FOREIGN KEY (id_akun) REFERENCES akun(id_akun)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE detail_laporan (
    id_detail_laporan INT AUTO_INCREMENT PRIMARY KEY,
    id_laporan INT NOT NULL,
    id_tempat_sampah INT NOT NULL,
    jumlah_penuh INT DEFAULT 0,
    jam_rawan VARCHAR(100),
    CONSTRAINT fk_detail_laporan_laporan
        FOREIGN KEY (id_laporan) REFERENCES laporan(id_laporan)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_detail_laporan_tempat_sampah
        FOREIGN KEY (id_tempat_sampah) REFERENCES tempat_sampah(id_tempat_sampah)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);
