package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ThreatAlertEntity
import com.example.data.local.ThreatSignatureEntity
import com.example.ui.theme.CyberBg
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top-level HUD Card for the RF Microwave Threat Detection System & Sentinel IDS.
 */
@Composable
fun ThreatDetectionHudCard(
    activeAlertsCount: Int,
    totalSignaturesCount: Int,
    isScanning: Boolean,
    isSentinelActive: Boolean,
    onRunScan: () -> Unit,
    onToggleSentinel: () -> Unit,
    onOpenSignaturesDb: () -> Unit,
    onViewAllAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "threatPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "threatPulseAlpha"
    )

    val threatLevelColor = when {
        activeAlertsCount >= 3 -> NeonRed
        activeAlertsCount > 0 -> NeonAmber
        else -> NeonGreen
    }

    val threatLevelLabel = when {
        activeAlertsCount >= 3 -> "DEFCON 1 / CRITICAL"
        activeAlertsCount > 0 -> "ELEVATED THREAT"
        else -> "AIRSPACE SECURE"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (activeAlertsCount > 0) threatLevelColor.copy(alpha = pulseAlpha) else CyberBorder,
                RoundedCornerShape(12.dp)
            )
            .testTag("threat_detection_hud_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(threatLevelColor.copy(alpha = 0.15f))
                            .border(1.dp, threatLevelColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeAlertsCount > 0) Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = "Threat IDS",
                            tint = threatLevelColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RF THREAT DETECTION SYSTEM",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSentinelActive) NeonGreen else TextMuted)
                            )
                        }
                        Text(
                            text = "Microwave & RF Sentinel IDS • Pattern Matching Engine",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Sentinel Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("sentinel_toggle_row")
                ) {
                    Text(
                        text = if (isSentinelActive) "SENTINEL ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSentinelActive) NeonGreen else TextMuted,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isSentinelActive,
                        onCheckedChange = { onToggleSentinel() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurfaceElevated
                        ),
                        modifier = Modifier.size(width = 38.dp, height = 24.dp)
                    )
                }
            }

            HorizontalDivider(color = CyberBorder.copy(alpha = 0.6f))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Threat Level Gauge
                Column {
                    Text(
                        text = "SPECTRUM DEFCON",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = threatLevelLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = threatLevelColor,
                            fontSize = 12.sp
                        )
                    )
                }

                // Active Alerts
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ACTIVE ALERTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = "$activeAlertsCount UNRESOLVED",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (activeAlertsCount > 0) NeonRed else NeonGreen,
                            fontSize = 12.sp
                        )
                    )
                }

                // Known Signatures
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SIGNATURES DB",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = "$totalSignaturesCount LOADED",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRunScan,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeAlertsCount > 0) NeonRed else NeonCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("run_threat_scan_button")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SCANNING RF...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Scan",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RUN THREAT SCAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenSignaturesDb,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(38.dp)
                        .testTag("signatures_db_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Signatures",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SIGNATURES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                if (activeAlertsCount > 0) {
                    OutlinedButton(
                        onClick = onViewAllAlerts,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonAmber),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonAmber.copy(alpha = 0.5f))),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(38.dp)
                            .testTag("view_alerts_button")
                    ) {
                        Text(
                            text = "ALERTS ($activeAlertsCount)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Live Alert Banner that pulses when critical or high threat is detected in microwave finder.
 */
@Composable
fun LiveThreatAlertBanner(
    alert: ThreatAlertEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bannerPulse")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBorder"
    )

    val alertColor = when (alert.threatLevel) {
        "CRITICAL" -> NeonRed
        "HIGH" -> NeonAmber
        else -> NeonCyan
    }

    Surface(
        color = Color(0xFF260505),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, alertColor.copy(alpha = pulseBorder), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("live_threat_alert_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(alertColor.copy(alpha = 0.2f))
                        .border(1.dp, alertColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Threat Alert",
                        tint = alertColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${alert.threatLevel} THREAT: ${alert.matchedSignatureName}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = alertColor,
                                fontSize = 12.sp
                            )
                        )
                    }
                    Text(
                        text = "Carrier: ${alert.signalFrequencyMhz} MHz (${alert.signalBandName}) • Dist: ${alert.signalDistanceM.toInt()}m @ ${alert.signalAzimuthDeg.toInt()}°",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(alertColor.copy(alpha = 0.2f))
                    .border(1.dp, alertColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "INVESTIGATE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = alertColor,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

/**
 * Detailed Threat Alert Item Card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThreatAlertCard(
    alert: ThreatAlertEntity,
    onSelect: () -> Unit,
    onTriangulate: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alertColor = when (alert.threatLevel) {
        "CRITICAL" -> NeonRed
        "HIGH" -> NeonAmber
        "MEDIUM" -> NeonCyan
        else -> NeonGreen
    }

    val isResolved = alert.status == "MITIGATED" || alert.status == "DISMISSED"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isResolved) CyberSurface.copy(alpha = 0.6f) else CyberSurface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isResolved) CyberBorder.copy(alpha = 0.4f) else alertColor.copy(alpha = 0.7f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .testTag("threat_alert_card_${alert.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Level + Name + Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(alertColor.copy(alpha = 0.2f))
                            .border(1.dp, alertColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alert.threatLevel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = alertColor,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = alert.detectionType.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (alert.status) {
                                "ACTIVE" -> NeonRed.copy(alpha = 0.15f)
                                "INVESTIGATING" -> NeonAmber.copy(alpha = 0.15f)
                                "MITIGATED" -> NeonGreen.copy(alpha = 0.15f)
                                else -> TextMuted.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when (alert.status) {
                                "ACTIVE" -> NeonRed
                                "INVESTIGATING" -> NeonAmber
                                "MITIGATED" -> NeonGreen
                                else -> TextMuted
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = alert.status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = when (alert.status) {
                                "ACTIVE" -> NeonRed
                                "INVESTIGATING" -> NeonAmber
                                "MITIGATED" -> NeonGreen
                                else -> TextMuted
                            },
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Title
            Text(
                text = alert.matchedSignatureName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            )

            // RF Telemetry Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "FREQ: ${alert.signalFrequencyMhz} MHz",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "PWR: ${alert.signalPowerDbm} dBm",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "DIST: ${alert.signalDistanceM.toInt()}m (${alert.signalAzimuthDeg.toInt()}°)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NeonAmber,
                        fontSize = 11.sp
                    )
                )
            }

            // Location Address
            Text(
                text = "Location: ${alert.locationAddress}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 11.sp
                )
            )

            // Anomaly Indicators Tags
            val indicatorList = alert.anomalyIndicators.split(";").filter { it.isNotBlank() }
            if (indicatorList.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    indicatorList.take(3).forEach { indicator ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberSurfaceElevated)
                                .border(0.5.dp, alertColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = indicator.trim().take(45) + if (indicator.trim().length > 45) "..." else "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onTriangulate,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.5f))),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Map",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "MAP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        )
                    }

                    if (alert.status == "ACTIVE") {
                        OutlinedButton(
                            onClick = { onUpdateStatus("INVESTIGATING") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonAmber),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonAmber.copy(alpha = 0.5f))),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "INVESTIGATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }

                    if (alert.status != "MITIGATED") {
                        OutlinedButton(
                            onClick = { onUpdateStatus("MITIGATED") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonGreen.copy(alpha = 0.5f))),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mitigate",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "MITIGATED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Detailed Modal Dialog for a selected Threat Alert.
 */
@Composable
fun ThreatAlertDetailDialog(
    alert: ThreatAlertEntity,
    onDismiss: () -> Unit,
    onTriangulateOnMap: () -> Unit,
    onMarkStatus: (String) -> Unit,
    onDeleteAlert: () -> Unit
) {
    val alertColor = when (alert.threatLevel) {
        "CRITICAL" -> NeonRed
        "HIGH" -> NeonAmber
        "MEDIUM" -> NeonCyan
        else -> NeonGreen
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = CyberBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(1.5.dp, alertColor, RoundedCornerShape(16.dp))
                .testTag("threat_alert_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(alertColor.copy(alpha = 0.15f))
                                .border(1.dp, alertColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = alertColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = alert.alertId,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Detected: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(alert.timestamp))}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CyberBorder)

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Threat Level & Confidence Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberSurface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "POTENTIAL THREAT LEVEL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted
                                        )
                                    )
                                    Text(
                                        text = alert.threatLevel,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = alertColor
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "DETECTION MODE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted
                                        )
                                    )
                                    Text(
                                        text = alert.detectionType,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "CONFIDENCE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted
                                        )
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", alert.confidencePercent),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Matched Malicious Signature
                    item {
                        Column {
                            Text(
                                text = "MATCHED SIGNATURE / THREAT PATTERN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = alert.matchedSignatureName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    if (alert.matchedSignatureId != null) {
                                        Text(
                                            text = "Signature DB ID: ${alert.matchedSignatureId}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Signal RF Telemetry Breakdown
                    item {
                        Column {
                            Text(
                                text = "CARRIER RF PARAMETERS & GEOLOCATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Center Frequency:",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                        Text(
                                            text = "${alert.signalFrequencyMhz} MHz (${alert.signalBandName})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Modulation & Protocol:",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                        Text(
                                            text = alert.signalProtocol,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Received Power / Distance:",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                        Text(
                                            text = "${alert.signalPowerDbm} dBm • ~${alert.signalDistanceM.toInt()}m (Bearing ${alert.signalAzimuthDeg.toInt()}°)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                color = NeonAmber
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Emitter Site:",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                        Text(
                                            text = alert.locationAddress,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Anomaly Indicators List
                    item {
                        Column {
                            Text(
                                text = "ANOMALY BEHAVIORAL INDICATORS TRIGGERED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonRed
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val anomalies = alert.anomalyIndicators.split(";").filter { it.isNotBlank() }
                                    anomalies.forEachIndexed { index, anomaly ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(
                                                text = "[#${index + 1}] ",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeonRed
                                                )
                                            )
                                            Text(
                                                text = anomaly.trim(),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    color = TextPrimary,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Technical Details & Exploit Scenario
                    item {
                        Column {
                            Text(
                                text = "TECHNICAL ASSESSMENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = alert.technicalSummary,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    ),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Recommended Actionable Countermeasures
                    item {
                        Column {
                            Text(
                                text = "RECOMMENDED COUNTERMEASURES & MITIGATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF002B1D)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Remedy",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = alert.recommendedCountermeasure,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CyberBorder)

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onTriangulateOnMap()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Map",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TRIANGULATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (alert.status != "MITIGATED") {
                            Button(
                                onClick = {
                                    onMarkStatus("MITIGATED")
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Mitigate",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "MARK MITIGATED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onDeleteAlert()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonRed.copy(alpha = 0.5f))),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DELETE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog to inspect and manage the Database of Known Malicious Signal Signatures.
 */
@Composable
fun MaliciousSignaturesDatabaseDialog(
    signatures: List<ThreatSignatureEntity>,
    onToggleEnabled: (String, Boolean) -> Unit,
    onAddCustomSignature: (ThreatSignatureEntity) -> Unit,
    onDeleteSignature: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    val categories = remember(signatures) {
        listOf("ALL") + signatures.map { it.category }.distinct()
    }

    val filteredSignatures = remember(signatures, searchQuery, selectedCategory) {
        signatures.filter { sig ->
            val matchesCategory = selectedCategory == "ALL" || sig.category.equals(selectedCategory, true)
            val matchesSearch = searchQuery.isBlank() ||
                sig.name.contains(searchQuery, true) ||
                sig.id.contains(searchQuery, true) ||
                sig.targetBand.contains(searchQuery, true) ||
                sig.signaturePatternHex.contains(searchQuery, true) ||
                sig.technicalDetails.contains(searchQuery, true)
            matchesCategory && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = CyberBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .border(1.5.dp, CyberBorder, RoundedCornerShape(16.dp))
                .testTag("malicious_signatures_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Signatures",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MALICIOUS RF SIGNATURES DATABASE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "${signatures.size} Known Threat Signatures • Active IDS Heuristics",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search signatures by name, band, hex pattern...",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("signature_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan,
                                containerColor = CyberSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) NeonCyan else CyberBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Signatures List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSignatures, key = { it.id }) { sig ->
                        SignatureItemCard(
                            signature = sig,
                            onToggleEnabled = { isEnabled -> onToggleEnabled(sig.id, isEnabled) },
                            onDelete = { onDeleteSignature(sig.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Bar: Add Custom Signature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredSignatures.size} of ${signatures.size} signatures",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )

                    Button(
                        onClick = { showAddCustomDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp).testTag("add_signature_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ADD SIGNATURE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

    if (showAddCustomDialog) {
        AddCustomSignatureDialog(
            onDismiss = { showAddCustomDialog = false },
            onAdd = { newSig ->
                onAddCustomSignature(newSig)
                showAddCustomDialog = false
            }
        )
    }
}

/**
 * Card representing an individual threat signature entry.
 */
@Composable
fun SignatureItemCard(
    signature: ThreatSignatureEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val levelColor = when (signature.threatLevel) {
        "CRITICAL" -> NeonRed
        "HIGH" -> NeonAmber
        "MEDIUM" -> NeonCyan
        else -> NeonGreen
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (signature.isEnabled) CyberSurface else CyberSurface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (signature.isEnabled) CyberBorder else CyberBorder.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .testTag("signature_card_${signature.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: ID, Threat Badge, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(levelColor.copy(alpha = 0.2f))
                            .border(1.dp, levelColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = signature.threatLevel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = levelColor,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = signature.id,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = signature.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (signature.isCustom) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = NeonRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Switch(
                        checked = signature.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurfaceElevated
                        ),
                        modifier = Modifier.size(width = 36.dp, height = 22.dp)
                    )
                }
            }

            // Signature Name
            Text(
                text = signature.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (signature.isEnabled) TextPrimary else TextMuted,
                    fontSize = 13.sp
                )
            )

            // Target Band & Frequency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Band: ${signature.targetBand}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "Freq: ${signature.minFrequencyMhz} - ${signature.maxFrequencyMhz} MHz",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NeonAmber,
                        fontSize = 11.sp
                    )
                )
            }

            if (signature.signaturePatternHex.isNotBlank()) {
                Text(
                    text = "Hex Pattern: [${signature.signaturePatternHex}]",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreen,
                        fontSize = 10.sp
                    )
                )
            }

            // Expanded Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f))

                    Column {
                        Text(
                            text = "SPECTRAL ANOMALY TRIGGER:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NeonRed,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = signature.spectralAnomalyTrigger,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "BEHAVIORAL RULES:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NeonAmber,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = signature.behavioralAnomalyRules,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "EXPLOIT / TECHNICAL DETAIL:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = signature.technicalDetails,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "RECOMMENDED COUNTERMEASURE:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = signature.recommendedCountermeasure,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog to add a new custom threat signature into Room database.
 */
@Composable
fun AddCustomSignatureDialog(
    onDismiss: () -> Unit,
    onAdd: (ThreatSignatureEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Microwave Uplink") }
    var threatLevel by remember { mutableStateOf("HIGH") }
    var targetBand by remember { mutableStateOf("5.8 GHz Microwave") }
    var minFreq by remember { mutableStateOf("5800.0") }
    var maxFreq by remember { mutableStateOf("5900.0") }
    var hexPattern by remember { mutableStateOf("") }
    var anomalyTrigger by remember { mutableStateOf("Carrier purity < 80% OR Continuous burst > -45 dBm") }
    var technicalDetails by remember { mutableStateOf("Hostile emitter observed broadcasting unauthenticated payloads in airspace.") }
    var countermeasure by remember { mutableStateOf("Deploy directional RF filtering and isolate adjacent antenna lines.") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = CyberBg,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(1.dp, NeonGreen, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "DEFINE CUSTOM THREAT SIGNATURE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Signature Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberSurface,
                                unfocusedContainerColor = CyberSurface,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Category") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CyberSurface,
                                    unfocusedContainerColor = CyberSurface,
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = threatLevel,
                                onValueChange = { threatLevel = it },
                                label = { Text("Level (CRITICAL/HIGH/MED)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CyberSurface,
                                    unfocusedContainerColor = CyberSurface,
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = targetBand,
                            onValueChange = { targetBand = it },
                            label = { Text("Target RF Band") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberSurface,
                                unfocusedContainerColor = CyberSurface,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = minFreq,
                                onValueChange = { minFreq = it },
                                label = { Text("Min Freq (MHz)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CyberSurface,
                                    unfocusedContainerColor = CyberSurface,
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = maxFreq,
                                onValueChange = { maxFreq = it },
                                label = { Text("Max Freq (MHz)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CyberSurface,
                                    unfocusedContainerColor = CyberSurface,
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = hexPattern,
                            onValueChange = { hexPattern = it },
                            label = { Text("Hex Signature Pattern (e.g. DE AD)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberSurface,
                                unfocusedContainerColor = CyberSurface,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = anomalyTrigger,
                            onValueChange = { anomalyTrigger = it },
                            label = { Text("Spectral Anomaly Trigger Rule") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberSurface,
                                unfocusedContainerColor = CyberSurface,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = technicalDetails,
                            onValueChange = { technicalDetails = it },
                            label = { Text("Technical Exploitation Details") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberSurface,
                                unfocusedContainerColor = CyberSurface,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = countermeasure,
                            onValueChange = { countermeasure = it },
                            label = { Text("Recommended Operational Countermeasure") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberSurface,
                                unfocusedContainerColor = CyberSurface,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CANCEL")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val minVal = minFreq.toDoubleOrNull() ?: 5800.0
                            val maxVal = maxFreq.toDoubleOrNull() ?: 5900.0
                            val customId = "SIG-CUSTOM-${System.currentTimeMillis() % 10000}"
                            val newSig = ThreatSignatureEntity(
                                id = customId,
                                name = name.ifBlank { "Custom Threat Signature" },
                                category = category.ifBlank { "Microwave Uplink" },
                                threatLevel = threatLevel.uppercase().ifBlank { "HIGH" },
                                targetBand = targetBand.ifBlank { "5.8 GHz Microwave" },
                                minFrequencyMhz = minVal,
                                maxFrequencyMhz = maxVal,
                                modulationPattern = "ALL",
                                signaturePatternHex = hexPattern.trim(),
                                spectralAnomalyTrigger = anomalyTrigger,
                                behavioralAnomalyRules = "Custom behavioral heuristic trigger",
                                technicalDetails = technicalDetails,
                                recommendedCountermeasure = countermeasure,
                                isEnabled = true,
                                isCustom = true
                            )
                            onAdd(newSig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SAVE SIGNATURE")
                    }
                }
            }
        }
    }
}
