package com.example.data.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.ScanResult
import com.example.data.evidence.EvidenceIntegrity
import com.example.data.evidence.WifiObservation
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.DeviceLocationInfo
import com.example.data.model.RealWifiTelemetry
import com.example.data.model.RfSignalInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RealDeviceHardwareManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var activeLocationListener: LocationListener? = null

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    fun hasWifiStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns the most recent Android Wi-Fi scan observations.
     *
     * These are observations of Wi-Fi access points reported by Android.
     * They are not broadband RF measurements and do not establish transmitter
     * location, direction, identity, or intent.
     */
    fun getNearbyWifiObservations(location: DeviceLocationInfo? = null): List<WifiObservation> {
        if (!hasWifiStatePermission() || wifiManager == null) return emptyList()

        val results: List<ScanResult> = try {
            wifiManager.scanResults
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return results.map { result ->
            val timestampMs = System.currentTimeMillis()
            val canonical = listOf(
                result.SSID,
                result.BSSID,
                result.frequency,
                result.level,
                result.channelWidth,
                result.capabilities,
                timestampMs
            ).joinToString("|")
            WifiObservation(
                evidenceId = "wifi-" + EvidenceIntegrity.sha256(canonical).take(16),
                ssid = result.SSID.takeIf { it.isNotBlank() },
                bssid = result.BSSID.takeIf { it.isNotBlank() },
                frequencyMhz = result.frequency,
                rssiDbm = result.level,
                channelWidth = result.channelWidth,
                capabilities = result.capabilities,
                timestampMs = timestampMs,
                latitude = location?.latitude,
                longitude = location?.longitude,
                locationAccuracyMeters = location?.accuracyMeters
            )
        }
    }

    /**
     * Attempts to acquire the device's actual real hardware GPS or network location.
     */
    suspend fun getRealDeviceLocation(): DeviceLocationInfo = withContext(Dispatchers.IO) {
        if (!hasLocationPermission() || locationManager == null) {
            return@withContext DeviceLocationInfo(
                latitude = 0.0,
                longitude = 0.0,
                altitudeMeters = 0.0,
                accuracyMeters = -1f,
                provider = "UNAVAILABLE",
                locationName = "Location unavailable",
                locationAddress = "Grant location permission to collect a real device fix.",
                isRealHardwareFix = false
            )
        }

        var bestLocation: Location? = null

        try {
            // Check all available providers: GPS, Network, Passive
            val providers = listOfNotNull(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                            bestLocation = loc
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission revoked or not granted
        }

        if (bestLocation != null) {
            val (locName, locAddr) = reverseGeocode(bestLocation.latitude, bestLocation.longitude)
            return@withContext DeviceLocationInfo(
                latitude = bestLocation.latitude,
                longitude = bestLocation.longitude,
                altitudeMeters = bestLocation.altitude,
                accuracyMeters = bestLocation.accuracy,
                provider = bestLocation.provider ?: "GPS",
                locationName = locName,
                locationAddress = locAddr,
                timestampMs = bestLocation.time,
                isRealHardwareFix = true
            )
        } else {
            // No cached location yet: request a one-shot update on main looper if possible
            val activeProvider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            return@withContext DeviceLocationInfo(
                latitude = 0.0,
                longitude = 0.0,
                altitudeMeters = 0.0,
                accuracyMeters = -1f,
                provider = activeProvider ?: "NO_ACTIVE_PROVIDER",
                locationName = "Searching for GPS Fix...",
                locationAddress = "Waiting for satellite/network acquisition",
                isRealHardwareFix = false
            )
        }
    }

    /**
     * Registers continuous hardware location updates from GPS and Network providers.
     */
    fun startContinuousLocationUpdates(
        coroutineScope: CoroutineScope,
        onLocationUpdated: (DeviceLocationInfo) -> Unit
    ) {
        if (!hasLocationPermission() || locationManager == null) return

        try {
            stopLocationUpdates()

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val (locName, locAddr) = reverseGeocode(location.latitude, location.longitude)
                        val info = DeviceLocationInfo(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitudeMeters = location.altitude,
                            accuracyMeters = location.accuracy,
                            provider = location.provider ?: "GPS",
                            locationName = locName,
                            locationAddress = locAddr,
                            timestampMs = location.time,
                            isRealHardwareFix = true
                        )
                        withContext(Dispatchers.Main) {
                            onLocationUpdated(info)
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            activeLocationListener = listener

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000L,
                    1.0f,
                    listener,
                    Looper.getMainLooper()
                )
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    1.0f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: SecurityException) {
            // Ignored if permission not granted
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun stopLocationUpdates() {
        activeLocationListener?.let {
            try {
                locationManager?.removeUpdates(it)
            } catch (_: Exception) {}
        }
        activeLocationListener = null
    }

    suspend fun createCustomLocation(
        lat: Double,
        lon: Double,
        customName: String? = null
    ): DeviceLocationInfo = withContext(Dispatchers.IO) {
        val (autoName, autoAddr) = reverseGeocode(lat, lon)
        DeviceLocationInfo(
            latitude = lat,
            longitude = lon,
            altitudeMeters = 35.0,
            accuracyMeters = 1.0f,
            provider = "MANUAL_PIN",
            locationName = customName?.takeIf { it.isNotBlank() } ?: autoName,
            locationAddress = autoAddr,
            timestampMs = System.currentTimeMillis(),
            isRealHardwareFix = true
        )
    }

    /**
     * Inspects real active Wi-Fi radio connection state.
     */
    fun getRealWifiTelemetry(): RealWifiTelemetry {
        if (!hasWifiStatePermission() || wifiManager == null) {
            return RealWifiTelemetry(
                isConnected = false,
                ssid = "Permission Required",
                bssid = "00:00:00:00:00:00",
                rssiDbm = -100,
                frequencyMhz = 0,
                linkSpeedMbps = 0,
                securityType = "N/A",
                ipAddress = "0.0.0.0",
                isRealHardware = false
            )
        }

        val network = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(network)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        val wifiInfo: WifiInfo? = try {
            wifiManager.connectionInfo
        } catch (e: Exception) {
            null
        }

        if (wifiInfo != null && isWifi) {
            val rawSsid = wifiInfo.ssid?.replace("\"", "") ?: "Connected Wi-Fi"
            val ssid = if (rawSsid == "<unknown ssid>") "Connected Wi-Fi AP" else rawSsid
            val bssid = wifiInfo.bssid ?: "00:00:00:00:00:00"
            val rssi = wifiInfo.rssi
            val freq = wifiInfo.frequency
            val linkSpeed = wifiInfo.linkSpeed
            val ipInt = wifiInfo.ipAddress
            val ipStr = String.format(
                Locale.US,
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )

            val bandName = when {
                freq in 2400..2500 -> "2.4 GHz Band"
                freq in 4900..5900 -> "5 GHz Band"
                freq > 5900 -> "6 GHz Wi-Fi 6E"
                else -> "RF Wi-Fi"
            }

            return RealWifiTelemetry(
                isConnected = true,
                ssid = ssid,
                bssid = bssid,
                rssiDbm = rssi,
                frequencyMhz = freq,
                linkSpeedMbps = linkSpeed,
                securityType = bandName,
                ipAddress = ipStr,
                isRealHardware = true
            )
        }

        return RealWifiTelemetry(
            isConnected = false,
            ssid = "No Active Wi-Fi Connection",
            bssid = "00:00:00:00:00:00",
            rssiDbm = -95,
            frequencyMhz = 0,
            linkSpeedMbps = 0,
            securityType = "Cellular / Offline",
            ipAddress = "127.0.0.1",
            isRealHardware = true
        )
    }

    /**
     * Converts real Wi-Fi telemetry into an active RF signal that can be monitored on spectrum and tactical map.
     */
    /**
     * Wi-Fi connection telemetry is intentionally not converted into an RF signal.
     * Android exposes Wi-Fi observations, not arbitrary RF/IQ spectrum measurements.
     * Use getNearbyWifiObservations() for evidence collection.
     */
    fun createRealWifiSignal(deviceLocation: DeviceLocationInfo): RfSignalInfo? = null

    private fun reverseGeocode(latitude: Double, longitude: Double): Pair<String, String> {
        return try {
            if (!Geocoder.isPresent()) {
                return Pair("Device GPS Fix", String.format(Locale.US, "Lat: %.4f, Lon: %.4f", latitude, longitude))
            }
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val name = addr.featureName ?: addr.locality ?: "Device Real GPS Location"
                val fullAddr = addr.getAddressLine(0) ?: "${addr.locality ?: ""}, ${addr.countryName ?: ""}"
                Pair(name, fullAddr)
            } else {
                Pair("Device GPS Fix", String.format(Locale.US, "Lat: %.4f, Lon: %.4f", latitude, longitude))
            }
        } catch (e: Exception) {
            Pair("Device GPS Fix", String.format(Locale.US, "Lat: %.4f, Lon: %.4f", latitude, longitude))
        }
    }

    /**
     * Geocodes a text address string or Lat,Lon into a real device location info.
     */
    suspend fun searchAddressLocation(addressQuery: String): DeviceLocationInfo = withContext(Dispatchers.IO) {
        if (addressQuery.isBlank()) return@withContext getRealDeviceLocation()

        if (addressQuery.contains(",")) {
            val parts = addressQuery.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lon = parts[1].trim().toDoubleOrNull()
                if (lat != null && lon != null) {
                    return@withContext createCustomLocation(lat, lon, "Custom Lat/Lon Coordinates")
                }
            }
        }

        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(addressQuery, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val fullAddr = addr.getAddressLine(0) ?: addressQuery
                    val name = addr.featureName ?: addr.locality ?: addressQuery
                    return@withContext DeviceLocationInfo(
                        latitude = addr.latitude,
                        longitude = addr.longitude,
                        altitudeMeters = 25.0,
                        accuracyMeters = 1.0f,
                        provider = "GEOCODER_SEARCH",
                        locationName = name,
                        locationAddress = fullAddr,
                        timestampMs = System.currentTimeMillis(),
                        isRealHardwareFix = true
                    )
                }
            }
        } catch (_: Exception) {}

        return@withContext DeviceLocationInfo(
            latitude = 0.0,
            longitude = 0.0,
            altitudeMeters = 0.0,
            accuracyMeters = -1.0f,
            provider = "GEOCODER_UNAVAILABLE",
            locationName = "Location not resolved",
            locationAddress = "The requested address could not be resolved to coordinates.",
            timestampMs = System.currentTimeMillis(),
            isRealHardwareFix = false
        )
    }
}
