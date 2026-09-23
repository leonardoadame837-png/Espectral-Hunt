# Espectral Hunt

Espectral Hunt is an **Android-only field investigation platform** built around measurements and observations available from the phone itself. It does **not** require or use an external SDR, RF receiver, radio front-end, or other external radio hardware.

## Current direction

The application is being changed from a demonstration-oriented RF/security dashboard into an evidence-grounded Android investigation tool.

### Real Android data sources

Depending on the device and granted permissions, the app can work with:

- Android Wi-Fi observations, including frequency and RSSI reported by the platform
- Bluetooth observations when the corresponding Android APIs and permissions are available
- Microphone audio measurements
- GPS/location fixes
- Accelerometer, gyroscope, geomagnetic and other sensors when present
- Camera evidence when the camera workspace is implemented
- Imported evidence already collected by the user

Android does not expose arbitrary broadband RF spectrum or raw IQ from the phone's ordinary Wi-Fi/Bluetooth interfaces. Therefore Espectral Hunt must not manufacture a microwave spectrum, demodulated payload, RF azimuth, transmitter distance, or transmitter location from ordinary phone telemetry.

Android Wi-Fi scanning also has platform permission and scan-rate restrictions. The app must record whether a scan actually succeeded and must preserve the observation timestamp rather than treating stale scan results as a new measurement.

## Forensic evidence model

The evidence layer classifies information as:

- **MEASURED** — directly recorded from a supported Android source
- **CALCULATED** — deterministic calculation performed from recorded evidence
- **AI_INTERPRETATION** — an AI interpretation that cites its supporting evidence
- **UNVERIFIED_HYPOTHESIS** — a possible explanation that still requires verification

Every evidence record has an evidence ID and SHA-256 integrity value. AI findings are required to reference existing evidence IDs.

### Non-negotiable AI rules

The AI Investigation Agent must:

1. Never invent a frequency, RSSI, timestamp, GPS coordinate, acoustic value, device identity, payload, event, or other measurement.
2. Never turn a map visualization into proof that a transmitter exists at a particular location.
3. Never treat an inferred or estimated value as a direct measurement.
4. Cite the evidence records supporting factual findings.
5. State limitations and alternative explanations when evidence is incomplete.
6. Abstain from a factual conclusion when the available evidence does not support it.
7. Keep original evidence separate from AI-generated text.
8. Preserve provenance so another reviewer can reproduce the analysis.

The AI is an **interpreter of evidence**, not the source of evidence.

## Measurement integrity

Simulated data is permitted only for development/testing and must remain explicitly labeled as **SIMULATED**. It must never enter the real-device evidence stream or be presented as a physical RF measurement.

A Wi-Fi RSSI reading is evidence of the reported Wi-Fi observation. It is not, by itself, proof of transmitter identity, direction, distance, ownership, intent, or physical source location.

Likewise, a microphone recording is evidence of an acoustic recording at the phone's position; it does not by itself identify the source of the sound.

## Tactical analysis

The tactical workspace can visualize evidence that has actually been collected. The map must distinguish:

- measurement location
- observation location
- calculated area/track
- AI interpretation
- unverified hypothesis

No synthetic emitter markers should be created around the phone merely because the phone has a GPS fix.

## Android sensors

Android devices do not have a standardized sensor set. Espectral Hunt should detect sensor availability at runtime and record the actual sensor type, timestamp, accuracy metadata, and raw/normalized values when a sensor is used.

The application should degrade gracefully when a device does not contain a particular sensor.

## Development status

The repository currently contains the Android foundation, Jetpack Compose UI, Room persistence, Android location/Wi-Fi access, microphone capture, and the existing investigation UI.

Recent integrity changes:

- Removed the unused generic blank workflow.
- Android CI now uses the checked-in Gradle wrapper.
- Added forensic evidence classification and provenance models.
- Added deterministic SHA-256 evidence hashing and validation tests.
- Added a real Android Wi-Fi observation model.
- Stopped converting ordinary Wi-Fi connection telemetry into fabricated broadband RF measurements.
- Removed synthetic RF emitter and tactical vulnerability findings from the real-device data path.
- Removed fabricated coordinate fallbacks when a real GPS/geocoder result is unavailable.
- Declared optional Android motion/position sensor capabilities and nearby-device permissions.

## Build

Use the Gradle wrapper:

~~~text
./gradlew test
./gradlew assembleDebug
~~~

On Windows:

~~~text
gradlew.bat test
gradlew.bat assembleDebug
~~~

The Android workflow builds the debug APK and publishes successful main-branch builds as GitHub Releases.

## Responsible use

Espectral Hunt is intended for lawful, authorized observation, measurement, analysis, and investigation. Evidence integrity metadata helps preserve provenance; it does not by itself establish scientific validity or legal admissibility.

## Repository

GitHub: https://github.com/leonardoadame837-png/Espectral-Hunt
