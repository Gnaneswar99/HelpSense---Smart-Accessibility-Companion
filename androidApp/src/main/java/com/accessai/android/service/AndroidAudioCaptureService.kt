package com.accessai.android.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.accessai.core.util.AudioCaptureService
import com.accessai.core.util.AudioChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android implementation of AudioCaptureService.
 * Uses AudioRecord API for low-latency audio capture optimized for ML inference.
 *
 * Configuration:
 * - Sample rate: 16kHz (standard for most sound classification models)
 * - Channel: Mono (sufficient for sound detection)
 * - Encoding: 16-bit PCM
 * - Chunk duration: ~1 second (16000 samples at 16kHz)
 *
 * The audio is captured in chunks that match the input size expected
 * by the YAMNet sound classification model.
 */
class AndroidAudioCaptureService(
    private val context: Context
) : AudioCaptureService {

    companion object {
        const val SAMPLE_RATE = 16000           // 16kHz — standard for ML audio models
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_DURATION_MS = 975       // ~1 second chunks for YAMNet
        const val CHUNK_SIZE_SAMPLES = (SAMPLE_RATE * CHUNK_DURATION_MS) / 1000  // 15600 samples
    }

    private var audioRecord: AudioRecord? = null
    private var _isCapturing = false

    override fun audioStream(): Flow<AudioChunk> = callbackFlow {
        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            close(SecurityException("RECORD_AUDIO permission not granted"))
            return@callbackFlow
        }

        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
            CHUNK_SIZE_SAMPLES * 2  // 2 bytes per sample (16-bit)
        )

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            close(IllegalStateException("AudioRecord failed to initialize"))
            return@callbackFlow
        }

        audioRecord = recorder
        recorder.startRecording()
        _isCapturing = true

        // Read audio in a background coroutine
        val readJob = launch(Dispatchers.IO) {
            val buffer = ShortArray(CHUNK_SIZE_SAMPLES)

            while (isActive && _isCapturing) {
                val readCount = recorder.read(buffer, 0, CHUNK_SIZE_SAMPLES)

                if (readCount > 0) {
                    val chunk = AudioChunk(
                        data = buffer.copyOf(readCount),
                        sampleRate = SAMPLE_RATE,
                        channelCount = 1,
                        timestamp = System.currentTimeMillis()
                    )
                    trySend(chunk)
                } else if (readCount == AudioRecord.ERROR_BAD_VALUE ||
                    readCount == AudioRecord.ERROR_INVALID_OPERATION
                ) {
                    // Audio error — stop capturing
                    break
                }
            }
        }

        awaitClose {
            _isCapturing = false
            readJob.cancel()
            recorder.stop()
            recorder.release()
            audioRecord = null
        }
    }

    override suspend fun startCapture() {
        // Capture is started when collecting from audioStream()
        // This method is for explicit control if needed
        _isCapturing = true
    }

    override suspend fun stopCapture() {
        _isCapturing = false
        audioRecord?.stop()
    }

    override fun isCapturing(): Boolean = _isCapturing
}
