package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EvidenceRecordEntity
import com.example.ui.theme.*

@Composable
fun EvidenceChainScreen(evidence: List<EvidenceRecordEntity>, onCaptureLocationEvidence: () -> Unit,
                       onReplay: () -> Unit, modifier: Modifier = Modifier) {
    var whyRecord by remember { mutableStateOf<EvidenceRecordEntity?>(null) }
    LazyColumn(modifier.fillMaxSize().background(CyberBg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("EVIDENCE CHAIN", color = NeonCyan, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Sensor → Raw measurement → Calculation → Observation → Finding",
                        color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Icon(Icons.Default.Verified, null, tint = NeonGreen)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCaptureLocationEvidence, modifier = Modifier.weight(1f)) {
                    Text("CAPTURE VERIFIED LOCATION", fontSize = 9.sp)
                }
                OutlinedButton(onClick = onReplay, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp)); Text("REPLAY", fontSize = 9.sp)
                }
            }
        }
        item { Text("Only committed evidence appears here. A phone location is evidence of the phone's location—not proof of a finding or transmitter.",
            color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
        if (evidence.isEmpty()) item { Text("No evidence committed yet.", color = TextMuted) }
        items(evidence) { record ->
            Card(colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier.fillMaxWidth().border(1.dp, CyberBorder, RoundedCornerShape(10.dp))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(record.classification, color = classificationColor(record.classification),
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text("#" + record.id, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    }
                    Text(record.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(record.payload, color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    record.locationLabel?.let { Text("WHERE: " + it, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp) }
                    Text("HASH: " + record.sha256.take(20) + "…", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
                    Text("PARENT: " + (record.previousHash?.take(16)?.plus("…") ?: "GENESIS"),
                        color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
                    OutlinedButton(onClick = { whyRecord = record }) {
                        Icon(Icons.Default.HelpOutline, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp)); Text("SHOW ME WHY", fontSize = 9.sp)
                    }
                }
            }
        }
    }
    whyRecord?.let { record ->
        AlertDialog(onDismissRequest = { whyRecord = null }, title = { Text("WHY THIS EVIDENCE EXISTS") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Classification: " + record.classification)
                Text("Source: " + record.sourceType)
                Text(record.payload)
                Text("Chain parent: " + (record.previousHash?.take(20)?.plus("…") ?: "GENESIS"))
                if (record.supportingEvidenceIds.isNotBlank()) Text("Supporting evidence IDs: " + record.supportingEvidenceIds)
                Text("NOT PROVEN: source identity, cause, intent, or any finding not directly supported by linked evidence.")
            }}, confirmButton = { TextButton(onClick = { whyRecord = null }) { Text("CLOSE") } })
    }
}
private fun classificationColor(value: String): Color = when (value) {
    "MEASURED" -> NeonGreen
    "CALCULATED" -> NeonCyan
    "AI_INTERPRETATION" -> NeonAmber
    else -> NeonRed
}
