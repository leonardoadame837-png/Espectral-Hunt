package com.example.data.hardware

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

data class VoiceCaptureEntry(
    val id: String,
    val timestampFormatted: String,
    val textContent: String,
    val peakFrequencyHz: Int,
    val dbVolume: Float,
    val isRealMicCapture: Boolean
)

class VoiceAudioCapturer(private val context: Context) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _rmsDb = MutableStateFlow(-60f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _peakFreqHz = MutableStateFlow(0)
    val peakFreqHz: StateFlow<Int> = _peakFreqHz.asStateFlow()

    private val _liveVoiceWaveform = MutableStateFlow(FloatArray(64) { 0f })
    val liveVoiceWaveform: StateFlow<FloatArray> = _liveVoiceWaveform.asStateFlow()

    private val _isSpeechDetected = MutableStateFlow(false)
    val isSpeechDetected: StateFlow<Boolean> = _isSpeechDetected.asStateFlow()

    private val _capturedTranscripts = MutableStateFlow<List<VoiceCaptureEntry>>(emptyList())
    val capturedTranscripts: StateFlow<List<VoiceCaptureEntry>> = _capturedTranscripts.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startVoiceCapture(scope: CoroutineScope) {
        if (!hasRecordAudioPermission()) return
        if (_isRecording.value) return

        _isRecording.value = true

        // Start Speech Recognizer for real-time speech-to-text
        scope.launch(Dispatchers.Main) {
            initSpeechRecognizer()
        }

        captureJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    _isRecording.value = false
                    return@launch
                }

                audioRecord?.startRecording()
                val audioBuffer = ShortArray(1024)
                val waveformDisplayBuffer = FloatArray(64)

                while (isActive && _isRecording.value) {
                    val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                    if (readSize > 0) {
                        // Calculate RMS Volume
                        var sumSquare = 0.0
                        var maxVal = 0
                        for (i in 0 until readSize) {
                            val sample = audioBuffer[i].toInt()
                            sumSquare += sample * sample
                            if (abs(sample) > maxVal) {
                                maxVal = abs(sample)
                            }
                        }
                        val rms = sqrt(sumSquare / readSize)
                        val db = if (rms > 0) (20 * log10(rms / 32768.0)).toFloat().coerceIn(-60f, 0f) else -60f
                        _rmsDb.value = db

                        // Voice Activity Detection (VAD threshold)
                        val speechActive = db > -35f
                        _isSpeechDetected.value = speechActive

                        // Estimate Zero-Crossing Rate to compute dominant acoustic voice frequency
                        var zeroCrossings = 0
                        for (i in 0 until readSize - 1) {
                            if ((audioBuffer[i] >= 0 && audioBuffer[i + 1] < 0) ||
                                (audioBuffer[i] < 0 && audioBuffer[i + 1] >= 0)
                            ) {
                                zeroCrossings++
                            }
                        }
                        val estimatedFreq = ((zeroCrossings * sampleRate) / (2 * readSize)).coerceIn(80, 4000)
                        _peakFreqHz.value = if (speechActive) estimatedFreq else 0

                        // Downsample buffer to 64 normalized float points for real-time oscilloscope
                        val chunkSize = readSize / 64
                        if (chunkSize > 0) {
                            for (i in 0 until 64) {
                                val idx = (i * chunkSize).coerceIn(0, readSize - 1)
                                waveformDisplayBuffer[i] = audioBuffer[idx] / 32768.0f
                            }
                            _liveVoiceWaveform.value = waveformDisplayBuffer.copyOf()
                        }
                    }
                }
            } catch (e: Exception) {
                _isRecording.value = false
            } finally {
                stopAudioRecord()
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {
                        _isSpeechDetected.value = true
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        // Restart recognition cycle
                        if (_isRecording.value) {
                            restartSpeechRecognition()
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            if (recognizedText.isNotBlank()) {
                                addTranscript(recognizedText)
                            }
                        }
                        if (_isRecording.value) {
                            restartSpeechRecognition()
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val partialText = matches[0]
                            if (partialText.isNotBlank()) {
                                // Live update partial transcript
                            }
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            restartSpeechRecognition()
        } catch (_: Exception) {}
    }

    private fun restartSpeechRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {}
    }

    fun addTranscript(text: String, fromMicrophone: Boolean = true) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val entry = VoiceCaptureEntry(
            id = "voice_${System.currentTimeMillis()}",
            timestampFormatted = sdf.format(Date()),
            textContent = text,
            peakFrequencyHz = _peakFreqHz.value,
            dbVolume = _rmsDb.value,
            isRealMicCapture = fromMicrophone
        )
        _capturedTranscripts.value = (listOf(entry) + _capturedTranscripts.value).take(50)
    }

    fun stopVoiceCapture() {
        _isRecording.value = false
        captureJob?.cancel()
        captureJob = null
        stopAudioRecord()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _rmsDb.value = -60f
        _peakFreqHz.value = 0
        _isSpeechDetected.value = false
        _liveVoiceWaveform.value = FloatArray(64) { 0f }
    }

    private fun stopAudioRecord() {
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun clearVoiceTranscripts() {
        _capturedTranscripts.value = emptyList()
    }
}
