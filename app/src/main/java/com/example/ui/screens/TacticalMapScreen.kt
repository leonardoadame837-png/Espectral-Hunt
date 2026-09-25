package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.FindingLocationSource
import com.example.data.model.MapTargetItem
import com.example.data.model.RfSignalInfo
import com.example.data.model.TacticalEnvironmentMode
import com.example.data.model.VulnerabilityFinding
import com.example.ui.components.TacticalRadarMapView
import com.example.ui.components.TacticalRealMapView
import com.example.ui.components.launchExternalMap
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TacticalMapScreen(
    signals: List<RfSignalInfo>,
    findings: List<VulnerabilityFinding>,
    selectedTarget: MapTargetItem?,
    onSelectTarget: (MapTargetItem) -> Unit,
    selectedSignal: RfSignalInfo?,
    onSelectSignal: (RfSignalInfo) -> Unit,
    environmentMode: TacticalEnvironmentMode = TacticalEnvironmentMode.REAL_DEVICE,
    deviceLocation: DeviceLocationInfo? = null,
    onToggleEnvironmentMode: (TacticalEnvironmentMode) -> Unit = {},
    onRefreshLocation: () -> Unit = {},
    onSearchAddress: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 0 = Real Geographic Map, 1 = 3-Node Triangulation Radar
    var mapModeIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            onToggleEnvironmentMode(TacticalEnvironmentMode.REAL_DEVICE)
            onRefreshLocation()
            Toast.makeText(context, "Hardware GPS Telemetry Synchronized!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission denied. Running fallback positioning.", Toast.LENGTH_SHORT).show()
        }
    }

    // Build unified target items
    val allTargets = remember(findings, signals) {
        val list = mutableListOf<MapTargetItem>()
        findings.filter { it.locationSource == FindingLocationSource.VERIFIED_OBSERVATION }
            .forEach { list.add(MapTargetItem.FindingTarget(it)) }
        signals.forEach { list.add(MapTargetItem.SignalTarget(it)) }
        list
    }

    val isRealMode = environmentMode == TacticalEnvironmentMode.REAL_DEVICE

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Environment Telemetry & Geolocation Origin Banner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurface)
                    .border(1.dp, if (isRealMode) NeonGreen.copy(alpha = 0.6f) else NeonAmber.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isRealMode) Icons.Default.MyLocation else Icons.Default.Public,
                            contentDescription = "Environment Mode",
                            tint = if (isRealMode) NeonGreen else NeonAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRealMode) "REAL DEVICE GPS TELEMETRY" else "SF TESTBED CYBER-RANGE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isRealMode) NeonGreen else NeonAmber,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isRealMode) Color(0xFF00291C) else Color(0xFF332000))
                            .border(1.dp, if (isRealMode) NeonGreen else NeonAmber, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isRealMode) "REAL HARDWARE" else "SAN FRANCISCO SIM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (isRealMode) NeonGreen else NeonAmber,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isRealMode) {
                        "MI UBICACIÓN es solo el origen del dispositivo. Los hallazgos solo aparecen en el mapa cuando tienen una ubicación de observación verificada."
                    } else {
                        "Los datos del testbed son simulados y no representan hallazgos físicos ni tu dirección."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                val curLat = if (isRealMode) deviceLocation?.latitude ?: 37.7780 else 37.7780
                val curLon = if (isRealMode) deviceLocation?.longitude ?: -122.4100 else -122.4100
                Text(
                    text = "ORIGIN: ${String.format(Locale.US, "%.5f, %.5f", curLat, curLon)} | ELEV: ${deviceLocation?.altitudeMeters?.toInt() ?: 45}m | FIX: ${if (isRealMode) (deviceLocation?.provider?.uppercase() ?: "GPS") else "SIMULATED"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRealMode) Color(0xFF003D2A) else NeonGreen.copy(alpha = 0.2f),
                            contentColor = NeonGreen
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(1.dp, NeonGreen, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync GPS",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRealMode) "REFRESH REAL GPS" else "USE REAL GPS FIX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (isRealMode) {
                                onToggleEnvironmentMode(TacticalEnvironmentMode.SAN_FRANCISCO_RANGE)
                                Toast.makeText(context, "Switched to San Francisco Cyber-Range testbed", Toast.LENGTH_SHORT).show()
                            } else {
                                onToggleEnvironmentMode(TacticalEnvironmentMode.REAL_DEVICE)
                                onRefreshLocation()
                                Toast.makeText(context, "Switched to Real Device mode", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isRealMode) NeonAmber else NeonCyan
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(1.dp, if (isRealMode) NeonAmber.copy(alpha = 0.7f) else NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = if (isRealMode) Icons.Default.Public else Icons.Default.MyLocation,
                            contentDescription = "Toggle Mode",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRealMode) "SWITCH TO SF TESTBED" else "SWITCH TO REAL GPS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
        // Mode Selector: Real Map vs Triangulation Radar
        item {
            TabRow(
                selectedTabIndex = mapModeIndex,
                containerColor = CyberSurface,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[mapModeIndex]),
                        color = NeonCyan,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = mapModeIndex == 0,
                    onClick = { mapModeIndex = 0 },
                    modifier = Modifier.testTag("tab_real_map"),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Real Map",
                                modifier = Modifier.size(16.dp),
                                tint = if (mapModeIndex == 0) NeonCyan else TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REAL GEOGRAPHIC MAP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (mapModeIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (mapModeIndex == 0) NeonCyan else TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                )

                Tab(
                    selected = mapModeIndex == 1,
                    onClick = { mapModeIndex = 1 },
                    modifier = Modifier.testTag("tab_radar_map"),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Radar Map",
                                modifier = Modifier.size(16.dp),
                                tint = if (mapModeIndex == 1) NeonCyan else TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3-NODE RADAR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (mapModeIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (mapModeIndex == 1) NeonCyan else TextMuted,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                )
            }
        }

        // Active Map View Display
        item {
            if (mapModeIndex == 0) {
                TacticalRealMapView(
                    findings = findings,
                    signals = signals,
                    selectedTarget = selectedTarget,
                    onSelectTarget = { target ->
                        onSelectTarget(target)
                        if (target is MapTargetItem.SignalTarget) {
                            onSelectSignal(target.signal)
                        }
                    },
                    deviceLocation = deviceLocation,
                    environmentMode = environmentMode,
                    onSearchAddress = onSearchAddress
                )
            } else {
                TacticalRadarMapView(
                    signals = signals,
                    selectedSignal = selectedSignal,
                    onSelectSignal = { signal ->
                        onSelectSignal(signal)
                        onSelectTarget(MapTargetItem.SignalTarget(signal))
                    }
                )
            }
        }

        // Target Quick-Select Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "LOCATED TARGETS (TAP TO FOCUS ON MAP):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 8.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allTargets.forEach { item ->
                        val isSelected = selectedTarget?.id == item.id
                        val itemColor = if (item.isThreat) NeonRed else (if (item is MapTargetItem.FindingTarget) NeonAmber else NeonGreen)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) itemColor.copy(alpha = 0.25f) else CyberSurfaceElevated)
                                .border(1.dp, if (isSelected) itemColor else CyberBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    onSelectTarget(item)
                                    if (item is MapTargetItem.SignalTarget) {
                                        onSelectSignal(item.signal)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(itemColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) TextPrimary else TextSecondary,
                                            fontSize = 9.sp
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.locationName} (${String.format(Locale.US, "%.3f, %.3f", item.latitude, item.longitude)})",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted,
                                            fontSize = 7.5.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Triangulation Sensor Nodes Status
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Sensor Nodes",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTIVE SENSOR RECEIVER ANCHORS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Text(
                        text = "3 / 3 SYNCHRONIZED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val nodes = listOf(
                        Triple("NODE ALPHA (α)", "37.7749°N, 122.4194°W", "Civic Center Base"),
                        Triple("NODE BRAVO (β)", "37.7892°N, 122.4014°W", "Financial Dist Mast"),
                        Triple("NODE CHARLIE (γ)", "37.7650°N, 122.4180°W", "Mission District Relay")
                    )

                    nodes.forEach { (title, coords, station) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceElevated)
                                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 9.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = coords,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonGreen,
                                        fontSize = 7.5.sp
                                    )
                                )
                                Text(
                                    text = station,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted,
                                        fontSize = 7.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Targeted Signal/Finding Geolocation Details
        selectedTarget?.let { target ->
            val isFinding = target is MapTargetItem.FindingTarget
            val themeColor = if (target.isThreat) NeonRed else (if (isFinding) NeonAmber else NeonGreen)

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurface)
                        .border(1.dp, themeColor, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (target.isThreat) Icons.Default.Warning else Icons.Default.LocationOn,
                                contentDescription = "Target Status",
                                tint = themeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "UBICACIÓN DEL HALLAZGO (OBSERVACIÓN VERIFICADA)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    color = themeColor,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // Copy Coordinates
                        IconButton(
                            onClick = {
                                val coords = "${target.latitude}, ${target.longitude}"
                                clipboardManager.setText(AnnotatedString(coords))
                                Toast.makeText(context, "GPS Coordinates copied: $coords", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy GPS Coordinates",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = target.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tactical Coords Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberSurfaceElevated)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("LATITUDE (WGS84)", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                Text(
                                    String.format(Locale.US, "%.6f° N", target.latitude),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberSurfaceElevated)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("LONGITUDE (WGS84)", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                Text(
                                    String.format(Locale.US, "%.6f° W", -target.longitude),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberSurfaceElevated)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("ELEVATION MSL", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                Text(
                                    "${target.elevationMeters} METERS",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Physical Street Address & Facility Name
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF040813))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "TACTICAL SITE FACILITY:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber,
                                    fontSize = 8.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = target.locationName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = target.locationAddress,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                    fontSize = 9.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dedicated Signal Analyst / Threat Intelligence Dossier Block
                    if (target is MapTargetItem.SignalTarget) {
                        val sig = target.signal
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sig.isRogue) Color(0xFF1E070B) else Color(0xFF040A18))
                                .border(1.dp, if (sig.isRogue) NeonRed else NeonCyan, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SIGNAL ANALYST ASSESSMENT:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sig.isRogue) NeonRed else NeonCyan,
                                            fontSize = 8.5.sp
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (sig.isRogue) Color(0xFF380E15) else Color(0xFF0F3B25))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = sig.threatRating.ifBlank { if (sig.isRogue) "ROGUE" else "AUTH" },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sig.isRogue) NeonRed else NeonGreen,
                                                fontSize = 7.5.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "DESCRIPTION: ${sig.description}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        lineHeight = 13.5.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "EVALUATION: ${sig.analystAssessment}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = if (sig.isRogue) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                                        fontSize = 9.5.sp,
                                        lineHeight = 13.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "ACTION: ${sig.recommendedAction} | Purity: ${sig.spectralPurityPercent}% | Mod: ${sig.modulationType} | Security: ${sig.encryptionState}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonAmber,
                                        fontSize = 8.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    } else if (target is MapTargetItem.FindingTarget) {
                        val finding = target.finding
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E070B))
                                .border(1.dp, NeonRed, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "VULNERABILITY DESCRIPTION & IMPACT:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonRed,
                                        fontSize = 8.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = finding.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        lineHeight = 13.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "REMEDIATION: ${finding.remediation}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonAmber,
                                        fontSize = 8.5.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Google Maps / GPS Navigation Intent Button
                    Button(
                        onClick = {
                            launchExternalMap(context, target.latitude, target.longitude, target.locationName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_target_maps_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in Maps",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LAUNCH GOOGLE MAPS / GPS NAVIGATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
