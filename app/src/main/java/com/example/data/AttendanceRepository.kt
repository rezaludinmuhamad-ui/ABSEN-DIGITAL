package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AttendanceRepository(private val studentDao: StudentDao) {
    val allSiswa: Flow<List<Siswa>> = studentDao.getAllSiswa()
    val allAbsensi: Flow<List<AbsensiDenganSiswa>> = studentDao.getAbsensiDenganSiswa()

    suspend fun getSiswaByNis(nis: String): Siswa? {
        return studentDao.getSiswaByNis(nis)
    }

    suspend fun insertSiswa(siswa: Siswa) {
        studentDao.insertSiswa(siswa)
    }

    suspend fun updateSiswa(siswa: Siswa) {
        studentDao.updateSiswa(siswa)
    }

    suspend fun deleteSiswa(siswa: Siswa) {
        studentDao.deleteSiswa(siswa)
    }

    suspend fun insertAbsensi(absensi: Absensi) {
        studentDao.insertAbsensi(absensi)
    }

    suspend fun deleteAbsensiById(id: Int) {
        studentDao.deleteAbsensiById(id)
    }

    suspend fun clearAllData() {
        studentDao.deleteAllAbsensi()
        studentDao.deleteAllSiswa()
    }

    suspend fun populateSampleDataIfNeeded() {
        val currentSiswa = allSiswa.first()
        if (currentSiswa.isEmpty()) {
            val samples = listOf(
                Siswa(nis = "220101", nama = "Ahmad Yusuf Al-Fatih", kelas = "MTS 7-A", faceFeaturesRegistered = true),
                Siswa(nis = "220102", nama = "Siti Fatimah Azzahra", kelas = "MTS 7-A", faceFeaturesRegistered = true),
                Siswa(nis = "220103", nama = "Muhammad Ridwan Hakim", kelas = "MTS 7-B", faceFeaturesRegistered = true),
                Siswa(nis = "220104", nama = "Aisyah Humaira Putri", kelas = "MTS 7-B", faceFeaturesRegistered = false),
                Siswa(nis = "220105", nama = "Faisal Ibrahim Lubis", kelas = "MTS 8-A", faceFeaturesRegistered = true),
                Siswa(nis = "220106", nama = "Zahra Khairunnisa", kelas = "MTS 8-A", faceFeaturesRegistered = false),
                Siswa(nis = "220107", nama = "Habib Rizky Ramadhan", kelas = "MTS 8-B", faceFeaturesRegistered = true),
                Siswa(nis = "220108", nama = "Luqmanul Hakim", kelas = "MTS 9-A", faceFeaturesRegistered = true),
                Siswa(nis = "220109", nama = "Siti Aminah Ma'ruf", kelas = "MTS 9-B", faceFeaturesRegistered = true)
            )
            for (siswa in samples) {
                studentDao.insertSiswa(siswa)
            }

            // Also populate some historic logs
            val someLogs = listOf(
                Absensi(siswaNis = "220101", timestamp = System.currentTimeMillis() - 3600000 * 24, status = "Hadir", metode = "Scan Wajah", catatan = "Datang awal"),
                Absensi(siswaNis = "220102", timestamp = System.currentTimeMillis() - 3600000 * 23, status = "Hadir", metode = "Barcode", catatan = "Sesuai jadwal"),
                Absensi(siswaNis = "220103", timestamp = System.currentTimeMillis() - 3600000 * 22, status = "Terlambat", metode = "Manual", catatan = "Ban sepeda bocor"),
                Absensi(siswaNis = "220105", timestamp = System.currentTimeMillis() - 3100000, status = "Hadir", metode = "Scan Wajah", catatan = "Wajah terverifikasi"),
                Absensi(siswaNis = "220108", timestamp = System.currentTimeMillis() - 1500000, status = "Terlambat", metode = "Barcode", catatan = "Gerbang hampir tutup")
            )
            for (log in someLogs) {
                studentDao.insertAbsensi(log)
            }
        }
    }
}
