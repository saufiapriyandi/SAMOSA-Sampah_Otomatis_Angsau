# ERD Sederhana SAMOSA

Versi ini dibuat lebih sederhana agar mirip dengan contoh diagram relasi tabel pada laporan.

## Entitas

1. `akun`
2. `profil`
3. `tempat_sampah`
4. `sensor`
5. `data_sampah`
6. `notifikasi`
7. `laporan`

## Relasi

1. `akun` 1:1 `profil`
2. `akun` 1:N `laporan`
3. `akun` 1:N `notifikasi`
4. `tempat_sampah` 1:N `sensor`
5. `tempat_sampah` 1:N `data_sampah`
6. `tempat_sampah` 1:N `notifikasi`

## Mermaid

```mermaid
erDiagram
    AKUN ||--|| PROFIL : memiliki
    AKUN ||--o{ LAPORAN : membuat
    AKUN ||--o{ NOTIFIKASI : menerima
    TEMPAT_SAMPAH ||--o{ SENSOR : memiliki
    TEMPAT_SAMPAH ||--o{ DATA_SAMPAH : menghasilkan
    TEMPAT_SAMPAH ||--o{ NOTIFIKASI : memicu

    AKUN {
        int id_akun PK
        varchar username
        varchar email
        varchar password
    }

    PROFIL {
        int id_profil PK
        int id_akun FK
        varchar nama
        varchar jabatan
        varchar no_hp
        text alamat
    }

    TEMPAT_SAMPAH {
        int id_tempat_sampah PK
        varchar kode_bin
        varchar lokasi
        int kapasitas_persen
        varchar status
        boolean is_active
    }

    SENSOR {
        int id_sensor PK
        int id_tempat_sampah FK
        varchar jenis_sensor
        varchar status_sensor
    }

    DATA_SAMPAH {
        int id_data PK
        int id_tempat_sampah FK
        int volume_sampah
        date tanggal
        time waktu
        varchar status
    }

    NOTIFIKASI {
        int id_notifikasi PK
        int id_akun FK
        int id_tempat_sampah FK
        text pesan
        varchar status_baca
        datetime tanggal
    }

    LAPORAN {
        int id_laporan PK
        int id_akun FK
        date tanggal_laporan
        varchar periode
        int jumlah_penuh
        text keterangan
    }
```

## Bentuk relasi untuk ditulis di laporan

- Tabel `akun` berelasi satu banding satu dengan tabel `profil`.
- Tabel `akun` berelasi satu banding banyak dengan tabel `laporan`.
- Tabel `akun` berelasi satu banding banyak dengan tabel `notifikasi`.
- Tabel `tempat_sampah` berelasi satu banding banyak dengan tabel `sensor`.
- Tabel `tempat_sampah` berelasi satu banding banyak dengan tabel `data_sampah`.
- Tabel `tempat_sampah` berelasi satu banding banyak dengan tabel `notifikasi`.
