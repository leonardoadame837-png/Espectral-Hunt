package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentThought
import com.example.data.model.ConsensusReport
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.TacticalEnvironmentMode
import com.example.data.model.TargetPreset
import com.example.data.model.VulnerabilityFinding
import com.example.ui.components.AgentThoughtStreamView
import com.example.ui.components.ConsensusReportCard
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BugHuntAgentScreen(
    presets: List<TargetPreset>,
    selectedPreset: TargetPreset?,
    currentCode: String,
    targetName: String,
    isHunting: Boolean,
    agentThoughts: List<AgentThought>,
    consensusReport: ConsensusReport?,
    isSaved: Boolean,
    onSelectPreset: (TargetPreset) -> Unit,
    onCodeChange: (String) -> Unit,
    onTargetNameChange: (String) -> Unit,
    onRunParallelHunt: () -> Unit,
    onSaveReport: (ConsensusReport) -> Unit,
    onLocateFindingOnMap: (VulnerabilityFinding) -> Unit = {},
    environmentMode: TacticalEnvironmentMode = TacticalEnvironmentMode.REAL_DEVICE,
    deviceLocation: DeviceLocationInfo? = null,
    onToggleEnvironmentMode: (TacticalEnvironmentMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header & Description
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF003640)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Multi-Agent",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PARALLEL MULTI-AGENT CYBERSECURITY PLATFORM",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "4 specialized agents (SAST Static Analysis, Cryptographic Key Inspector, RF Waveform Telemetry Demodulator, and Threat Consensus Modeler) operate concurrently in parallel to discover vulnerabilities, verify exploitability, and synthesize a single unified defense answer.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }

        // Geolocation & Environment Anchor
        item {
            val isReal = environmentMode == TacticalEnvironmentMode.REAL_DEVICE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurface)
                    .border(1.dp, if (isReal) NeonGreen.copy(alpha = 0.5f) else NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isReal) Icons.Default.MyLocation else Icons.Default.Public,
                            contentDescription = "Environment",
                            tint = if (isReal) NeonGreen else NeonAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isReal) "AUDIT GEOLOCATION: REAL DEVICE" else "AUDIT GEOLOCATION: SF TESTBED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isReal) NeonGreen else NeonAmber,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, if (isReal) NeonGreen else NeonAmber, RoundedCornerShape(4.dp))
                            .clickable {
                                if (isReal) {
                                    onToggleEnvironmentMode(TacticalEnvironmentMode.SAN_FRANCISCO_RANGE)
                                } else {
                                    onToggleEnvironmentMode(TacticalEnvironmentMode.REAL_DEVICE)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isReal) "SWITCH TO SF TESTBED" else "SWITCH TO REAL GPS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (isReal) NeonAmber else NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isReal) {
                        "Findings will be geolocated to your live device coordinates: ${deviceLocation?.locationName ?: "Host Device"} (${String.format(java.util.Locale.US, "%.4f, %.4f", deviceLocation?.latitude ?: 37.7780, deviceLocation?.longitude ?: -122.4100)})."
                    } else {
                        "Findings are mapped to the San Francisco simulated testbed corridor (37.7780, -122.4100)."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        lineHeight = 13.sp
                    )
                )
            }
        }

        // Preset Target Scenarios Chips
        item {
            Column {
                Text(
                    text = "SELECT AUDIT TARGET SCENARIO:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontSize = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = preset.id == selectedPreset?.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF003828) else CyberSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonGreen else CyberBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectPreset(preset) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonGreen else TextPrimary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Target Name & Code Editor Terminal
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "TARGET SOURCE / PROTOCOL BUFFER INPUT:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 10.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetName,
                    onValueChange = onTargetNameChange,
                    label = { Text("Target Identifier / Firmware Binary Name", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_name_input"),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        fontSize = 11.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedContainerColor = Color(0xFF040814),
                        unfocusedContainerColor = Color(0xFF040814)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF020612))
                        .border(1.dp, Color(0xFF162544), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    BasicTextField(
                        value = currentCode,
                        onValueChange = onCodeChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("code_editor_input"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        cursorBrush = SolidColor(NeonCyan)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Launch Parallel Hunt Button
                Button(
                    onClick = onRunParallelHunt,
                    enabled = !isHunting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("launch_hunt_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color(0xFF002214),
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isHunting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PARALLEL AGENTS REASONING...",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Hunt",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DISPATCH 4 PARALLEL AGENTS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        // Live Parallel Agent Streams
        item {
            AgentThoughtStreamView(
                agentThoughts = agentThoughts,
                isHunting = isHunting
            )
        }

        // Unified Consensus Output Report
        consensusReport?.let { report ->
            item {
                ConsensusReportCard(
                    report = report,
                    isSaved = isSaved,
                    onSaveReport = onSaveReport,
                    onLocateFindingOnMap = onLocateFindingOnMap
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
