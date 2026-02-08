package com.accessai.android.ui.screens.audio

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accessai.android.ml.TFLiteSoundClassificationEngine
import com.accessai.android.service.PermissionManager
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.SoundEvent
import com.accessai.core.model.SoundPriority
import com.accessai.core.util.AudioCaptureService
import com.accessai.core.util.HapticService
import com.accessai.core.util.TextToSpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Audio monitoring screen.
 */
data class AudioUiState(
    val isListening: Boolean = false,
    val currentSound: String = "Not listening",
    val currentPriority: SoundPriority = SoundPriority.LOW,
    val confidence: Float = 0f,
    val recentEvents: List<SoundEventDisplay> = emptyList(),
    val inferenceTimeMs: Long = 0
)

/**
 * Display model for a sound event in the history list.
 */
data class SoundEventDisplay(
    val soundClass: String,
    val displayName: String,
    val emoji: String,
    val priority: SoundPriority,
    val confidence: Float,
    val timeAgo: String
)

/**
 * AudioViewModel - manages sound monitoring, classification, and alerts.
 */
class AudioViewModel(
    private val audioCaptureService: AudioCaptureService,
    private val soundClassificationEngine: TFLiteSoundClassificationEngine,
    private val ttsService: TextToSpeechService,
    private val hapticService: HapticService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    private val eventHistory = mutableListOf<SoundEventDisplay>()
    private var monitoringStartTime = 0L

    init {
        viewModelScope.launch {
            soundClassificationEngine.initialize()
        }
    }

    /**
     * Start listening for sounds.
     */
    fun startListening() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isListening = true,
                currentSound = "Listening..."
            )
            monitoringStartTime = System.currentTimeMillis()

            audioCaptureService.audioStream().collect { chunk ->
                // Classify each audio chunk
                val result = soundClassificationEngine.classify(
                    audioData = chunk.data,
                    sampleRate = chunk.sampleRate
                )

                when (result) {
                    is InferenceResult.Success -> {
                        val event = result.data
                        if (event.confidence > 0.3f) {
                            handleSoundEvent(event, result.inferenceTimeMs)
                        }
                    }
                    is InferenceResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            currentSound = "Error: ${result.message}"
                        )
                    }
                    is InferenceResult.Loading -> {}
                }
            }
        }
    }

    /**
     * Handle a detected sound event — update UI, trigger alerts.
     */
    private suspend fun handleSoundEvent(event: SoundEvent, inferenceTimeMs: Long) {
        val displayName = getDisplayName(event.soundClass)
        val emoji = getEmoji(event.soundClass)

        // Add to history
        val display = SoundEventDisplay(
            soundClass = event.soundClass,
            displayName = displayName,
            emoji = emoji,
            priority = event.priority,
            confidence = event.confidence,
            timeAgo = getTimeAgo(monitoringStartTime)
        )

        // Keep last 20 events, avoid consecutive duplicates
        if (eventHistory.lastOrNull()?.soundClass != event.soundClass) {
            eventHistory.add(0, display)
            if (eventHistory.size > 20) eventHistory.removeLast()
        }

        _uiState.value = _uiState.value.copy(
            currentSound = "$emoji $displayName",
            currentPriority = event.priority,
            confidence = event.confidence,
            recentEvents = eventHistory.toList(),
            inferenceTimeMs = inferenceTimeMs
        )

        // Trigger alerts for high-priority sounds
        when (event.priority) {
            SoundPriority.CRITICAL -> {
                hapticService.heavyImpact()
                ttsService.speak("Alert! $displayName detected!", queueMode = false)
            }
            SoundPriority.HIGH -> {
                hapticService.mediumImpact()
                ttsService.speak("$displayName detected", queueMode = true)
            }
            SoundPriority.NORMAL -> {
                hapticService.lightTap()
            }
            SoundPriority.LOW -> { /* No alert */ }
        }
    }

    /**
     * Stop listening.
     */
    fun stopListening() {
        viewModelScope.launch {
            audioCaptureService.stopCapture()
            _uiState.value = _uiState.value.copy(
                isListening = false,
                currentSound = "Stopped"
            )
        }
    }

    private fun getDisplayName(soundClass: String): String = when (soundClass) {
        "fire_alarm" -> "Fire Alarm"
        "siren" -> "Siren"
        "car_horn" -> "Car Horn"
        "glass_breaking" -> "Glass Breaking"
        "emergency" -> "Emergency Sound"
        "scream" -> "Screaming"
        "doorbell" -> "Doorbell"
        "door_knock" -> "Door Knock"
        "phone_ring" -> "Phone Ringing"
        "alarm" -> "Alarm"
        "baby_cry" -> "Baby Crying"
        "speech" -> "Speech"
        "music" -> "Music"
        "dog_bark" -> "Dog Barking"
        "cat_meow" -> "Cat Meowing"
        "vehicle" -> "Vehicle"
        "thunder" -> "Thunder"
        "laughter" -> "Laughter"
        "silence" -> "Silence"
        "background_noise" -> "Background Noise"
        else -> soundClass.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    private fun getEmoji(soundClass: String): String = when (soundClass) {
        "fire_alarm" -> "\uD83D\uDD25"  // 🔥
        "siren" -> "\uD83D\uDEA8"       // 🚨
        "car_horn" -> "\uD83D\uDE97"    // 🚗
        "glass_breaking" -> "\uD83D\uDCA5"
        "emergency" -> "\u26A0\uFE0F"   // ⚠️
        "scream" -> "\uD83D\uDE31"       // 😱
        "doorbell" -> "\uD83D\uDECE\uFE0F" // 🛎️
        "door_knock" -> "\uD83D\uDEAA" // 🚪
        "phone_ring" -> "\uD83D\uDCF1" // 📱
        "alarm" -> "\u23F0"             // ⏰
        "baby_cry" -> "\uD83D\uDC76"   // 👶
        "speech" -> "\uD83D\uDDE3\uFE0F" // 🗣️
        "music" -> "\uD83C\uDFB5"       // 🎵
        "dog_bark" -> "\uD83D\uDC36"    // 🐶
        "cat_meow" -> "\uD83D\uDC31"    // 🐱
        "vehicle" -> "\uD83D\uDE99"     // 🚙
        "thunder" -> "\u26A1"           // ⚡
        "laughter" -> "\uD83D\uDE02"    // 😂
        "silence" -> "\uD83E\uDD2B"     // 🤫
        else -> "\uD83D\uDD0A"          // 🔊
    }

    private fun getTimeAgo(startTime: Long): String {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        return when {
            elapsed < 5 -> "just now"
            elapsed < 60 -> "${elapsed}s ago"
            else -> "${elapsed / 60}m ago"
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { audioCaptureService.stopCapture() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScreen(
    onBack: () -> Unit,
    viewModel: AudioViewModel = org.koin.androidx.compose.koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(PermissionManager.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.startListening()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Monitor") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopListening()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Current sound display with pulse animation
            CurrentSoundCard(
                currentSound = uiState.currentSound,
                priority = uiState.currentPriority,
                confidence = uiState.confidence,
                isListening = uiState.isListening,
                inferenceTimeMs = uiState.inferenceTimeMs
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Start/Stop button
            FloatingActionButton(
                onClick = {
                    if (!hasAudioPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (uiState.isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                modifier = Modifier
                    .size(80.dp)
                    .semantics {
                        contentDescription = if (uiState.isListening)
                            "Stop listening. Tap to stop sound monitoring."
                        else "Start listening. Tap to begin sound monitoring."
                    },
                containerColor = if (uiState.isListening)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = if (uiState.isListening) "Tap to stop" else "Tap to listen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recent events history
            if (uiState.recentEvents.isNotEmpty()) {
                Text(
                    text = "Recent Sounds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() }
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.recentEvents) { event ->
                        SoundEventCard(event)
                    }
                }
            }
        }
    }
}

/**
 * Current sound display with animated background based on priority.
 */
@Composable
fun CurrentSoundCard(
    currentSound: String,
    priority: SoundPriority,
    confidence: Float,
    isListening: Boolean,
    inferenceTimeMs: Long
) {
    val bgColor by animateColorAsState(
        targetValue = when (priority) {
            SoundPriority.CRITICAL -> Color(0xFFFFCDD2) // red
            SoundPriority.HIGH -> Color(0xFFFFF9C4)     // yellow
            SoundPriority.NORMAL -> Color(0xFFE3F2FD)   // blue
            SoundPriority.LOW -> Color(0xFFF5F5F5)      // gray
        },
        label = "bgColor"
    )

    // Pulse animation when listening
    val pulseScale = if (isListening) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        scale
    } else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale)
            .semantics {
                liveRegion = LiveRegionMode.Assertive
                contentDescription = "Current sound: $currentSound. " +
                        if (confidence > 0) "Confidence: ${(confidence * 100).toInt()} percent." else ""
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentSound,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (confidence > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "Confidence: ${(confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${inferenceTimeMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }
        }
    }
}

/**
 * Card for a single sound event in the history list.
 */
@Composable
fun SoundEventCard(event: SoundEventDisplay) {
    val borderColor = when (event.priority) {
        SoundPriority.CRITICAL -> Color(0xFFE53935)
        SoundPriority.HIGH -> Color(0xFFFFA000)
        SoundPriority.NORMAL -> Color(0xFF1E88E5)
        SoundPriority.LOW -> Color(0xFFBDBDBD)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${event.displayName}, ${event.timeAgo}, " +
                        "confidence ${(event.confidence * 100).toInt()} percent"
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(borderColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Emoji
            Text(text = event.emoji, style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.width(12.dp))

            // Sound details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(event.confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575)
                )
            }

            // Time
            Text(
                text = event.timeAgo,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}
