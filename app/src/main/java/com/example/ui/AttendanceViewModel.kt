package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Absensi
import com.example.data.AbsensiDenganSiswa
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.data.Siswa
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface ScanStatus {
    object Idle : ScanStatus
    object Scanning : ScanStatus
    data class Success(val siswa: Siswa, val status: String, val jam: String, val metode: String) : ScanStatus
    data class Error(val message: String) : ScanStatus
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(database.studentDao())

    // Jam Masuk config (Standard 07:00 AM)
    private val _jamMasukTarget = MutableStateFlow("07:00")
    val jamMasukTarget = _jamMasukTarget.asStateFlow()

    // Screen navigation state
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen = _currentScreen.asStateFlow()

    // Siswa list and Absensi logs
    val allSiswa = repository.allSiswa.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allAbsensi = repository.allAbsensi.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtering inputs
    private val _siswaSearchQuery = MutableStateFlow("")
    val siswaSearchQuery = _siswaSearchQuery.asStateFlow()

    private val _classFilter = MutableStateFlow("Semua")
    val classFilter = _classFilter.asStateFlow()

    val filteredSiswa = combine(allSiswa, _siswaSearchQuery, _classFilter) { list, query, cls ->
        list.filter { siswa ->
            val matchQuery = siswa.nama.contains(query, ignoreCase = true) || siswa.nis.contains(query)
            val matchClass = cls == "Semua" || siswa.kelas == cls
            matchQuery && matchClass
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Scanning State
    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    val scanStatus = _scanStatus.asStateFlow()

    // Camera Live Detection Statuses
    private val _faceInView = MutableStateFlow(false)
    val faceInView = _faceInView.asStateFlow()

    private val _faceMetrics = MutableStateFlow(Pair(0f, 0f)) // width, height
    val faceMetrics = _faceMetrics.asStateFlow()

    init {
        viewModelScope.launch {
            repository.populateSampleDataIfNeeded()
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
        resetScanState()
    }

    fun setSiswaSearchQuery(query: String) {
        _siswaSearchQuery.value = query
    }

    fun setClassFilter(cls: String) {
        _classFilter.value = cls
    }

    fun updateJamMasukTarget(time: String) {
        if (time.matches(Regex("\\d{2}:\\d{2}"))) {
            _jamMasukTarget.value = time
        }
    }

    fun resetScanState() {
        _scanStatus.value = ScanStatus.Idle
        _faceInView.value = false
        _faceMetrics.value = Pair(0f, 0f)
    }

    fun onFaceFoundInCamera(present: Boolean, width: Float, height: Float) {
        _faceInView.value = present
        _faceMetrics.value = Pair(width, height)
    }

    // Process a student scan (Called either by physical CameraX decodes or virtual simulation)
    fun processAttendanceScan(nis: String, metode: String) {
        if (_scanStatus.value is ScanStatus.Success) return // Prevent duplicate double scans instantly

        viewModelScope.launch {
            _scanStatus.value = ScanStatus.Scanning
            delay(1200) // Beautiful scanning progress delay for visual immersion

            val siswaFound = repository.getSiswaByNis(nis)
            if (siswaFound == null) {
                _scanStatus.value = ScanStatus.Error("NIS '$nis' Tidak Terdaftar di Madrasah!")
                delay(3000)
                _scanStatus.value = ScanStatus.Idle
                return@launch
            }

            if (metode == "Scan Wajah" && !siswaFound.faceFeaturesRegistered) {
                _scanStatus.value = ScanStatus.Error("Wajah ${siswaFound.nama} belum terdaftar!")
                delay(3500)
                _scanStatus.value = ScanStatus.Idle
                return@launch
            }

            // Calculate "Hadir" or "Terlambat" based on Target Jam Masuk
            val now = Calendar.getInstance()
            val hourNow = now.get(Calendar.HOUR_OF_DAY)
            val minuteNow = now.get(Calendar.MINUTE)

            val parsedTarget = _jamMasukTarget.value.split(":")
            val targetHour = parsedTarget.getOrNull(0)?.toIntOrNull() ?: 7
            val targetMin = parsedTarget.getOrNull(1)?.toIntOrNull() ?: 0

            val isLate = (hourNow > targetHour) || (hourNow == targetHour && minuteNow > targetMin)
            val status = if (isLate) "Terlambat" else "Hadir"

            val jamStr = String.format("%02d:%02d", hourNow, minuteNow)

            val absensi = Absensi(
                siswaNis = nis,
                status = status,
                metode = metode,
                catatan = if (isLate) "Datang pukul $jamStr" else "Tepat waktu"
            )

            // Save to database
            repository.insertAbsensi(absensi)

            _scanStatus.value = ScanStatus.Success(
                siswa = siswaFound,
                status = status,
                jam = jamStr,
                metode = metode
            )

            // Reset scanner to idle automatically after 4 seconds
            delay(4000)
            _scanStatus.value = ScanStatus.Idle
        }
    }

    // Register high quality profile configurations
    fun addSiswa(nis: String, nama: String, kelas: String, registerFace: Boolean) {
        viewModelScope.launch {
            val newSiswa = Siswa(
                nis = nis.trim(),
                nama = nama.trim(),
                kelas = kelas,
                faceFeaturesRegistered = registerFace
            )
            repository.insertSiswa(newSiswa)
        }
    }

    fun toggleSiswaFaceRegister(siswa: Siswa) {
        viewModelScope.launch {
            val updated = siswa.copy(faceFeaturesRegistered = !siswa.faceFeaturesRegistered)
            repository.updateSiswa(updated)
        }
    }

    fun deleteSiswa(siswa: Siswa) {
        viewModelScope.launch {
            repository.deleteSiswa(siswa)
        }
    }

    fun deleteAbsensi(id: Int) {
        viewModelScope.launch {
            repository.deleteAbsensiById(id)
        }
    }

    fun resetAllMadrasahData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.populateSampleDataIfNeeded()
        }
    }
}

// ViewModel Factory
class AttendanceViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
