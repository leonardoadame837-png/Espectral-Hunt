package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EvidenceRecordEntity
import com.example.ui.theme.*

@Composable
fun InvestigationReplayScreen(evidence: List<EvidenceRecordEntity>, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(CyberBg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("INVESTIGATION REPLAY", color = NeonCyan, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Chronological reconstruction of committed evidence",
                        color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Icon(Icons.Default.Replay, null, tint = NeonCyan)
            }
        }
        item { OutlinedButton(onClick = onBack) { Text("BACK TO EVIDENCE") } }
        items(evidence.sortedBy { it.createdAt }) { record ->
            Row(Modifier.fillMaxWidth()) {
                Text(java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(record.createdAt)),
                    color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 9.sp, modifier = Modifier.width(88.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.classification, color = TextPrimary, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Text(record.title, color = TextSecondary, fontSize = 11.sp)
                    Text(record.payload, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                }
            }
        }
    }
}
