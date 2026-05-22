package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "siswa")
data class Siswa(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nis: String, // Nomor Induk Siswa (used as Barcode / QR ID)
    val nama: String,
    val kelas: String,
    val photoUri: String? = null, // Path to local registered photo for face scan
    val faceFeaturesRegistered: Boolean = false // Simulation face database registration
)

@Entity(tableName = "absensi")
data class Absensi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val siswaNis: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // Hadir, Terlambat, Izin, Sakit, Alpa
    val metode: String, // Barcode/QR, Scan Wajah, Manual
    val catatan: String? = null
)

// Combined UI representation
data class AbsensiDenganSiswa(
    val absensiId: Int,
    val studentNis: String,
    val studentNama: String,
    val studentKelas: String,
    val timestamp: Long,
    val status: String,
    val metode: String,
    val catatan: String?
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    val formattedDate: String
        get() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
