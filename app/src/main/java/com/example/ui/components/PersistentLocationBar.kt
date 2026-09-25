package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceLocationInfo
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Persistent Location HUD strip that remains visible across all screens of the application.
 * Ensures the user's live physical coordinates, altitude, and fix status are always displayed.
 */
@Composable
fun PersistentLocationBar(
    deviceLocation: DeviceLocationInfo,
    onRefreshLocation: () -> Unit,
    onSetCustomLocation: (Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLocationDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_gps")
    val gpsPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gpsPulseAlpha"
    )

    Surface(
        color = Color(0xFF09121F),
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFF1E3A5F))
            .clickable { showLocationDialog = true }
            .testTag("persistent_location_hud")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Location Icon and Coordinates
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (deviceLocation.isRealHardwareFix) Color(0xFF00382B) else Color(0xFF2E2412)
                        )
                        .border(
                            1.dp,
                            if (deviceLocation.isRealHardwareFix) NeonGreen else NeonAmber,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "My Location Indicator",
                        tint = if (deviceLocation.isRealHardwareFix) NeonGreen else NeonAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MI UBICACIÓN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = " • MY LOCATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Fix status pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (deviceLocation.isRealHardwareFix) Color(0xFF063324) else Color(0xFF3B280A)
                                )
                                .border(
                                    0.5.dp,
                                    if (deviceLocation.isRealHardwareFix) NeonGreen.copy(alpha = gpsPulseAlpha) else NeonAmber,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (deviceLocation.isRealHardwareFix) "GPS LOCKED" else "ACQUIRING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (deviceLocation.isRealHardwareFix) NeonGreen else NeonAmber
                                )
                            )
                        }
                    }

                    // Live Coordinates in high-contrast monospace
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%.5f, %.5f", deviceLocation.latitude, deviceLocation.longitude),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        )
                        if (deviceLocation.altitudeMeters > 0) {
                            Text(
                                text = "  |  ${deviceLocation.altitudeMeters.toInt()}m",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            )
                        }
                    }

                    // Neighborhood / City Name
                    Text(
                        text = deviceLocation.locationName.ifBlank { "Dispositivo Local / Real Device" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Actions: Copy, Refresh, Edit Pin
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Copy coordinates button
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(
                            "My Coordinates",
                            "${deviceLocation.latitude}, ${deviceLocation.longitude}"
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Ubicación copiada: ${deviceLocation.latitude}, ${deviceLocation.longitude}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_coordinates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Coordinates",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Refresh GPS button
                IconButton(
                    onClick = onRefreshLocation,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("refresh_gps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh GPS",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Edit Pin button
                IconButton(
                    onClick = { showLocationDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("configure_location_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditLocation,
                        contentDescription = "Configure Location",
                        tint = NeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showLocationDialog) {
        LocationConfigDialog(
            currentLocation = deviceLocation,
            onDismiss = { showLocationDialog = false },
            onRefreshGps = {
                onRefreshLocation()
                showLocationDialog = false
            },
            onSaveManualLocation = { lat, lon, name ->
                onSetCustomLocation(lat, lon, name)
                showLocationDialog = false
            }
        )
    }
}

/**
 * Tactical Dialog to view full GPS telemetry, fine-tune coordinates, or pick preset locations.
 */
@Composable
fun LocationConfigDialog(
    currentLocation: DeviceLocationInfo,
    onDismiss: () -> Unit,
    onRefreshGps: () -> Unit,
    onSaveManualLocation: (Double, Double, String) -> Unit
) {
    var latText by remember { mutableStateOf(String.format(Locale.US, "%.5f", currentLocation.latitude)) }
    var lonText by remember { mutableStateOf(String.format(Locale.US, "%.5f", currentLocation.longitude)) }
    var nameText by remember { mutableStateOf(currentLocation.locationName) }
    var isManualMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurfaceElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UBICACIÓN Y COORDENADAS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tu ubicación actual SOLO sirve como referencia del dispositivo y para centrar el mapa. No se usa como la ubicación de un hallazgo, señal o evidencia a menos que esa observación haya sido capturada allí.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // GPS Telemetry Overview Box
                Surface(
                    color = Color(0xFF0A1424),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ESTADO DEL SENSOR:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            )
                            Text(
                                text = if (currentLocation.isRealHardwareFix) "GPS ACTIVO (CONEXIÓN REAL)" else "MODO EMULADOR / ESPERANDO FIX",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentLocation.isRealHardwareFix) NeonGreen else NeonAmber,
                                    fontSize = 9.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "LAT: ${currentLocation.latitude}°",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "LON: ${currentLocation.longitude}°",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "ELEVACIÓN: ${currentLocation.altitudeMeters.toInt()} metros",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        )
                        Text(
                            text = "PROVEEDOR: ${currentLocation.provider}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )
                        if (currentLocation.locationAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "DIRECCIÓN: ${currentLocation.locationAddress}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonCyan,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action: Re-acquire Hardware GPS
                ElevatedButton(
                    onClick = onRefreshGps,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF00382B),
                        contentColor = NeonGreen
                    )
                ) {
                    Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RE-CALCULAR GPS HARDWARE REAL",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Manual Coordinates entry
                OutlinedButton(
                    onClick = { isManualMode = !isManualMode },
                    modifier = Modifier.fillMaxWidth(),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CyberBorder)
                    )
                ) {
                    Icon(imageVector = Icons.Default.EditLocation, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isManualMode) "OCULTAR AJUSTE MANUAL" else "INGRESAR COORDENADAS MANUALMENTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        )
                    )
                }

                AnimatedVisibility(visible = isManualMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("Latitud (ej: 40.4168 o -34.6037)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = lonText,
                            onValueChange = { lonText = it },
                            label = { Text("Longitud (ej: -3.7038 o -58.3816)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = nameText,
                            onValueChange = { nameText = it },
                            label = { Text("Nombre de Ubicación / Ciudad") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberBorder
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ElevatedButton(
                            onClick = {
                                val lat = latText.toDoubleOrNull() ?: currentLocation.latitude
                                val lon = lonText.toDoubleOrNull() ?: currentLocation.longitude
                                onSaveManualLocation(lat, lon, nameText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "FIJAR ESTA UBICACIÓN",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CERRAR",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                )
            }
        }
    )
}
