package com.example.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SignalAudioSynthesizer(private val coroutineScope: CoroutineScope) {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSignalTone = MutableStateFlow(880)
    val currentSignalTone: StateFlow<Int> = _currentSignalTone.asStateFlow()

    private val _currentModulationType = MutableStateFlow("FSK")
    val currentModulationType: StateFlow<String> = _currentModulationType.asStateFlow()

    private val _volume = MutableStateFlow(0.35f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Real-time audio waveform samples for oscilloscope UI (64 normalized points)
    private val _waveformSamples = MutableStateFlow(FloatArray(64) { 0f })
    val waveformSamples: StateFlow<FloatArray> = _waveformSamples.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    private val sampleRate = 44100

    fun updateTone(baseToneHz: Int) {
        _currentSignalTone.value = baseToneHz.coerceIn(100, 8000)
    }

    fun updateModulationType(modulationType: String) {
        _currentModulationType.value = modulationType
    }

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
    }

    fun startDemodAudio(baseToneHz: Int = 880, modulationType: String = "FSK") {
        _currentSignalTone.value = baseToneHz.coerceIn(100, 8000)
        _currentModulationType.value = modulationType

        if (_isPlaying.value && playbackJob?.isActive == true) {
            // Already playing: dynamic properties updated seamlessly without AudioTrack interruption
            return
        }

        _isPlaying.value = true

        playbackJob = coroutineScope.launch(Dispatchers.IO) {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, 4096)

            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val buffer = ShortArray(bufferSize / 2)
                var phase = 0.0
                var step = 0
                val waveformBuffer = FloatArray(64)

                while (isActive && _isPlaying.value) {
                    val baseTone = _currentSignalTone.value.toDouble()
                    val modType = _currentModulationType.value
                    val currentVol = _volume.value.toDouble()

                    // Calculate frequency and amplitude modulation based on mode
                    val (freq, amDepth) = when (modType) {
                        "AM" -> {
                            val modEnvelope = 0.5 + 0.5 * sin(step * 0.15)
                            Pair(baseTone, modEnvelope)
                        }
                        "FM" -> {
                            val freqDev = sin(step * 0.1) * 200.0
                            Pair(baseTone + freqDev, 1.0)
                        }
                        "FSK" -> {
                            val isMark = (step / 15) % 2 == 0
                            Pair(if (isMark) baseTone else (baseTone * 1.35), 1.0)
                        }
                        "CW / Morse", "CW", "Morse" -> {
                            // Morse code pattern (CQ SOS style pulse sequence)
                            val morseSeq = intArrayOf(1, 0, 1, 0, 1, 0, 0, 3, 0, 3, 0, 3, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0)
                            val idx = (step / 8) % morseSeq.size
                            val active = morseSeq[idx] > 0
                            Pair(baseTone, if (active) 1.0 else 0.0)
                        }
                        "QAM", "PSK", "BPSK" -> {
                            val phaseNoise = sin(step * 0.3) * 60.0
                            Pair(baseTone + phaseNoise, 0.8 + 0.2 * sin(step * 0.2))
                        }
                        "Chirp Spread", "Chirp" -> {
                            val sweepRatio = (step % 25) / 25.0
                            Pair(baseTone * (0.7 + sweepRatio * 0.6), 1.0)
                        }
                        "Microwave Relay", "Microwave" -> {
                            val burst = if ((step / 10) % 4 != 0) 1.0 else 0.1
                            Pair(baseTone + ((step % 30) * 12.0), burst)
                        }
                        else -> Pair(baseTone, 1.0)
                    }

                    val phaseIncrement = (2.0 * PI * freq) / sampleRate

                    for (i in buffer.indices) {
                        val sampleVal = sin(phase) * currentVol * amDepth
                        val sampleShort = (sampleVal.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                        buffer[i] = sampleShort
                        phase += phaseIncrement
                        if (phase > 2.0 * PI) phase -= 2.0 * PI

                        if (i < 64) {
                            waveformBuffer[i] = sampleVal.toFloat()
                        }
                    }

                    _waveformSamples.value = waveformBuffer.copyOf()

                    audioTrack?.write(buffer, 0, buffer.size)
                    step++
                    delay(20)
                }
            } catch (e: Exception) {
                // Graceful handling
            } finally {
                releaseTrack()
            }
        }
    }

    fun stopDemodAudio() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        releaseTrack()
    }

    private fun releaseTrack() {
        runCatching { audioTrack?.pause() }
        runCatching { audioTrack?.flush() }
        runCatching { audioTrack?.stop() }
        runCatching { audioTrack?.release() }
        audioTrack = null
        _waveformSamples.value = FloatArray(64) { 0f }
    }
}

