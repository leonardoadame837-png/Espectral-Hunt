# SpectraHunter

SpectraHunter is an Android application foundation for spectrum observation, RF signal visualization, and evidence-oriented event analysis.

## Project Status

**Stage:** Android project foundation / initial UI scaffolding

The current project establishes the documentation and application foundation for future spectrum-analysis, camera/audio correlation, and investigation workflows.

> **Measurement integrity:** Simulated data must be clearly identified as simulated. The application must not present simulated or inferred results as physical RF measurements.

## Planned Capabilities

- RF spectrum visualization
- Waterfall-style signal history
- Signal/peak detection and basic RF statistics
- Clear separation between simulated and measured/imported sources
- IP camera integration
- Accessible camera audio analysis
- Timestamp-based RF, video, and audio event correlation
- Investigation and evidence records
- Authenticated backend/API integration

## Architecture Direction

The application is being designed so measurement sources remain separate from the analysis and presentation layers:

```text
Measurement / Import
        |
        v
Signal Normalization
        |
        v
Spectrum Analysis
        |
        +----> Waterfall / Visualization
        |
        +----> Detection / RF Statistics
        |
        +----> Event Correlation
        |
        v
Investigation / Evidence
```

Camera and audio inputs are complementary evidence sources. A camera connection by itself is **not** evidence of an RF detection.

## Android Foundation

The planned Android application uses:

- Kotlin
- Android Gradle build system
- Jetpack Compose / Material 3
- AndroidX
- Minimum Android API target selected for the initial application foundation

The initial manifest should request only permissions required by implemented functionality. Network access is expected for future camera/API integrations; camera, microphone, location, Bluetooth, and other sensitive permissions should be added only when the corresponding feature is implemented.

## Source Types

SpectraHunter should preserve source provenance throughout the analysis pipeline. Example source categories include:

- **SIMULATED** — generated test/demo data
- **IMPORTED_MEASUREMENT** — previously captured measurement data
- **LIVE_MEASUREMENT** — data received from an authorized live measurement source
- **UNKNOWN** — source could not be established

A frequency, power value, SNR estimate, or detection confidence value does not by itself prove transmitter identity, physical location, or intent.

## Responsible Use

SpectraHunter is intended for lawful, authorized spectrum observation, testing, analysis, and investigation workflows.

Users are responsible for complying with applicable laws, regulations, privacy requirements, and authorization requirements when collecting RF, video, or audio data.

## Development

Open the project in Android Studio and allow Gradle to synchronize the project.

Future development will add the spectrum-analysis engine, measurement-source adapters, camera/audio workspace, event correlation, authentication, and investigation storage incrementally.

## Repository

GitHub: https://github.com/leonardoadame837-png/Espectral-Hunt
