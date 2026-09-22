package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.MapTargetItem
import com.example.data.model.RfSignalInfo
import com.example.data.model.TacticalEnvironmentMode
import com.example.data.model.VulnerabilityFinding
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tactical Real Geographic Vector Map.
 * Projects real latitude & longitude coordinates onto an interactive tactical map with
 * San Francisco waterfront contours, arterial streets, tech hubs, and geolocation pins.
 */
@Composable
fun TacticalRealMapView(
    findings: List<VulnerabilityFinding>,
    signals: List<RfSignalInfo>,
    selectedTarget: MapTargetItem?,
    onSelectTarget: (MapTargetItem) -> Unit,
    deviceLocation: DeviceLocationInfo? = null,
    environmentMode: TacticalEnvironmentMode = TacticalEnvironmentMode.REAL_DEVICE,
    onSearchAddress: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var addressSearchInput by remember { mutableStateOf("") }

    // Zoom & Pan state
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Map filter: 0 = ALL, 1 = FINDINGS, 2 = RF SIGNALS, 3 = ROGUE/CRITICAL
    var selectedFilterIndex by remember { mutableStateOf(0) }

    // Address overlay controls
    var showAddressesOnMap by remember { mutableStateOf(true) }
    var onlyDangerousOnMap by remember { mutableStateOf(false) }

    // Pulse animation for critical threats
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val threatPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "threatPulse"
    )

    // Build unified target list
    val allTargets = remember(findings, signals) {
        val list = mutableListOf<MapTargetItem>()
        findings.forEach { list.add(MapTargetItem.FindingTarget(it)) }
        signals.forEach { list.add(MapTargetItem.SignalTarget(it)) }
        list
    }

    val filteredTargets = remember(allTargets, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> allTargets.filterIsInstance<MapTargetItem.FindingTarget>()
            2 -> allTargets.filterIsInstance<MapTargetItem.SignalTarget>()
            3 -> allTargets.filter { it.isThreat }
            else -> allTargets
        }
    }

    val isRealDevice = environmentMode == TacticalEnvironmentMode.REAL_DEVICE && deviceLocation != null && deviceLocation.isRealHardwareFix

    val baseLat = remember(environmentMode, deviceLocation, filteredTargets) {
        if (isRealDevice) {
            deviceLocation!!.latitude
        } else if (filteredTargets.isNotEmpty()) {
            filteredTargets.map { it.latitude }.average()
        } else {
            37.7780
        }
    }

    val baseLon = remember(environmentMode, deviceLocation, filteredTargets) {
        if (isRealDevice) {
            deviceLocation!!.longitude
        } else if (filteredTargets.isNotEmpty()) {
            filteredTargets.map { it.longitude }.average()
        } else {
            -122.4100
        }
    }

    val isSanFranciscoRange = environmentMode == TacticalEnvironmentMode.SAN_FRANCISCO_RANGE ||
        (baseLat in 37.70..37.85 && baseLon in -122.52..-122.36)

    val latSpan = 0.055 / zoomLevel
    val lonSpan = 0.065 / zoomLevel

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Map Header with Real Geolocation Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF00303D))
                        .border(1.dp, NeonCyan, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Real Map",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isRealDevice) "REAL HARDWARE GEOGRAPHIC MAP" else "TACTICAL CYBER-RANGE MAP",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = if (isRealDevice) {
                            "${deviceLocation?.locationName?.uppercase() ?: "HOST DEVICE"} (${String.format(Locale.US, "%.4f, %.4f", baseLat, baseLon)})"
                        } else {
                            "SAN FRANCISCO BAY AREA TESTBED (WGS84 GPS PROJECTION)"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontSize = 8.5.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isRealDevice) Color(0xFF002B1D) else Color(0xFF332000))
                    .border(1.dp, if (isRealDevice) NeonGreen else NeonAmber, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isRealDevice) NeonGreen else NeonAmber)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRealDevice) "LIVE GPS LOCK" else "SF TESTBED SIM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = if (isRealDevice) NeonGreen else NeonAmber,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Street Address Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = addressSearchInput,
                onValueChange = { addressSearchInput = it },
                placeholder = {
                    Text(
                        "Search street address or Lat,Lon...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("map_address_search_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = {
                    if (addressSearchInput.isNotBlank()) {
                        onSearchAddress(addressSearchInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("map_search_address_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Address",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Target Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                "ALL (${allTargets.size})",
                "FINDINGS (${findings.size})",
                "RF SIGNALS (${signals.size})",
                "ROGUE/CRIT (${allTargets.count { it.isThreat }})"
            )

            filters.forEachIndexed { index, title ->
                val isSelected = selectedFilterIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                        .border(1.dp, if (isSelected) NeonCyan else CyberBorder, RoundedCornerShape(6.dp))
                        .clickable { selectedFilterIndex = index }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) NeonCyan else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Address Display Controls on Map
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showAddressesOnMap) NeonCyan.copy(alpha = 0.18f) else CyberSurfaceElevated)
                    .border(1.dp, if (showAddressesOnMap) NeonCyan else CyberBorder, RoundedCornerShape(6.dp))
                    .clickable { showAddressesOnMap = !showAddressesOnMap }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showAddressesOnMap) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Addresses on Map",
                        tint = if (showAddressesOnMap) NeonCyan else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showAddressesOnMap) "STREET ADDRESSES: ON MAP" else "STREET ADDRESSES: OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (showAddressesOnMap) NeonCyan else TextMuted,
                            fontSize = 8.5.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (onlyDangerousOnMap) NeonRed.copy(alpha = 0.22f) else CyberSurfaceElevated)
                    .border(1.dp, if (onlyDangerousOnMap) NeonRed else CyberBorder, RoundedCornerShape(6.dp))
                    .clickable { onlyDangerousOnMap = !onlyDangerousOnMap }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Dangerous Only Filter",
                        tint = if (onlyDangerousOnMap) NeonRed else NeonAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (onlyDangerousOnMap) "DANGER FOCUS: ACTIVE" else "SHOW ALL PINS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (onlyDangerousOnMap) NeonRed else TextSecondary,
                            fontSize = 8.5.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Map Canvas with Zoom Overlay Controls & Dynamic Street Address Badges
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF030A14))
                .border(1.dp, Color(0xFF16324A), RoundedCornerShape(8.dp))
        ) {
            val mapWidthPx = constraints.maxWidth.toFloat()
            val mapHeightPx = constraints.maxHeight.toFloat()
            val density = LocalDensity.current

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .pointerInput(filteredTargets, zoomLevel, panOffsetX, panOffsetY) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()

                            // Find nearest pin to tap
                            val closest = filteredTargets.minByOrNull { target ->
                                val normX = ((target.longitude - (baseLon - lonSpan / 2)) / lonSpan).toFloat()
                                val normY = (1.0f - ((target.latitude - (baseLat - latSpan / 2)) / latSpan)).toFloat()

                                val px = normX * w + panOffsetX
                                val py = normY * h + panOffsetY

                                val dx = tapOffset.x - px
                                val dy = tapOffset.y - py
                                dx * dx + dy * dy
                            }

                            if (closest != null) {
                                onSelectTarget(closest)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                fun geoToPixel(lat: Double, lon: Double): Offset {
                    val normX = ((lon - (baseLon - lonSpan / 2)) / lonSpan).toFloat()
                    val normY = (1.0f - ((lat - (baseLat - latSpan / 2)) / latSpan)).toFloat()
                    return Offset(normX * w + panOffsetX, normY * h + panOffsetY)
                }

                // 1. Geography / Range Grid
                if (isSanFranciscoRange) {
                    // Water Body Background (SF Bay & Pacific Basin)
                    val bayPath = Path().apply {
                        val p1 = geoToPixel(37.820, -122.390)
                        val p2 = geoToPixel(37.805, -122.400)
                        val p3 = geoToPixel(37.795, -122.392)
                        val p4 = geoToPixel(37.780, -122.385)
                        val p5 = geoToPixel(37.755, -122.380)

                        moveTo(w, 0f)
                        lineTo(p1.x, p1.y)
                        cubicTo(p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)
                        lineTo(p5.x, p5.y)
                        lineTo(w, h)
                        close()
                    }

                    drawPath(
                        path = bayPath,
                        color = Color(0xFF041829)
                    )

                    // Coastline Shoreline Accent Glow
                    drawPath(
                        path = bayPath,
                        color = NeonCyan.copy(alpha = 0.35f),
                        style = Stroke(width = 1.5f)
                    )

                    // 2. Tactical Street Grid & Arterials
                    // Market Street (Main Diagonal Arterial)
                    val mktStart = geoToPixel(37.760, -122.440)
                    val mktEnd = geoToPixel(37.795, -122.395)
                    drawLine(
                        color = Color(0x3500E5FF),
                        start = mktStart,
                        end = mktEnd,
                        strokeWidth = 2.5f
                    )

                    // Mission Street Corridor
                    val misStart = geoToPixel(37.750, -122.420)
                    val misEnd = geoToPixel(37.790, -122.400)
                    drawLine(
                        color = Color(0x2500E5FF),
                        start = misStart,
                        end = misEnd,
                        strokeWidth = 1.8f
                    )

                    // Embarcadero Waterfront Highway
                    val emb1 = geoToPixel(37.810, -122.415)
                    val emb2 = geoToPixel(37.794, -122.393)
                    val emb3 = geoToPixel(37.778, -122.387)
                    drawLine(
                        color = Color(0x3500FF9D),
                        start = emb1,
                        end = emb2,
                        strokeWidth = 2.0f
                    )
                    drawLine(
                        color = Color(0x3500FF9D),
                        start = emb2,
                        end = emb3,
                        strokeWidth = 2.0f
                    )

                    // Bay Bridge Approach (I-80)
                    val brgStart = geoToPixel(37.785, -122.398)
                    val brgEnd = geoToPixel(37.800, -122.370)
                    drawLine(
                        color = Color(0x40FF9100),
                        start = brgStart,
                        end = brgEnd,
                        strokeWidth = 2.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                    )
                } else {
                    // Tactical Concentric Distance Range Rings centered on Real Device Anchor
                    val centerDevicePos = geoToPixel(baseLat, baseLon)
                    val ringRadii = listOf(45f, 95f, 155f, 225f)
                    ringRadii.forEach { r ->
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.22f),
                            radius = r,
                            center = centerDevicePos,
                            style = Stroke(
                                width = 1.2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        )
                    }

                    // Tactical Crosshairs
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.30f),
                        start = Offset(centerDevicePos.x, 0f),
                        end = Offset(centerDevicePos.x, h),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.30f),
                        start = Offset(0f, centerDevicePos.y),
                        end = Offset(w, centerDevicePos.y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                // Secondary Grid Lines
                val latSteps = 7
                for (i in 0..latSteps) {
                    val lat = (baseLat - latSpan / 2) + (latSpan * (i.toDouble() / latSteps))
                    val pStart = geoToPixel(lat, baseLon - lonSpan / 2)
                    val pEnd = geoToPixel(lat, baseLon + lonSpan / 2)
                    drawLine(
                        color = Color(0x1000E5FF),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = 1f
                    )
                }

                val lonSteps = 7
                for (j in 0..lonSteps) {
                    val lon = (baseLon - lonSpan / 2) + (lonSpan * (j.toDouble() / lonSteps))
                    val pStart = geoToPixel(baseLat - latSpan / 2, lon)
                    val pEnd = geoToPixel(baseLat + latSpan / 2, lon)
                    drawLine(
                        color = Color(0x1000E5FF),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = 1f
                    )
                }

                // 3. Anchor Base Station / Host Device GPS Anchor ("YOU ARE HERE / MI UBICACIÓN")
                val anchorPos = if (isSanFranciscoRange) {
                    geoToPixel(37.7749, -122.4194)
                } else {
                    geoToPixel(baseLat, baseLon)
                }
                drawCircle(
                    color = NeonGreen.copy(alpha = 0.20f),
                    radius = 32f * (if (isRealDevice) threatPulse.coerceIn(1.0f, 2.0f) else 1f),
                    center = anchorPos
                )
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.45f),
                    radius = 18f,
                    center = anchorPos,
                    style = Stroke(width = 1.8f)
                )
                drawCircle(
                    color = NeonGreen,
                    radius = 8f,
                    center = anchorPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = anchorPos
                )

                // 4. Render All Target Pins (Findings & RF Signals)
                filteredTargets.forEach { target ->
                    val pos = geoToPixel(target.latitude, target.longitude)
                    val isSelected = target.id == selectedTarget?.id
                    val isFinding = target is MapTargetItem.FindingTarget

                    val pinColor = when {
                        target.isThreat -> NeonRed
                        isFinding -> NeonAmber
                        else -> NeonGreen
                    }

                    // Pulsing Ring for Critical / Rogue Threats
                    if (target.isThreat || isSelected) {
                        drawCircle(
                            color = pinColor.copy(alpha = (1.0f - (threatPulse - 1f) / 1.2f).coerceIn(0f, 0.7f)),
                            radius = (if (isSelected) 18f else 12f) * threatPulse,
                            center = pos,
                            style = Stroke(width = 1.5f)
                        )

                        // Vector Connection to Base Station if selected
                        if (isSelected) {
                            drawLine(
                                color = pinColor.copy(alpha = 0.6f),
                                start = anchorPos,
                                end = pos,
                                strokeWidth = 1.4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                            )
                        }
                    }

                    // Pin Marker Shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.6f),
                        radius = if (isSelected) 9f else 6.5f,
                        center = Offset(pos.x + 1f, pos.y + 1f)
                    )

                    // Pin Marker Body
                    if (isFinding) {
                        // Diamond for Security Vulnerabilities
                        val path = Path().apply {
                            val r = if (isSelected) 9f else 6.5f
                            moveTo(pos.x, pos.y - r)
                            lineTo(pos.x + r, pos.y)
                            lineTo(pos.x, pos.y + r)
                            lineTo(pos.x - r, pos.y)
                            close()
                        }
                        drawPath(path = path, color = pinColor)
                        drawPath(path = path, color = Color.White, style = Stroke(width = 1.2f))
                    } else {
                        // Circle for RF Signals
                        drawCircle(
                            color = pinColor,
                            radius = if (isSelected) 8f else 5.5f,
                            center = pos
                        )
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 8f else 5.5f,
                            center = pos,
                            style = Stroke(width = 1.2f)
                        )
                    }
                }

                // 5. Tactical Center Crosshair
                val centerOffset = Offset(w / 2f, h / 2f)
                drawLine(
                    color = Color(0x3000E5FF),
                    start = Offset(centerOffset.x - 12f, centerOffset.y),
                    end = Offset(centerOffset.x + 12f, centerOffset.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0x3000E5FF),
                    start = Offset(centerOffset.x, centerOffset.y - 12f),
                    end = Offset(centerOffset.x, centerOffset.y + 12f),
                    strokeWidth = 1f
                )
            }

            // Persistent Location Overlay on Map (Top Left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xF0071322))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (deviceLocation?.isRealHardwareFix == true) NeonGreen else NeonAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MI UBICACIÓN: ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            )
                            Text(
                                text = String.format(Locale.US, "%.5f, %.5f", baseLat, baseLon),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                        val fullAddrText = deviceLocation?.locationAddress?.takeIf { it.isNotBlank() }
                            ?: deviceLocation?.locationName?.takeIf { it.isNotBlank() }
                            ?: if (isSanFranciscoRange) "Market St & 4th St, San Francisco, CA" else "Sensor Host Anchor Fix"
                        Text(
                            text = "STREET: $fullAddrText",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )
                    }
                }
            }

            // Compass Rose Overlay (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC040B16))
                    .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Compass North",
                        tint = NeonRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NORTH 000°",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Map Zoom Controls & Recenter (Bottom Right)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xDD040B16))
                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .clickable { zoomLevel = (zoomLevel * 1.25f).coerceAtMost(3.0f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xDD040B16))
                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .clickable { zoomLevel = (zoomLevel / 1.25f).coerceAtLeast(0.7f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xDD040B16))
                        .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .clickable {
                            zoomLevel = 1.0f
                            panOffsetX = 0f
                            panOffsetY = 0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Recenter",
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Dynamic On-Map Physical Street Address Callout Badges
            if (showAddressesOnMap) {
                filteredTargets.forEach { target ->
                    val isSelected = target.id == selectedTarget?.id
                    val isThreat = target.isThreat

                    // Only show callout if enabled: threats always have priority!
                    if (isThreat || isSelected || !onlyDangerousOnMap) {
                        val normX = ((target.longitude - (baseLon - lonSpan / 2)) / lonSpan).toFloat()
                        val normY = (1.0f - ((target.latitude - (baseLat - latSpan / 2)) / latSpan)).toFloat()
                        val px = normX * mapWidthPx + panOffsetX
                        val py = normY * mapHeightPx + panOffsetY

                        // Check visibility within map viewable bounds
                        if (px in -40f..(mapWidthPx + 40f) && py in -20f..(mapHeightPx + 40f)) {
                            // Calculate clamped position so it doesn't clip off-screen
                            val clampX = (px - 50f).coerceIn(8f, (mapWidthPx - 160f).coerceAtLeast(8f))
                            val clampY = (py - 38f).coerceIn(38f, (mapHeightPx - 44f).coerceAtLeast(38f))

                            val xDp = with(density) { clampX.toDp() }
                            val yDp = with(density) { clampY.toDp() }

                            val badgeBorder = if (isThreat) NeonRed else (if (isSelected) NeonCyan else Color(0xFF163C55))
                            val badgeBg = if (isThreat) Color(0xF2220408) else (if (isSelected) Color(0xF0051B2F) else Color(0xEB040F1D))

                            Box(
                                modifier = Modifier
                                    .offset(x = xDp, y = yDp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeBg)
                                    .border(if (isThreat || isSelected) 1.5.dp else 1.dp, badgeBorder, RoundedCornerShape(4.dp))
                                    .clickable { onSelectTarget(target) }
                                    .padding(horizontal = 5.dp, vertical = 3.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isThreat) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Threat",
                                                tint = NeonRed,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "DANGER",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Black,
                                                    color = NeonRed,
                                                    fontSize = 7.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                        }
                                        Text(
                                            text = target.title.take(18),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isThreat) NeonRed else (if (isSelected) NeonCyan else TextPrimary),
                                                fontSize = 8.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "📍 ${target.locationAddress}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isThreat) Color(0xFFFFD54F) else NeonGreen,
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

        // Dedicated Dangerous Findings & Rogue Frequencies Physical Address Section
        val dangerousTargets = remember(filteredTargets) { filteredTargets.filter { it.isThreat } }
        if (dangerousTargets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF180306))
                    .border(1.5.dp, NeonRed, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Threat Detected",
                            tint = NeonRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DANGEROUS TARGETS PHYSICAL ADDRESSES (${dangerousTargets.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = NeonRed,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonRed.copy(alpha = 0.25f))
                            .border(1.dp, NeonRed, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CRITICAL THREATS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NeonRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                dangerousTargets.forEach { threat ->
                    val isSelected = threat.id == selectedTarget?.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF330911) else Color(0xFF24060B))
                            .border(1.dp, if (isSelected) NeonRed else Color(0xFF5E141E), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(NeonRed)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = threat.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            fontSize = 7.5.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = threat.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("${threat.locationAddress} (${threat.latitude}, ${threat.longitude})"))
                                    Toast.makeText(context, "Address copied: ${threat.locationAddress}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Address",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Street Address Highlight
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Address",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STREET ADDRESS: ${threat.locationAddress}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD54F),
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Text(
                            text = "GPS: ${String.format(Locale.US, "%.5f°N, %.5f°W", threat.latitude, -threat.longitude)} | Site: ${threat.locationName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 8.sp
                            )
                        )

                        if (threat is MapTargetItem.SignalTarget) {
                            val sig = threat.signal
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "DESC: ${sig.description}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                ),
                                maxLines = 2
                            )
                            Text(
                                text = "ANALYST: ${sig.analystAssessment}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 8.5.sp,
                                    lineHeight = 11.sp
                                ),
                                maxLines = 2
                            )
                        } else if (threat is MapTargetItem.FindingTarget) {
                            val finding = threat.finding
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "IMPACT: ${finding.description}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                ),
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onSelectTarget(threat) },
                                modifier = Modifier.weight(1f).height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NeonCyan else Color(0xFF2E1A22),
                                    contentColor = if (isSelected) Color.Black else TextPrimary
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isSelected) "PIN FOCUSED" else "FOCUS ON MAP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.5.sp
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    launchExternalMap(context, threat.latitude, threat.longitude, threat.locationAddress)
                                },
                                modifier = Modifier.weight(1f).height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Navigate",
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "NAVIGATE GPS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Target Detail Card with Real Location (Ubicación) and Navigation Intent
        selectedTarget?.let { target ->
            val isFinding = target is MapTargetItem.FindingTarget
            val themeColor = if (target.isThreat) NeonRed else (if (isFinding) NeonAmber else NeonGreen)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, themeColor, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeColor.copy(alpha = 0.2f))
                                .border(1.dp, themeColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = target.badgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor,
                                    fontSize = 8.5.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFinding) "VULNERABILITY DEFECT" else "RF SIGNAL EMITTER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 8.5.sp
                            )
                        )
                    }

                    // Copy GPS Coordinates
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

                Spacer(modifier = Modifier.height(4.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

                // Real Geolocation (Ubicación) Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF030814))
                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Real Location",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "REAL GEOLOCATION (UBICACIÓN):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    fontSize = 9.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

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

                        Spacer(modifier = Modifier.height(6.dp))

                        // GPS Coordinates Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberSurfaceElevated)
                                    .padding(4.dp)
                            ) {
                                Column {
                                    Text("LATITUDE", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                    Text(
                                        String.format(Locale.US, "%.6f° N", target.latitude),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberSurfaceElevated)
                                    .padding(4.dp)
                            ) {
                                Column {
                                    Text("LONGITUDE", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                    Text(
                                        String.format(Locale.US, "%.6f° W", -target.longitude),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(0.8f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberSurfaceElevated)
                                    .padding(4.dp)
                            ) {
                                Column {
                                    Text("ELEVATION", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                    Text(
                                        "${target.elevationMeters}m MSL",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Launch External Navigation Intent (Google Maps / OpenStreetMap / GPS)
                Button(
                    onClick = {
                        launchExternalMap(context, target.latitude, target.longitude, target.locationName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_in_maps_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open in External Maps",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OPEN IN REAL GOOGLE MAPS / GPS",
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
}

/**
 * Fires an Android Intent to open the real coordinates on Google Maps or any installed GPS app,
 * with web fallback if no maps app is present.
 */
fun launchExternalMap(context: Context, latitude: Double, longitude: Double, label: String) {
    val encodedLabel = Uri.encode(label)
    val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
