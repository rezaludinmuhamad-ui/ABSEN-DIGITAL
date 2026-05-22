package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    // Siswa Queries
    @Query("SELECT * FROM siswa ORDER BY nama ASC")
    fun getAllSiswa(): Flow<List<Siswa>>

    @Query("SELECT * FROM siswa WHERE nis = :nis LIMIT 1")
    suspend fun getSiswaByNis(nis: String): Siswa?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSiswa(siswa: Siswa)

    @Update
    suspend fun updateSiswa(siswa: Siswa)

    @Delete
    suspend fun deleteSiswa(siswa: Siswa)

    @Query("DELETE FROM siswa")
    suspend fun deleteAllSiswa()

    // Absensi Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsensi(absensi: Absensi)

    @Query("SELECT * FROM absensi ORDER BY timestamp DESC")
    fun getAllAbsensiRaw(): Flow<List<Absensi>>

    @Query("""
        SELECT 
            a.id AS absensiId,
            s.nis AS studentNis,
            s.nama AS studentNama,
            s.kelas AS studentKelas,
            a.timestamp AS timestamp,
            a.status AS status,
            a.metode AS metode,
            a.catatan AS catatan
        FROM absensi a
        INNER JOIN siswa s ON a.siswaNis = s.nis
        ORDER BY a.timestamp DESC
    """)
    fun getAbsensiDenganSiswa(): Flow<List<AbsensiDenganSiswa>>

    @Query("DELETE FROM absensi")
    suspend fun deleteAllAbsensi()

    @Query("DELETE FROM absensi WHERE id = :id")
    suspend fun deleteAbsensiById(id: Int)
}
