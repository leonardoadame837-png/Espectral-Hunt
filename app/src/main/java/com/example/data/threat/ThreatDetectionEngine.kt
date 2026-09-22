package com.example.data.threat

import com.example.data.local.ThreatAlertEntity
import com.example.data.local.ThreatSignatureEntity
import com.example.data.model.RfSignalInfo
import java.util.Locale
import kotlin.math.abs

class ThreatDetectionEngine {

    val defaultMaliciousSignatures: List<ThreatSignatureEntity> = listOf(
        ThreatSignatureEntity(
            id = "SIG-MICROWAVE-001",
            name = "Rogue Microwave C2 Command Injection",
            category = "Microwave Uplink",
            threatLevel = "CRITICAL",
            targetBand = "5.8 GHz Microwave",
            minFrequencyMhz = 5800.0,
            maxFrequencyMhz = 5900.0,
            modulationPattern = "ALL",
            signaturePatternHex = "DE AD",
            payloadRegex = "TACTICAL_PING|0xDEAD|CMD:",
            spectralAnomalyTrigger = "Carrier spectral purity < 82% OR Excessive sideband splatter > +/- 10 MHz",
            behavioralAnomalyRules = "Unauthenticated command burst framing with high proximity RF power (> -50 dBm)",
            technicalDetails = "Exploits unauthenticated telemetry receiver firmware by injecting raw commands over 5.8 GHz Ku/C directional microwave link. Overrides station actuators and dispatches arbitrary root system shell commands.",
            recommendedCountermeasure = "Deploy radio direction finding (RDF) for physical mast interdiction. Engage hardware RF notch-filter centered on 5842.5 MHz."
        ),
        ThreatSignatureEntity(
            id = "SIG-IMSI-002",
            name = "IMSI Catcher / StingRay Cell-Site Simulator",
            category = "Cellular Interception",
            threatLevel = "CRITICAL",
            targetBand = "Cellular 850/1900/LTE",
            minFrequencyMhz = 824.0,
            maxFrequencyMhz = 894.0,
            modulationPattern = "ALL",
            signaturePatternHex = "08 29",
            payloadRegex = "CELL_SITE_SIMULATOR|LOCATION_UPDATING_REQUEST|FORCE_2G",
            spectralAnomalyTrigger = "Broadcast power 15 dBm higher than neighbor towers with silent cipher downgrade (A5/0)",
            behavioralAnomalyRules = "Forced 2G/GSM downgrade attack, rejected ciphering, persistent Location Updating Request triggers",
            technicalDetails = "Rogue base station transceiver spoofing legitimate cellular carrier network identity (MCC/MNC). Forces mobile baseband processors in vicinity to downgrade to unencrypted A5/0 GSM mode for passive IMSI harvesting and wiretapping.",
            recommendedCountermeasure = "Lock cellular modem to 4G/5G SA only; disable 2G legacy GSM fallbacks. Deploy SDR beacon fingerprinting to triangulate rogue transmitter mast."
        ),
        ThreatSignatureEntity(
            id = "SIG-GPS-SPOOF-003",
            name = "GPS L1 Navigation Spoofing Beacon",
            category = "Satellite & GNSS Spoofing",
            threatLevel = "CRITICAL",
            targetBand = "L-Band / GNSS",
            minFrequencyMhz = 1570.0,
            maxFrequencyMhz = 1580.0,
            modulationPattern = "Chirp",
            signaturePatternHex = "53 50 4F 4F 46",
            payloadRegex = "NAV_SPOOF|FALSE_PSEUDORANGE|DRIFT_CHIRP",
            spectralAnomalyTrigger = "Carrier power > -125 dBm (authentic GNSS signals are below thermal noise floor ~ -130 dBm)",
            behavioralAnomalyRules = "Synchronous power ramp-up, unnatural clock bias drift > 500 ns/s, false ephemeris correlation",
            technicalDetails = "Transmits counterfeit pseudorange and ephemeris signals on 1575.42 MHz (GPS L1 C/A). Gradually pulls receiver tracking loops away from genuine satellite constellations to deceive autopilot and navigation telemetry.",
            recommendedCountermeasure = "Engage multi-constellation GNSS (Galileo/GLONASS/BeiDou) cross-verification. Reject sudden C/N0 power spikes > 50 dB-Hz on L1."
        ),
        ThreatSignatureEntity(
            id = "SIG-JAMMER-004",
            name = "Broadband Microwave RF Denial-of-Service Jammer",
            category = "Electronic Warfare",
            threatLevel = "CRITICAL",
            targetBand = "5.8 GHz Microwave",
            minFrequencyMhz = 5700.0,
            maxFrequencyMhz = 5950.0,
            modulationPattern = "ALL",
            signaturePatternHex = "FF FF FF FF",
            payloadRegex = "JAMMING|SPLATTER|NOISE_BURST",
            spectralAnomalyTrigger = "Spectral purity < 70% with continuous wideband noise floor elevation > 30 dB",
            behavioralAnomalyRules = "Duty cycle > 95%, catastrophic SNR degradation, wideband sweeping sweep frequency",
            technicalDetails = "High-power RF noise generator or swept-frequency sweep oscillator sweeping across microwave link frequencies to saturate receiver low-noise amplifiers (LNA) and cause complete link loss.",
            recommendedCountermeasure = "Switch communications to alternate FHSS spread-spectrum band. Activate spatial null-steering beamforming antenna."
        ),
        ThreatSignatureEntity(
            id = "SIG-SURVEILLANCE-005",
            name = "Covert Audio Surveillance Bug Transmitter",
            category = "Covert Surveillance",
            threatLevel = "HIGH",
            targetBand = "433 MHz Sub-GHz",
            minFrequencyMhz = 400.0,
            maxFrequencyMhz = 470.0,
            modulationPattern = "ALL",
            signaturePatternHex = "AA AA AA AA",
            payloadRegex = "VOICE_CARRIER|AUDIO_MOD|FIXED_KEY_STREAM_BURST|ROGUE_BURST",
            spectralAnomalyTrigger = "Continuous narrowband FM/AM carrier or periodic 100ms chirp pulse near room ambient resonance",
            behavioralAnomalyRules = "Persistent carrier emission during room conversations; fixed un-salted synchronization preamble",
            technicalDetails = "Miniaturized clandestine audio transmitter concealed in wall outlets or equipment. Broadcasts demodulatable room audio over VHF/UHF/Sub-GHz frequencies without encryption.",
            recommendedCountermeasure = "Conduct non-linear junction detector (NLJD) physical sweep. Deploy acoustic masking pink noise and isolate RF emitter with near-field directional probe."
        ),
        ThreatSignatureEntity(
            id = "SIG-DRONE-C2-006",
            name = "Tactical Drone C2 Telemetry Uplink",
            category = "Unmanned Aerial C2",
            threatLevel = "HIGH",
            targetBand = "5.8 GHz / 2.4 GHz",
            minFrequencyMhz = 5725.0,
            maxFrequencyMhz = 5875.0,
            modulationPattern = "ALL",
            signaturePatternHex = "55 41 56 01",
            payloadRegex = "DRONE_C2|MAVLINK|EXPRESSLRS|ELRS|CROSSFIRE",
            spectralAnomalyTrigger = "Rapid frequency-hopping pattern with 500 Hz packet rate and distinct Gaussian FSK signature",
            behavioralAnomalyRules = "Directional beam tracking overhead, rapid bearing change, periodic control heartbeat frames",
            technicalDetails = "High-rate command and telemetry uplink used by tactical unmanned aerial systems (UAS) operating on 2.4/5.8 GHz. Transmits bidirectional MAVLink or proprietary flight telemetry.",
            recommendedCountermeasure = "Log flight controller telemetry IDs. Verify local airspace authorization; initiate protocol-aware RF geofencing warning."
        ),
        ThreatSignatureEntity(
            id = "SIG-EVILTWIN-007",
            name = "Rogue Wi-Fi Evil Twin & 802.11 Deauth Burst",
            category = "Wi-Fi Attack Vector",
            threatLevel = "HIGH",
            targetBand = "2.4 GHz WiFi/BT",
            minFrequencyMhz = 2400.0,
            maxFrequencyMhz = 2484.0,
            modulationPattern = "ALL",
            signaturePatternHex = "C0 00",
            payloadRegex = "DEAUTH|EVIL_TWIN|KARMA|ROGUE_AP",
            spectralAnomalyTrigger = "High volume of unencrypted 802.11 Type 0x00 Subtype 0x000C management deauth frames",
            behavioralAnomalyRules = "BSSID cloning of authorized access point with higher RSSI; spoofed beacon frames",
            technicalDetails = "Transmits spoofed 802.11 de-authentication management frames to disconnect clients from the legitimate enterprise network, enticing them to reconnect to a rogue clone AP for credential harvesting.",
            recommendedCountermeasure = "Enforce 802.11w Protected Management Frames (PMF). Blacklist rogue BSSID at client layer; inspect WPA3 enterprise certificates."
        ),
        ThreatSignatureEntity(
            id = "SIG-LORA-EXFIL-008",
            name = "Sub-GHz LoRa Covert Cyber Exfiltration Beacon",
            category = "Physical Cyber Exfiltration",
            threatLevel = "HIGH",
            targetBand = "915 MHz ISM Mesh",
            minFrequencyMhz = 902.0,
            maxFrequencyMhz = 928.0,
            modulationPattern = "FSK",
            signaturePatternHex = "4D 45 53 48",
            payloadRegex = "UNENCRYPTED_TELEMETRY_STREAM|CLEAR_PAYLOAD|HOP_COUNT",
            spectralAnomalyTrigger = "Chirp spread spectrum / FSK packet with anomalous hop count and cleartext payloads",
            behavioralAnomalyRules = "Multi-hop relay with no cryptographic authentication; payload contains system exfiltration fragments",
            technicalDetails = "Air-gapped network malware exfiltrating compromised host data via connected sub-GHz USB transceiver or compromised IoT peripheral over 915 MHz long-range mesh.",
            recommendedCountermeasure = "Block unauthenticated LoRa hop nodes in regional network routing tables. Mandate hardware-signed AES-128 CCM payloads."
        ),
        ThreatSignatureEntity(
            id = "SIG-SCADA-009",
            name = "Industrial SCADA Fixed Key-Stream Replay Exploit",
            category = "Industrial IoT Injection",
            threatLevel = "ELEVATED",
            targetBand = "433 MHz Sub-GHz",
            minFrequencyMhz = 430.0,
            maxFrequencyMhz = 440.0,
            modulationPattern = "ALL",
            signaturePatternHex = "2D D4",
            payloadRegex = "FIXED_KEY_STREAM_BURST|STATIC_KEY_STREAM|REPLAY",
            spectralAnomalyTrigger = "Static preamble ('2D D4') followed by unchanging keystream burst without timestamp nonces",
            behavioralAnomalyRules = "Identical packet transmission at fixed intervals; absence of rolling code cryptographic counters",
            technicalDetails = "Targeting legacy industrial PLCs and remote valve actuators. Lack of rolling code verification allows an adversary with an SDR to record and replay open/close switch commands.",
            recommendedCountermeasure = "Upgrade industrial RF transceiver firmware to enforce rolling-code KeeLoq or authenticated HMAC-SHA256 nonces."
        ),
        ThreatSignatureEntity(
            id = "SIG-BLE-TRACKER-010",
            name = "Rogue BLE Stalking & Location Tracking Beacon",
            category = "Covert Surveillance",
            threatLevel = "MEDIUM",
            targetBand = "2.4 GHz WiFi/BT",
            minFrequencyMhz = 2400.0,
            maxFrequencyMhz = 2483.5,
            modulationPattern = "ALL",
            signaturePatternHex = "4C 00 12 19",
            payloadRegex = "AIRTAG_CLONE|OPEN_HAYSTACK|FINDMY_BEACON",
            spectralAnomalyTrigger = "Periodic 2.4 GHz BLE advertisement burst cycling MAC addresses while preserving payload key",
            behavioralAnomalyRules = "Transmitter bearing consistently follows host coordinates over multiple geographic waypoints",
            technicalDetails = "Cloned Apple Find My / BLE tracking device emitting periodic rotating Bluetooth Low Energy advertisements designed to covertly track victim vehicle or personnel coordinates.",
            recommendedCountermeasure = "Utilize BLE background scanner to identify persistent tracking keys. Physically inspect vehicle wheel wells and chassis."
        )
    )

