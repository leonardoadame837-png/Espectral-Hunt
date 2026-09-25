package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.hardware.RealDeviceHardwareManager
import com.example.data.model.DeviceLocationInfo
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.sqrt

private data class BleEvidence(
    val address: String,
    val name: String?,
    val rssi: Int,
    val seenCount: Int,
    val lastSeenMs: Long
)

@Composable
fun PhoneEvidenceScreen(
    deviceLocation: DeviceLocationInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val hardwareManager = remember { RealDeviceHardwareManager(context) }

    var running by remember { mutableStateOf(true) }
    var magneticAvailable by remember { mutableStateOf(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) }
    var accelerometerAvailable by remember { mutableStateOf(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) }
    var magneticUt by remember { mutableFloatStateOf(0f) }
    var baselineUt by remember { mutableFloatStateOf(0f) }
    var magneticSamples by remember { mutableIntStateOf(0) }
    var tapCandidates by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }
    var lastAccelMagnitude by remember { mutableFloatStateOf(0f) }
    var wifiCount by remember { mutableIntStateOf(0) }
    val bleDevices = remember { mutableStateListOf<BleEvidence>() }
    var bleAvailable by remember { mutableStateOf(false) }
    var permissionRequestNeeded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionRequestNeeded = results.values.none { it }
    }

    fun refreshWifiEvidence() {
        wifiCount = hardwareManager.getNearbyWifiObservations(deviceLocation).size
    }

    DisposableEffect(running) {
        val magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magneticAvailable = magnetic != null
        accelerometerAvailable = accelerometer != null

        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

            override fun onSensorChanged(event: SensorEvent) {
                if (!running) return
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    val x = event.values.getOrElse(0) { 0f }
                    val y = event.values.getOrElse(1) { 0f }
                    val z = event.values.getOrElse(2) { 0f }
                    val magnitude = sqrt(x * x + y * y + z * z)
                    magneticUt = magnitude
                    magneticSamples += 1
                    baselineUt = if (magneticSamples <= 20) {
                        if (baselineUt == 0f) magnitude else baselineUt * 0.9f + magnitude * 0.1f
                    } else baselineUt * 0.98f + magnitude * 0.02f
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values.getOrElse(0) { 0f }
                    val y = event.values.getOrElse(1) { 0f }
                    val z = event.values.getOrElse(2) { 0f }
                    val magnitude = sqrt(x * x + y * y + z * z)
                    if (lastAccelMagnitude > 0f &&
                        kotlin.math.abs(magnitude - lastAccelMagnitude) > 2.5f &&
                        System.currentTimeMillis() - lastTapMs > 400L
                    ) {
                        tapCandidates += 1
                        lastTapMs = System.currentTimeMillis()
                    }
                    lastAccelMagnitude = magnitude
                }
            }
        }

        if (running) {
            magnetic?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    DisposableEffect(running) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = bluetoothManager?.adapter
        val scanner: BluetoothLeScanner? = adapter?.bluetoothLeScanner
        bleAvailable = scanner != null

        val hasScanPermission = Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

        if (!running || scanner == null || !hasScanPermission) {
            if (running && scanner != null && !hasScanPermission) permissionRequestNeeded = true
            onDispose { }
        } else {
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val address = result.device?.address ?: return
                    val existing = bleDevices.indexOfFirst { it.address == address }
                    val item = BleEvidence(
                        address = address,
                        name = result.device?.name?.takeIf { !it.isNullOrBlank() },
                        rssi = result.rssi,
                        seenCount = if (existing >= 0) bleDevices[existing].seenCount + 1 else 1,
                        lastSeenMs = System.currentTimeMillis()
                    )
                    if (existing >= 0) bleDevices[existing] = item else bleDevices.add(item)
                }
            }
            try {
                scanner.startScan(null, ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
            } catch (_: SecurityException) {
                permissionRequestNeeded = true
            }
            onDispose { try { scanner.stopScan(callback) } catch (_: Exception) {} }
        }
    }

    LaunchedEffect(Unit) { refreshWifiEvidence() }
    LaunchedEffect(running) {
        if (running) while (running) {
            refreshWifiEvidence()
            kotlinx.coroutines.delay(5000)
        }
    }

    if (permissionRequestNeeded) {
        LaunchedEffect(Unit) {
            val permissions = buildList {
                if (Build.VERSION.SDK_INT >= 31) {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }.toTypedArray()
            if (permissions.isNotEmpty()) permissionLauncher.launch(permissions)
            permissionRequestNeeded = false
        }
    }

    val magneticDelta = if (baselineUt > 0f) kotlin.math.abs(magneticUt - baselineUt) else 0f
    val magneticAnomaly = baselineUt > 0f && magneticDelta >= 25f
    val repeatBle = bleDevices.count { it.seenCount >= 3 }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(CyberBg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("PHONE EVIDENCE LAB", style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonCyan))
                    Text("Measured phone telemetry → deterministic evidence → analyst interpretation",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMuted))
                }
                Text(if (running) "LIVE" else "PAUSED", color = if (running) NeonGreen else NeonAmber,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) {
                Text(if (running) "PAUSE COLLECTION" else "START COLLECTION")
            }
        }
        item {
            EvidenceCard("MAGNETIC FIELD / WIRE INDICATION", Icons.Default.Sensors) {
                MetricRow("Field", if (magneticAvailable) "%.1f µT".format(Locale.US, magneticUt) else "Sensor unavailable")
                MetricRow("Baseline", if (magneticAvailable) "%.1f µT".format(Locale.US, baselineUt) else "—")
                MetricRow("Delta", if (magneticAvailable) "%.1f µT".format(Locale.US, magneticDelta) else "—")
                StatusRow(if (magneticAnomaly) "MAGNETIC ANOMALY — INVESTIGATE" else "NO CURRENT ANOMALY",
                    if (magneticAnomaly) NeonAmber else NeonGreen)
                Text("A magnetic anomaly is not proof of a wire. Nearby metal, speakers, power equipment and other objects can also change the field.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp))
            }
        }
        item {
            EvidenceCard("TAP / MOTION EVENTS", Icons.Default.Memory) {
                MetricRow("Accelerometer", if (accelerometerAvailable) "AVAILABLE" else "UNAVAILABLE")
                MetricRow("Candidate events", tapCandidates.toString())
                MetricRow("Last event", if (lastTapMs == 0L) "—" else "${System.currentTimeMillis() - lastTapMs} ms ago")
                Text("These are motion/tap candidates from the phone accelerometer. They are not proof of an external acoustic tap or deliberate activity.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp))
            }
        }
        item {
            EvidenceCard("NEARBY RADIO OBSERVATIONS / TRACKER REVIEW", Icons.Default.Bluetooth) {
                MetricRow("Wi-Fi observations", wifiCount.toString())
                MetricRow("BLE observations", bleDevices.size.toString())
                MetricRow("Repeated BLE identifiers", repeatBle.toString())
                Text("Repeated Wi-Fi/BLE identifiers are observations only. They do not prove a tracker, owner, identity, direction or physical location.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp))
            }
        }
        items(bleDevices.takeLast(12).reversed()) { device ->
            Card(colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                modifier = Modifier.fillMaxWidth().border(1.dp, CyberBorder, RoundedCornerShape(8.dp))) {
                Column(Modifier.padding(10.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(device.name ?: "BLE device / anonymous identifier", color = TextPrimary,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("${device.rssi} dBm", color = NeonCyan, fontFamily = FontFamily.Monospace)
                    }
                    Text("${device.address}  •  seen ${device.seenCount}×", color = TextMuted,
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    if (device.seenCount >= 3) Text("REPEATED OBSERVATION — REVIEW AS HYPOTHESIS ONLY",
                        color = NeonAmber, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            EvidenceCard("FIELD TRACK", Icons.Default.MyLocation) {
                MetricRow("Location", if (deviceLocation.isRealHardwareFix) "REAL DEVICE FIX" else "NO VERIFIED FIX")
                MetricRow("Coordinates", if (deviceLocation.isRealHardwareFix)
                    "${String.format(Locale.US, "%.6f, %.6f", deviceLocation.latitude, deviceLocation.longitude)}" else "—")
                MetricRow("Accuracy", if (deviceLocation.isRealHardwareFix)
                    "${String.format(Locale.US, "%.1f m", deviceLocation.accuracyMeters)}" else "—")
                Text("The track records where the phone was when evidence was collected. It does not create a transmitter track or emitter location.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp))
            }
        }
        item {
            EvidenceCard("EVIDENCE ANALYST", Icons.Default.Radar) {
                val interpretation = when {
                    magneticAnomaly && repeatBle > 0 ->
                        "Multiple observation types are active. Preserve raw measurements and review the magnetic anomaly and repeated BLE observations independently; neither establishes a wire or tracker."
                    magneticAnomaly ->
                        "A magnetic-field deviation is present relative to the local phone baseline. Collect repeat passes from different orientations before treating it as meaningful."
                    repeatBle > 0 ->
                        "Repeated BLE identifiers are present. Repetition supports an observation pattern only; identify the device independently before calling it a tracker."
                    wifiCount > 0 ->
                        "Android is reporting nearby Wi-Fi observations. These are radio telemetry records, not broadband RF spectrum measurements."
                    else ->
                        "No current phone evidence supports a specific wire, tap or tracker conclusion. Continue collecting measured data."
                }
                Text(interpretation, style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace, color = TextPrimary, fontSize = 11.sp))
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { (magneticSamples.coerceAtMost(100) / 100f) },
                    Modifier.fillMaxWidth().height(4.dp), color = NeonCyan, trackColor = CyberSurfaceElevated)
                Text("AI handoff: raw evidence stays authoritative; interpretations must cite evidence IDs and remain hypotheses when device capability cannot establish the claim.",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
            }
        }
        item {
            Text("PHONE LIMITATION: ordinary Android Wi-Fi/Bluetooth radios do not expose arbitrary broadband RF/IQ spectrum. Use an authorized external measurement source when actual microwave/RF spectrum evidence is required.",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = NeonRed, fontSize = 9.sp))
        }
    }
}

@Composable
private fun EvidenceCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, CyberBorder, RoundedCornerShape(10.dp))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = NeonCyan)
                Spacer(Modifier.width(8.dp))
                Text(title, color = NeonCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            content()
        }
    }
}
@Composable private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Text(value, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    }
}
@Composable private fun StatusRow(text: String, color: Color) {
    Text(text, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
}
