@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.AbsensiDenganSiswa
import com.example.data.Siswa
import com.example.ui.scanner.FaceDetectionAnalyzer
import com.example.ui.scanner.QRCodeAnalyzer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun AppNavigation(viewModel: AttendanceViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val transition = updateTransition(targetState = currentScreen, label = "ScreenTransition")
        
        transition.AnimatedContent(
            transitionSpec = {
                if (targetState == "scanner") {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
                } else {
                    fadeIn(animationSpec = twin(220)) togetherWith fadeOut(animationSpec = twin(220))
                }
            }
        ) { screen ->
            when (screen) {
                "dashboard" -> DashboardScreen(viewModel)
                "scanner" -> ScannerScreen(viewModel)
                "students" -> StudentsScreen(viewModel)
                "history" -> HistoryScreen(viewModel)
            }
        }
    }
}

// Global Animation Specs
fun <T> twin(duration: Int = 300): TweenSpec<T> = tween(durationMillis = duration, easing = FastOutSlowInEasing)

@Composable
fun LiveClockWidget() {
    var timeState by remember { mutableStateOf("") }
    var dateState by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val calendar = Calendar.getInstance()
                val localeId = Locale("id", "ID")
                
                val timeFormat = SimpleDateFormat("HH:mm:ss", localeId)
                val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", localeId)
                
                timeState = timeFormat.format(calendar.time)
                dateState = dateFormat.format(calendar.time)
            }
        }, 0, 1000)

        onDispose {
            timer.cancel()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clock_widget"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dateState.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeState,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MTS Darussalam Utama",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: AttendanceViewModel) {
    val allSiswa by viewModel.allSiswa.collectAsState()
    val allAbsensi by viewModel.allAbsensi.collectAsState()
    val jamMasuk by viewModel.jamMasukTarget.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showJamConfigDialog by remember { mutableStateOf(false) }

    // Compute stats for today
    val todayNisesVisited = remember(allAbsensi) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        allAbsensi.filter { it.timestamp >= startOfToday }
    }

    val totalSiswaCount = allSiswa.size
    val hadirCount = todayNisesVisited.filter { it.status == "Hadir" }.distinctBy { it.studentNis }.size
    val terlambatCount = todayNisesVisited.filter { it.status == "Terlambat" }.distinctBy { it.studentNis }.size
    val belumAbsenCount = (totalSiswaCount - (hadirCount + terlambatCount)).coerceAtLeast(0)

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Absen Madrasah",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Aplikasi Presensi Barcode & Scan Wajah",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showJamConfigDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Pengaturan Jam")
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Mulai Ulang Database")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            CustomBottomNavigation(currentScreen = "dashboard", onTabSelected = { screen ->
                viewModel.navigateTo(screen)
            })
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LiveClockWidget()
            }

            // Interactive Scan Gate Banner
            item {
                Text(
                    text = "MENU PINDAI KELAS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // QR Code/Barcode Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("scan_barcode_card")
                            .clickable {
                                viewModel.processAttendanceScan("", "dummy") // Prefetch simulation resets
                                viewModel.navigateTo("scanner")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Scan Barcode",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Gunakan Kartu NIS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    // Face Scanner Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("scan_wajah_card")
                            .clickable {
                                viewModel.navigateTo("scanner")
                                // We switch behavior within scanner UI
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Scan Wajah",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Biometrik Siswa",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }
            }

            // Stats Cards section
            item {
                Text(
                    text = "REKAPITULASI PRESENSI HARI INI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(
                        listOf(
                            Triple("Total Siswa", "$totalSiswaCount", Icons.Default.People),
                            Triple("Siswa Hadir", "$hadirCount", Icons.Default.CheckCircle),
                            Triple("Terlambat", "$terlambatCount", Icons.Default.Schedule),
                            Triple("Belum Absen", "$belumAbsenCount", Icons.Default.Block)
                        )
                    ) { stat ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = stat.third,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stat.first,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                    Text(
                                        text = stat.second,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Logs Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AKTIVITAS SCAN TERAKHIR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )
                    TextButton(onClick = { viewModel.navigateTo("history") }) {
                        Text("Lihat Semua")
                    }
                }
            }

            val recentLogs = allAbsensi.take(4)
            if (recentLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AssignmentLate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum ada scan hari ini.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            )
                            Text(
                                text = "Gunakan tombol scan di atas untuk memulai.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            } else {
                items(recentLogs) { log ->
                    AbsensiRowItem(log = log, onDelete = {
                        viewModel.deleteAbsensi(log.absensiId)
                    })
                }
            }
            
            // Padding bottom
            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Settings Jam Masuk Dialog
    if (showJamConfigDialog) {
        var jamText by remember { mutableStateOf(jamMasuk) }
        AlertDialog(
            onDismissRequest = { showJamConfigDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateJamMasukTarget(jamText)
                        showJamConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJamConfigDialog = false }) { Text("Batal") }
            },
            title = { Text("Konfigurasi Jam Masuk", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Siswa yang absen melebihi jam ini akan otomatis dicatat sebagai 'Terlambat'.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = jamText,
                        onValueChange = { jamText = it },
                        label = { Text("Jam Masuk (Format HH:mm)") },
                        placeholder = { Text("e.g. 07:00") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // Database Reset Confirm Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllMadrasahData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Mulai Ulang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
            },
            title = { Text("Mulai Ulang Database?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Tindakan ini akan mengosongkan riwayat presensi saat ini dan memuat ulang data siswa contoh.", style = MaterialTheme.typography.bodyMedium)
            }
        )
    }
}

@Composable
fun AbsensiRowItem(log: AbsensiDenganSiswa, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showMenu = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Presence Icon based on status
            val containerColor = if (log.status == "Hadir") {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            }
            val tintColor = if (log.status == "Hadir") {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
            val iconType = if (log.status == "Hadir") Icons.Default.Check else Icons.Default.Warning

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconType,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.studentNama,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NIS ${log.studentNis} • ${log.studentKelas}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Method Indicator
                    Icon(
                        imageVector = if (log.metode == "Scan Wajah") Icons.Default.Face else Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${log.metode} (${log.formattedTime})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Right Tag badge
            Box(
                modifier = Modifier
                    .background(
                        color = containerColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = log.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = tintColor
                    )
                )
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Hapus Log Presensi") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(viewModel: AttendanceViewModel) {
    val scanStatus by viewModel.scanStatus.collectAsState()
    val allSiswa by viewModel.allSiswa.collectAsState()
    val faceInView by viewModel.faceInView.collectAsState()
    val faceMetrics by viewModel.faceMetrics.collectAsState()

    var scanMode by remember { mutableStateOf("Barcode") } // Barcode or Face
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    // Virtual Simulator student selection state
    var selectedSimStudent by remember { mutableStateOf<Siswa?>(null) }
    var simExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize Simulator Selected Student with first student
    LaunchedEffect(allSiswa) {
        if (selectedSimStudent == null && allSiswa.isNotEmpty()) {
            selectedSimStudent = allSiswa.first()
        }
    }

    // Standard CameraX configurations
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Button(
                            onClick = { 
                                scanMode = "Barcode"
                                viewModel.resetScanState()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scanMode == "Barcode") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (scanMode == "Barcode") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Barcode")
                        }
                        Button(
                            onClick = { 
                                scanMode = "Wajah"
                                viewModel.resetScanState()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scanMode == "Wajah") MaterialTheme.colorScheme.tertiary else Color.Transparent,
                                contentColor = if (scanMode == "Wajah") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wajah")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo("dashboard") }, modifier = Modifier.testTag("scanner_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        bottomBar = {
            // SIMULATOR PANEL FOR EMULATOR TESTING
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulator_panel"),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔬 SIMULATOR SCANNER MADRASAH (UNTUK EMULATOR)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dropdown selection
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { simExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("student_select_sim_button")
                            ) {
                                Text(
                                    text = selectedSimStudent?.let { "${it.nama} (${it.kelas})" } ?: "Pilih Siswa...",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = simExpanded, onDismissRequest = { simExpanded = false }) {
                                allSiswa.forEach { siswa ->
                                    DropdownMenuItem(
                                        text = { Text("${siswa.nama} (${siswa.kelas})") },
                                        onClick = {
                                            selectedSimStudent = siswa
                                            simExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (siswa.faceFeaturesRegistered) Icons.Default.Face else Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (siswa.faceFeaturesRegistered) MaterialTheme.colorScheme.primary else Color.Gray
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Fire Simulate Click
                        Button(
                            onClick = {
                                selectedSimStudent?.let { student ->
                                    val simulatedMethod = if (scanMode == "Barcode") "Barcode" else "Scan Wajah"
                                    viewModel.processAttendanceScan(student.nis, simulatedMethod)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scanMode == "Barcode") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            ),
                            enabled = selectedSimStudent != null && scanStatus == ScanStatus.Idle,
                            modifier = Modifier.testTag("simulate_fire_button")
                        ) {
                            Text("Simulasikan")
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            // Camera setup checking permissions first
            if (cameraPermissionState.status.isGranted) {
                // Initialize Preview of CameraX
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }

                            // Analysis engine
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            if (scanMode == "Barcode") {
                                imageAnalysis.setAnalyzer(cameraExecutor, QRCodeAnalyzer { barcodeVal ->
                                    viewModel.processAttendanceScan(barcodeVal, "Barcode")
                                })
                            } else {
                                imageAnalysis.setAnalyzer(cameraExecutor, FaceDetectionAnalyzer { count, fw, fh ->
                                    viewModel.onFaceFoundInCamera(count > 0, fw, fh)
                                    // Automate face check-in in real mode if matches (we simulate the click if face stays)
                                })
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Warning Permission Request required Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Izin Kamera Dibutuhkan",
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aplikasi ini memmerlukan akses kamera belakang untuk memindai kartu barcode atau wajah biometrik siswa Madrasah secara langsung.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Izinkan Akses Kamera")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "(Atau gunakan panel simulasi di bawah untuk menguji aplikasi)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f))
                    )
                }
            }

            // TRANSLUCENT OVERLAY INTERFACES based on chosen Mode!
            if (scanMode == "Barcode") {
                BarcodeScannerOverlay()
            } else {
                FaceScannerOverlay(faceInView = faceInView, metrics = faceMetrics)
            }

            // RENDER ACTIVE SCANNING FULL SCREEN STATES
            when (val currentStatus = scanStatus) {
                is ScanStatus.Scanning -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.82f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = if (scanMode == "Barcode") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (scanMode == "Barcode") "MENGANALISA KARTU NIS..." else "MEMINDAI GEOMETRIS WAJAH...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "Menghubungkan ke Room Database Madrasah",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                is ScanStatus.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "ALARM SCAN GAGAL!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentStatus.message,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                TextButton(
                                    onClick = { viewModel.resetScanState() }
                                ) {
                                    Text("Coba Lagi", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                is ScanStatus.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val isLate = currentStatus.status == "Terlambat"
                        val containerCardColor = if (isLate) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                        val bannerColor = if (isLate) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        val onBannerText = Color.White

                        Card(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth()
                                .testTag("scan_success_card"),
                            colors = CardDefaults.cardColors(containerColor = containerCardColor),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Full colored banner at top
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bannerColor)
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(Color.White.copy(alpha = 0.25f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "PRESENSI BERHASIL",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                letterSpacing = 1.2.sp
                                            )
                                        )
                                        Text(
                                            text = currentStatus.metode.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = currentStatus.siswa.nama,
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "NIS ${currentStatus.siswa.nis} • Kelas ${currentStatus.siswa.kelas}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Time badge
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("WAKTU MASUK", style = MaterialTheme.typography.labelSmall)
                                                Text(currentStatus.jam, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }

                                        // Status badge
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("KETERANGAN", style = MaterialTheme.typography.labelSmall)
                                                Text(
                                                    text = currentStatus.status.uppercase(),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isLate) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { viewModel.resetScanState() },
                                        colors = ButtonDefaults.buttonColors(containerColor = bannerColor),
                                        modifier = Modifier.fillMaxWidth().testTag("scan_success_confirm")
                                    ) {
                                        Text("Selesai")
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun BarcodeScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserLine")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Laser"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Darken outside square viewport
        val viewPortSize = width * 0.65f
        val left = (width - viewPortSize) / 2
        val top = (height - viewPortSize) / 2.5f
        val right = left + viewPortSize
        val bottom = top + viewPortSize

        // Drawing hollow scrim out bounds
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        // Clear view area
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(viewPortSize, viewPortSize),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        // Draw bracket lines surrounding clear viewport
        val bracketLen = 28.dp.toPx()
        val strokeW = 4.dp.toPx()
        val bracketColor = Color(0xFF38B279) // Emerald Mint accent

        // Top Left
        drawLine(bracketColor, Offset(left - 2, top), Offset(left + bracketLen, top), strokeW)
        drawLine(bracketColor, Offset(left, top - 2), Offset(left, top + bracketLen), strokeW)

        // Top Right
        drawLine(bracketColor, Offset(right + 2, top), Offset(right - bracketLen, top), strokeW)
        drawLine(bracketColor, Offset(right, top - 2), Offset(right, top + bracketLen), strokeW)

        // Bottom Left
        drawLine(bracketColor, Offset(left - 2, bottom), Offset(left + bracketLen, bottom), strokeW)
        drawLine(bracketColor, Offset(left, bottom + 2), Offset(left, bottom - bracketLen), strokeW)

        // Bottom Right
        drawLine(bracketColor, Offset(right + 2, bottom), Offset(right - bracketLen, bottom), strokeW)
        drawLine(bracketColor, Offset(right, bottom + 2), Offset(right, bottom - bracketLen), strokeW)

        // Animated red laser checking line
        val laserY = top + (viewPortSize * laserOffset)
        drawLine(
            color = Color.Red,
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 2.5f.dp.toPx()
        )
    }
}

@Composable
fun FaceScannerOverlay(faceInView: Boolean, metrics: Pair<Float, Float>) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarHUD")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Oval shape viewport
        val viewPortW = width * 0.65f
        val viewPortH = viewPortW * 1.3f
        val left = (width - viewPortW) / 2
        val top = (height - viewPortH) / 2.8f
        
        // Draw Scrim
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        // Oval target
        drawOval(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(viewPortW, viewPortH),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        // Draw animated ring outline surrounding oval
        val strokeW = 3.dp.toPx()
        val ringColor = if (faceInView) Color(0xFFE5C158) else Color(0xFF4FD195).copy(alpha = 0.7f)
        drawOval(
            color = ringColor,
            topLeft = Offset(left - (strokeW * pulseScale), top - (strokeW * pulseScale)),
            size = Size(viewPortW + (strokeW * pulseScale * 2), viewPortH + (strokeW * pulseScale * 2)),
            style = Stroke(
                width = strokeW,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
            )
        )
    }

    // Display bio metrics subtitle overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 120.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (faceInView) "WAJAH TERDETEKSI: KUNCI MANDRI..." else "SEJAJARKAN WAJAH DI AREA OVAL",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = if (faceInView) Color(0xFFE5C158) else Color(0xFF38B279)
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Biometric SDK 3.0 • Madrasah Database Link",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
fun StudentsScreen(viewModel: AttendanceViewModel) {
    val filteredSiswa by viewModel.filteredSiswa.collectAsState()
    val classes = listOf("Semua", "MTS 7-A", "MTS 7-B", "MTS 8-A", "MTS 8-B", "MTS 9-A", "MTS 9-B")
    val selectedClass by viewModel.classFilter.collectAsState()
    val searchQuery by viewModel.siswaSearchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSiswaQrcodeShow by remember { mutableStateOf<Siswa?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text("Direktori Siswa", fontWeight = FontWeight.Black)
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            CustomBottomNavigation(currentScreen = "students", onTabSelected = { screen ->
                viewModel.navigateTo(screen)
            })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_siswa_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Siswa")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input textfield
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSiswaSearchQuery(it) },
                placeholder = { Text("Cari Siswa Berdasarkan Nama atau NIS...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("siswa_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Class Tabs selector
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                items(classes) { cls ->
                    val isSelected = selectedClass == cls
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setClassFilter(cls) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cls,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Students Content directory List
            if (filteredSiswa.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Siswa Ditemukan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                        Text(
                            text = "Gunakan tombol (+) untuk menambahkan siswa baru",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSiswa) { siswa ->
                        SiswaCardItem(
                            siswa = siswa,
                            onToggleFace = { viewModel.toggleSiswaFaceRegister(siswa) },
                            onDelete = { viewModel.deleteSiswa(siswa) },
                            onShowQr = { selectedSiswaQrcodeShow = siswa }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Add Student Dialog
    if (showAddDialog) {
        var nisVal by remember { mutableStateOf("") }
        var namaVal by remember { mutableStateOf("") }
        var currentSelectedClass by remember { mutableStateOf(classes.getOrElse(1) { "MTS 7-A" }) }
        var cbRegisterFace by remember { mutableStateOf(true) }
        var classExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (nisVal.isNotBlank() && namaVal.isNotBlank()) {
                            viewModel.addSiswa(nisVal, namaVal, currentSelectedClass, cbRegisterFace)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("submit_siswa_button")
                ) {
                    Text("Daftarkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
            },
            title = { Text("Registrasi Siswa Madrasah", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nisVal,
                        onValueChange = { nisVal = it },
                        label = { Text("NIS (Nomor Induk Siswa)") },
                        placeholder = { Text("e.g. 223591") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("nis_input")
                    )

                    OutlinedTextField(
                        value = namaVal,
                        onValueChange = { namaVal = it },
                        label = { Text("Nama Lengkap Siswa") },
                        placeholder = { Text("e.g. Faisal Yusuf") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("nama_input")
                    )

                    // Class Dropdown Select
                    Box {
                        OutlinedButton(
                            onClick = { classExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kelas: $currentSelectedClass")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                            classes.filter { it != "Semua" }.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text(cls) },
                                    onClick = {
                                        currentSelectedClass = cls
                                        classExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Register Biometric toggle checkbox option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { cbRegisterFace = !cbRegisterFace }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = cbRegisterFace, onCheckedChange = { cbRegisterFace = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Daftarkan Biometrik Wajah", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Izinkan siswa menggunakan Scan Wajah selain Barcode.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        )
    }

    // QR Barcode Card Visual Show Dialog
    selectedSiswaQrcodeShow?.let { siswa ->
        AlertDialog(
            onDismissRequest = { selectedSiswaQrcodeShow = null },
            confirmButton = {
                Button(onClick = { selectedSiswaQrcodeShow = null }) { Text("Tutup") }
            },
            title = {
                Text(
                    text = "KARTU NIS DIGITAL",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MADRASAH TSANAWIYAH DARUSSALAM",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(siswa.nama, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("Kelas: ${siswa.kelas} • NIS: ${siswa.nis}", style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Simulated Code-Scanner Graphic Card
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .border(width = 2.dp, color = Color.Black, shape = RoundedCornerShape(12.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Simulated barcode bar vectors
                            Canvas(modifier = Modifier.width(180.dp).height(80.dp)) {
                                val barWidthTotal = size.width
                                val totalBars = 36
                                val step = barWidthTotal / totalBars
                                val random = Random(siswa.nis.hashCode().toLong())

                                var currentX = 0f
                                while (currentX < barWidthTotal) {
                                    val isBlack = random.nextBoolean()
                                    val thickness = if (random.nextBoolean()) step * 1.5f else step * 0.5f
                                    
                                    if (isBlack) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(currentX, 0f),
                                            size = Size(thickness, size.height)
                                        )
                                    }
                                    currentX += thickness + (step * 0.4f)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "*${siswa.nis}*",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Simpan barcode ini untuk diposisikan di depan kamera scan saat presensi manual.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    }
}

@Composable
fun SiswaCardItem(siswa: Siswa, onToggleFace: () -> Unit, onDelete: () -> Unit, onShowQr: () -> Unit) {
    var showMoreDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("siswa_card_${siswa.nis}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initial Avatar in circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = siswa.nama.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = siswa.nama,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "NIS ${siswa.nis} • Kelas ${siswa.kelas}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                // Biometrics Badge indicator clickable
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleFace() }
                        .background(
                            color = if (siswa.faceFeaturesRegistered) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.LightGray.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (siswa.faceFeaturesRegistered) Icons.Default.Face else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (siswa.faceFeaturesRegistered) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (siswa.faceFeaturesRegistered) "Biometrik Ambil: Ya" else "Biometrik Ambil: Belum",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (siswa.faceFeaturesRegistered) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    )
                }
            }

            // Quick scan icon link
            IconButton(onClick = onShowQr) {
                Icon(imageVector = Icons.Default.QrCode, contentDescription = "Tampilkan Kartu Barcode", tint = MaterialTheme.colorScheme.primary)
            }

            Box {
                IconButton(onClick = { showMoreDropdown = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = showMoreDropdown, onDismissRequest = { showMoreDropdown = false }) {
                    DropdownMenuItem(
                        text = { Text("Registrasi/Hapus Wajah") },
                        onClick = {
                            onToggleFace()
                            showMoreDropdown = false
                        },
                        leadingIcon = { Icon(Icons.Default.Face, contentDescription = null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Hapus Siswa", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            showMoreDropdown = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: AttendanceViewModel) {
    val allAbsensi by viewModel.allAbsensi.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Riwayat Absensi", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            CustomBottomNavigation(currentScreen = "history", onTabSelected = { screen ->
                viewModel.navigateTo(screen)
            })
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Stats chart visualization using elegant material canvas boxes
            val totalCount = allAbsensi.size
            if (totalCount > 0) {
                val groupStatus = allAbsensi.groupBy { it.status }
                val hadirCount = groupStatus["Hadir"]?.size ?: 0
                val terlambatCount = groupStatus["Terlambat"]?.size ?: 0
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PERSENTASE DISTRIBUSI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Drawn segment line
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            val percentHadir = if (totalCount > 0) (hadirCount.toFloat() / totalCount) else 0f
                            if (percentHadir > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(percentHadir.coerceAtLeast(0.01f))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (percentHadir > 0.15) {
                                        Text("${(percentHadir * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            val percentLate = if (totalCount > 0) (terlambatCount.toFloat() / totalCount) else 0f
                            if (percentLate > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(percentLate.coerceAtLeast(0.01f))
                                        .background(MaterialTheme.colorScheme.tertiary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (percentLate > 0.15) {
                                        Text("${(percentLate * 100).toInt()}%", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tepat Waktu ($hadirCount)", style = MaterialTheme.typography.labelMedium)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Terlambat ($terlambatCount)", style = MaterialTheme.typography.labelMedium)
                            }
                            
                            Text("Total scan: $totalCount logs", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // RIwayat list of logs
            if (allAbsensi.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Hubungi Admin: Belum Ada Log",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                        Text(
                            text = "Presensi yang berhasil diproses akan muncul di sini.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allAbsensi) { log ->
                        AbsensiRowItem(log = log, onDelete = {
                            viewModel.deleteAbsensi(log.absensiId)
                        })
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(currentScreen: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == "dashboard" || currentScreen == "scanner",
            onClick = { onTabSelected("dashboard") },
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Beranda") },
            label = { Text("Beranda") }
        )
        NavigationBarItem(
            selected = currentScreen == "students",
            onClick = { onTabSelected("students") },
            icon = { Icon(imageVector = Icons.Default.People, contentDescription = "Siswa") },
            label = { Text("Siswa") }
        )
        NavigationBarItem(
            selected = currentScreen == "history",
            onClick = { onTabSelected("history") },
            icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Riwayat") },
            label = { Text("Riwayat") }
        )
    }
}