    /**
     * Evaluates a single RF signal against active threat signatures and behavioral anomaly heuristics.
     */
    fun evaluateSignal(
        signal: RfSignalInfo,
        signatures: List<ThreatSignatureEntity>
    ): ThreatAlertEntity? {
        val anomalies = mutableListOf<String>()
        var matchedSignature: ThreatSignatureEntity? = null
        var matchConfidence = 0f

        // 1. Evaluate against Known Malicious Signatures
        for (sig in signatures.filter { it.isEnabled }) {
            var signatureScore = 0f

            // Frequency Range Match
            val inFreqRange = signal.frequencyMhz in (sig.minFrequencyMhz - 2.0)..(sig.maxFrequencyMhz + 2.0)
            if (inFreqRange) {
                signatureScore += 35f
            }

            // Hex Signature Match
            val hexPattern = sig.signaturePatternHex.replace(" ", "")
            val signalHex = signal.rawHexPayload.replace(" ", "")
            if (hexPattern.isNotBlank() && signalHex.contains(hexPattern, ignoreCase = true)) {
                signatureScore += 35f
                anomalies.add("Malicious Hex Signature Match: [${sig.signaturePatternHex}] detected in raw carrier payload")
            }

            // Payload Keyword / Regex Match
            if (sig.payloadRegex.isNotBlank()) {
                val keywords = sig.payloadRegex.split("|")
                val matchedKeyword = keywords.firstOrNull { kw ->
                    signal.demodulatedText.contains(kw, ignoreCase = true) ||
                    signal.description.contains(kw, ignoreCase = true) ||
                    signal.analystAssessment.contains(kw, ignoreCase = true)
                }
                if (matchedKeyword != null) {
                    signatureScore += 25f
                    anomalies.add("Threat Indicator String: '$matchedKeyword' verified in demodulated telemetry")
                }
            }

            // Modulation pattern match
            if (sig.modulationPattern.equals("ALL", true) || signal.modulationType.contains(sig.modulationPattern, true)) {
                signatureScore += 10f
            }

            if (signatureScore >= 60f) {
                if (matchedSignature == null || signatureScore > matchConfidence) {
                    matchedSignature = sig
                    matchConfidence = signatureScore.coerceAtMost(99.5f)
                }
            }
        }

        // 2. Evaluate Behavioral RF Anomalies
        // A. Spectral Purity Degradation Anomaly
        if (signal.spectralPurityPercent < 85.0f) {
            val degradation = String.format(Locale.US, "%.1f%%", signal.spectralPurityPercent)
            anomalies.add("Spectral Purity Anomaly: Carrier purity severely degraded to $degradation (threshold: 85.0%) due to harmonic distortion or overdriven amplifier.")
        }

        // B. High Proximity RF Power Anomaly
        if (signal.powerDbm > -48.0f && signal.estimatedDistanceM < 200f) {
            anomalies.add("Proximity RF Power Burst Anomaly: Signal strength is abnormally high (${signal.powerDbm} dBm) within immediate perimeter (${signal.estimatedDistanceM.toInt()}m bearing ${signal.azimuthDegrees.toInt()}°).")
        }

        // C. Rogue Unauthenticated Telemetry
        if (signal.isRogue && (signal.encryptionState.contains("UNENCRYPTED", true) || signal.encryptionState.contains("NONE", true) || signal.encryptionState.contains("RAW", true))) {
            anomalies.add("Unauthenticated Rogue Emitter: Operating with unencrypted cleartext modulation framing and zero link-layer cryptography.")
        }

        // D. Replay Exploit / Static Synchronization
        if (signal.encryptionState.contains("STATIC", true) || signal.encryptionState.contains("REPLAY", true) || signal.analystAssessment.contains("replay", true)) {
            anomalies.add("Vulnerable Synchronization Protocol: Fixed non-rolling sequence counter susceptible to SDR replay injection.")
        }

        // E. Severe Threat Rating
        if (signal.threatRating.contains("CRITICAL", true) || signal.threatRating.contains("ROGUE", true)) {
            anomalies.add("Critical SIGINT Emitter Classification: Signal tagged as hostile or unauthorized in regional airspace database.")
        }

        // Determine if alert should be raised
        val hasSignatureMatch = matchedSignature != null
        val hasBehavioralAnomalies = anomalies.isNotEmpty()

        if (!hasSignatureMatch && !hasBehavioralAnomalies && !signal.isRogue) {
            return null
        }

        // If signal is rogue but no anomalies triggered yet, add baseline anomaly
        if (signal.isRogue && anomalies.isEmpty()) {
            anomalies.add("Unauthorized RF Emitter: Signal operates outside approved spectrum license parameters.")
        }

        // Compute final Threat Level
        val finalThreatLevel = when {
            matchedSignature?.threatLevel == "CRITICAL" || signal.threatRating.contains("CRITICAL", true) -> "CRITICAL"
            matchedSignature?.threatLevel == "HIGH" || signal.threatRating.contains("HIGH", true) || signal.isRogue -> "HIGH"
            matchedSignature?.threatLevel == "ELEVATED" || signal.threatRating.contains("ELEVATED", true) -> "HIGH"
            matchedSignature?.threatLevel == "MEDIUM" -> "MEDIUM"
            anomalies.size >= 2 -> "HIGH"
            else -> "MEDIUM"
        }

        val detectionType = when {
            hasSignatureMatch && anomalies.size >= 2 -> "HYBRID_DETECTION"
            hasSignatureMatch -> "SIGNATURE_MATCH"
            else -> "BEHAVIORAL_ANOMALY"
        }

        val finalConfidence = if (hasSignatureMatch) {
            matchConfidence.coerceIn(80f, 98.8f)
        } else {
            (65f + (anomalies.size * 10f)).coerceIn(70f, 94.5f)
        }

        val signatureName = matchedSignature?.name
            ?: "Behavioral RF Anomaly: ${signal.modulationType} on ${signal.bandName}"

        val summary = if (matchedSignature != null) {
            "${matchedSignature.technicalDetails} Detected on ${signal.frequencyMhz} MHz (${signal.bandName}) at ${signal.locationAddress}."
        } else {
            "Anomalous transmitter detected on ${signal.frequencyMhz} MHz (${signal.bandName}) displaying ${anomalies.size} behavioral deviations from nominal baseline."
        }

        val countermeasure = matchedSignature?.recommendedCountermeasure
            ?: signal.recommendedAction.ifBlank {
                "Deploy directional radio direction finding (RDF); isolate sensitive receivers and verify perimeter RF integrity."
            }

        return ThreatAlertEntity(
            alertId = "ALT-${signal.frequencyMhz.toInt()}-${signal.id.uppercase()}",
            signalId = signal.id,
            signalFrequencyMhz = signal.frequencyMhz,
            signalBandName = signal.bandName,
            signalProtocol = signal.protocolName,
            signalPowerDbm = signal.powerDbm,
            signalDistanceM = signal.estimatedDistanceM,
            signalAzimuthDeg = signal.azimuthDegrees,
            locationAddress = signal.locationAddress,
            matchedSignatureId = matchedSignature?.id,
            matchedSignatureName = signatureName,
            threatLevel = finalThreatLevel,
            detectionType = detectionType,
            anomalyIndicators = anomalies.joinToString("; "),
            confidencePercent = finalConfidence,
            technicalSummary = summary,
            recommendedCountermeasure = countermeasure,
            status = "ACTIVE"
        )
    }

    /**
     * Evaluates a list of RF signals against all active signatures and returns threat alerts.
     */
    fun evaluateAllSignals(
        signals: List<RfSignalInfo>,
        signatures: List<ThreatSignatureEntity>
    ): List<ThreatAlertEntity> {
        return signals.mapNotNull { signal ->
            evaluateSignal(signal, signatures)
        }
    }
}
