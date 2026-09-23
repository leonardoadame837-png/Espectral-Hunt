package com.example.data.evidence

data class WifiObservation(
    val evidenceId: String,
    val ssid: String?,
    val bssid: String?,
    val frequencyMhz: Int,
    val rssiDbm: Int,
    val channelWidth: Int,
    val capabilities: String,
    val timestampMs: Long,
    val latitude: Double?,
    val longitude: Double?,
    val locationAccuracyMeters: Float?
)
