package com.accessai.android.ui.screens.vision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accessai.android.ml.TFLiteImageCaptionEngine
import com.accessai.android.service.AndroidCameraService
import com.accessai.android.ui.components.CameraPreview
import com.accessai.core.model.InferenceResult
import com.accessai.core.util.TTSState
import com.accessai.core.util.TextToSpeechService
import com.accessai.core.util.HapticService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Vision screen.
 */
data class VisionUiState(
    val captionText: String = "Point your camera at something to describe it",
    val isProcessing: Boolean = false,
    val isSpeaking: Boolean = false,
    val confidence: Float = 0f
)

/**
 * VisionViewModel - manages camera feed, ML inference, and TTS.
 */
class VisionViewModel(
    val cameraService: AndroidCameraService,
    private val ttsService: TextToSpeechService,
    private val hapticService: HapticService,
    private val captionEngine: TFLiteImageCaptionEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisionUiState())
    val uiState: StateFlow<VisionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ttsService.observeState().collect { state ->
                _uiState.value = _uiState.value.copy(
                    isSpeaking = state == TTSState.SPEAKING
                )
            }
        }

        // Initialize ML engine
        viewModelScope.launch {
            captionEngine.initialize()
        }
    }

    /**
     * Capture image and run ML captioning.
     */
    fun captureAndDescribe() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            hapticService.lightTap()

            try {
                val imageBytes = cameraService.captureImage()

                // Run ML inference
                val result = captionEngine.caption(imageBytes)

                when (result) {
                    is InferenceResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            captionText = result.data.caption,
                            isProcessing = false,
                            confidence = result.confidenceScore
                        )
                        ttsService.speak(result.data.caption)
                        hapticService.success()
                    }
                    is InferenceResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            captionText = "Could not describe this scene. ${result.message}",
                            isProcessing = false,
                            confidence = 0f
                        )
                        hapticService.warning()
                    }
                    is InferenceResult.Loading -> {
                        // Stay in processing state
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    captionText = "Could not capture image. Please try again.",
                    isProcessing = false
                )
                hapticService.error()
            }
        }
    }

    fun speakCaption() {
        viewModelScope.launch {
            ttsService.speak(_uiState.value.captionText)
        }
    }

    fun stopSpeaking() {
        ttsService.stop()
    }

    fun switchCamera() {
        viewModelScope.launch { cameraService.switchCamera() }
    }

    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
        cameraService.release()
    }
}

/**
 * Vision screen with camera preview and caption overlay.
 *
 * Accessibility features:
 * - Live region for caption updates (screen reader announces changes)
 * - Large touch targets (minimum 56dp)
 * - Content descriptions on all interactive elements
 * - High contrast caption overlay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionScreen(
    onBack: () -> Unit,
    viewModel: VisionViewModel = org.koin.androidx.compose.koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vision") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back to home screen")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.switchCamera() },
                        modifier = Modifier.semantics {
                            contentDescription = "Switch between front and back camera"
                        }
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera preview fills upper area
            CameraPreview(
                cameraService = viewModel.cameraService,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 220.dp)
            )

            // Caption + controls overlay at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Caption card — live region so screen readers announce changes
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Caption: ${uiState.captionText}"
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xE6FFFFFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.captionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF212121)
                        )
                        if (uiState.confidence > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Confidence: ${(uiState.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speak / Stop button
                    FloatingActionButton(
                        onClick = {
                            if (uiState.isSpeaking) viewModel.stopSpeaking()
                            else viewModel.speakCaption()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .semantics {
                                contentDescription =
                                    if (uiState.isSpeaking) "Stop speaking"
                                    else "Read caption aloud"
                            },
                        containerColor = if (uiState.isSpeaking)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Main capture button
                    FloatingActionButton(
                        onClick = { viewModel.captureAndDescribe() },
                        modifier = Modifier
                            .size(72.dp)
                            .semantics {
                                contentDescription = "Capture photo and describe the scene"
                            },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        if (uiState.isProcessing) {
                            Text("...", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        } else {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Continuous mode toggle (placeholder for Step 3)
                    FloatingActionButton(
                        onClick = { /* TODO: Toggle continuous captioning mode */ },
                        modifier = Modifier
                            .size(56.dp)
                            .semantics {
                                contentDescription = "Toggle continuous description mode. Coming soon."
                            },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Text("\u25B6", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
